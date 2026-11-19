#!/usr/bin/env node
// @ts-check
/**
 * Export the public LibreFind catalog for CDN publication.
 *
 * Outputs (under <repo>/dist):
 *   catalog-<sha256>.json.gz  Immutable, gzipped catalog bundle.
 *   catalog-index.json        Atomic pointer consumed by the Android client.
 *   manifest.json             Counts and metadata used by the workflow.
 *
 * The bundle schema mirrors the Android DTOs in
 * app/src/main/java/com/jksalcedo/librefind/data/remote/model/CatalogDto.kt:
 *
 *   {
 *     "version": 1,
 *     "targets":   [{ "id", "packageName", "name", "categories" }],
 *     "solutions": [{ "id", "packageName", "name", "description", "license",
 *                     "iconUrl", "websiteUrl", "repositoryUrl", "fdroidUrl",
 *                     "upvotes", "downvotes" }],
 *     "mappings":  [{ "targetId", "solutionId" }]
 *   }
 */

import { createHash } from 'node:crypto'
import { mkdirSync, writeFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'
import { gzipSync } from 'node:zlib'
import { createClient } from '@supabase/supabase-js'

// Load a local .env when present (for example scripts/.env when running via
// `npm run`). CI provides the variables directly, so a missing file is fine.
try {
  process.loadEnvFile()
} catch {
  // No .env file; fall back to the process environment.
}

const BUNDLE_VERSION = 1
const PAGE_SIZE = 1000
const DNS_NAMESPACE = '6ba7b810-9dad-11d1-80b4-00c04fd430c8'

const TARGET_COLUMNS = 'package_name,name,category,alternatives'
const SOLUTION_COLUMNS = [
  'package_name',
  'name',
  'description',
  'license',
  'icon_url',
  'website',
  'repo_url',
  'fdroid_id',
  'source_repo',
].join(',')

const SUPABASE_URL = process.env.SUPABASE_URL
const SUPABASE_KEY = process.env.SUPABASE_KEY ?? process.env.SUPABASE_SERVICE_ROLE_KEY

const SCRIPT_DIR = dirname(fileURLToPath(import.meta.url))
const DIST_DIR = resolve(SCRIPT_DIR, '..', 'dist')

if (!SUPABASE_URL || !SUPABASE_KEY) {
  console.error('Missing SUPABASE_URL or SUPABASE_KEY (or SUPABASE_SERVICE_ROLE_KEY)')
  process.exit(1)
}

const supabase = createClient(SUPABASE_URL, SUPABASE_KEY, {
  auth: { persistSession: false, autoRefreshToken: false },
})

/**
 * RFC 4122 UUIDv5 (SHA-1 name-based UUID).
 * @param {string} name
 * @param {string} [namespace]
 * @returns {string}
 */
function uuidv5(name, namespace = DNS_NAMESPACE) {
  const namespaceBytes = Buffer.from(namespace.replaceAll('-', ''), 'hex')
  const hash = createHash('sha1')
    .update(namespaceBytes)
    .update(Buffer.from(name, 'utf8'))
    .digest()

  hash[6] = (hash[6] & 0x0f) | 0x50 // version 5
  hash[8] = (hash[8] & 0x3f) | 0x80 // RFC 4122 variant

  const hex = hash.subarray(0, 16).toString('hex')
  return [
    hex.slice(0, 8),
    hex.slice(8, 12),
    hex.slice(12, 16),
    hex.slice(16, 20),
    hex.slice(20),
  ].join('-')
}

/**
 * Fetch every row from a table. PostgREST caps a response at the project's
 * max-rows setting, so walk the table with Range requests.
 *
 * @param {string} table
 * @param {string} columns
 * @returns {Promise<Record<string, any>[]>}
 */
async function fetchAll(table, columns) {
  /** @type {Record<string, any>[]} */
  const rows = []
  let from = 0

  while (true) {
    const { data, error } = await supabase
      .from(table)
      .select(columns)
      .order('package_name', { ascending: true })
      .range(from, from + PAGE_SIZE - 1)

    if (error) {
      throw new Error(`Failed to fetch ${table}: ${error.message}`)
    }
    if (!data || data.length === 0) break

    rows.push(...data)
    // Advance by the number of rows actually returned. Some projects cap
    // responses below PAGE_SIZE, so a short page is not necessarily the end.
    from += data.length

    if (rows.length % PAGE_SIZE === 0) {
      console.log(`  ${table}: fetched ${rows.length} rows...`)
    }
  }

  console.log(`  ${table}: ${rows.length} rows`)
  return rows
}

/** @param {unknown} value */
function emptyToNull(value) {
  return typeof value === 'string' && value.length > 0 ? value : null
}

/**
 * @param {Record<string, any>[]} targetRows
 * @param {Record<string, any>[]} solutionRows
 */
function buildBundle(targetRows, solutionRows) {
  const targets = targetRows.map((row) => ({
    id: uuidv5(`target:${row.package_name}`),
    packageName: row.package_name,
    name: row.name,
    categories: row.category ? [row.category] : [],
  }))

  const solutions = solutionRows.map((row) => ({
    id: uuidv5(`solution:${row.package_name}`),
    packageName: row.package_name,
    name: row.name,
    // Descriptions intentionally preserve an empty string, matching the
    // existing bundled catalog.
    description: row.description ?? null,
    license: emptyToNull(row.license),
    iconUrl: emptyToNull(row.icon_url),
    websiteUrl: emptyToNull(row.website),
    // Some older community/F-Droid rows have no repo URL but do have a
    // source marker, so fall back to it the way the original exporter did.
    repositoryUrl: emptyToNull(row.repo_url) ?? emptyToNull(row.source_repo),
    fdroidUrl: row.fdroid_id
      ? `https://f-droid.org/packages/${row.fdroid_id}`
      : null,
    // Match votes are target-specific and are fetched live by the app; the
    // catalog itself only needs the solution rows.
    upvotes: 0,
    downvotes: 0,
  }))

  const solutionIdByPackage = new Map(
    solutions.map((solution) => [solution.packageName, solution.id]),
  )

  /** @type {{ targetId: string, solutionId: string }[]} */
  const mappings = []
  const seenMappings = new Set()
  let unresolvedAlternatives = 0

  for (const row of targetRows) {
    const targetId = uuidv5(`target:${row.package_name}`)
    const alternatives = Array.isArray(row.alternatives) ? row.alternatives : []

    for (const alternative of alternatives) {
      if (typeof alternative !== 'string' || alternative.length === 0) continue

      const solutionId = solutionIdByPackage.get(alternative)
      if (!solutionId) {
        unresolvedAlternatives++
        continue
      }

      const key = `${targetId}\u0000${solutionId}`
      if (seenMappings.has(key)) continue
      seenMappings.add(key)

      mappings.push({ targetId, solutionId })
    }
  }

  targets.sort((a, b) => a.packageName.localeCompare(b.packageName))
  solutions.sort((a, b) => a.packageName.localeCompare(b.packageName))
  mappings.sort(
    (a, b) =>
      a.targetId.localeCompare(b.targetId) ||
      a.solutionId.localeCompare(b.solutionId),
  )

  return {
    bundle: {
      version: BUNDLE_VERSION,
      targets,
      solutions,
      mappings,
    },
    unresolvedAlternatives,
  }
}

/**
 * @param {{ id: string, packageName: string }[]} rows
 * @param {string} label
 */
function assertUnique(rows, label) {
  const ids = new Set()
  const packages = new Set()

  for (const row of rows) {
    if (ids.has(row.id)) throw new Error(`Duplicate ${label} id: ${row.id}`)
    if (packages.has(row.packageName)) {
      throw new Error(`Duplicate ${label} package name: ${row.packageName}`)
    }
    ids.add(row.id)
    packages.add(row.packageName)
  }
}

const started = Date.now()

try {
  console.log('Fetching catalog rows from Supabase...')
  const [targetRows, solutionRows] = await Promise.all([
    fetchAll('targets', TARGET_COLUMNS),
    fetchAll('solutions', SOLUTION_COLUMNS),
  ])

  const { bundle, unresolvedAlternatives } = buildBundle(targetRows, solutionRows)

  if (bundle.targets.length === 0 || bundle.solutions.length === 0) {
    throw new Error('Refusing to export an empty catalog')
  }

  assertUnique(bundle.targets, 'target')
  assertUnique(bundle.solutions, 'solution')

  const json = JSON.stringify(bundle)
  const jsonBuffer = Buffer.from(json, 'utf8')
  const hash = createHash('sha256').update(jsonBuffer).digest('hex')
  const compressed = gzipSync(jsonBuffer)

  mkdirSync(DIST_DIR, { recursive: true })

  const filename = `catalog-${hash}.json.gz`
  const timestamp = Math.floor(Date.now() / 1000)
  const counts = {
    target_count: bundle.targets.length,
    solution_count: bundle.solutions.length,
    mapping_count: bundle.mappings.length,
  }

  const index = {
    version: BUNDLE_VERSION,
    filename,
    hash,
    timestamp,
    ...counts,
  }

  const manifest = {
    version: BUNDLE_VERSION,
    generated_at: new Date(timestamp * 1000).toISOString(),
    filename,
    hash,
    size_bytes: compressed.length,
    ...counts,
  }

  writeFileSync(resolve(DIST_DIR, filename), compressed)
  writeFileSync(
    resolve(DIST_DIR, 'catalog-index.json'),
    `${JSON.stringify(index, null, 2)}\n`,
  )
  writeFileSync(
    resolve(DIST_DIR, 'manifest.json'),
    `${JSON.stringify(manifest, null, 2)}\n`,
  )

  console.log('')
  console.log(`✅ Wrote dist/${filename}`)
  console.log(`   hash:      ${hash}`)
  console.log(`   targets:   ${counts.target_count}`)
  console.log(`   solutions: ${counts.solution_count}`)
  console.log(`   mappings:  ${counts.mapping_count}`)
  console.log(`   size:      ${compressed.length} bytes`)
  if (unresolvedAlternatives > 0) {
    console.log(
      `   skipped ${unresolvedAlternatives} alternative(s) with no matching solution`,
    )
  }
  console.log(`   elapsed:   ${((Date.now() - started) / 1000).toFixed(2)}s`)
} catch (err) {
  console.error('❌ Catalog export failed:', err)
  process.exit(1)
}
