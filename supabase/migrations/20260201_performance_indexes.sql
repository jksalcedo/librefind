-- Performance Optimization: Add indexes for frequently queried columns
-- Migration created: 2026-02-01
-- Purpose: Reduce query times from 300-900ms to <50ms

-- Index on targets.package_name for proprietary app lookups
CREATE INDEX IF NOT EXISTS idx_targets_package_name 
ON targets(package_name);

-- Index on solutions.package_name for alternative app lookups
CREATE INDEX IF NOT EXISTS idx_solutions_package_name 
ON solutions(package_name);

-- Composite index on user_votes for vote lookups and aggregations
-- Covers: (user_id, package_name, vote_type) and (user_id, package_name)
CREATE INDEX IF NOT EXISTS idx_user_votes_composite 
ON user_votes(user_id, package_name, vote_type);

-- Index on user_submissions.submitter_id for submission history queries
CREATE INDEX IF NOT EXISTS idx_user_submissions_submitter 
ON user_submissions(submitter_id);

-- Additional index for vote aggregations by package
CREATE INDEX IF NOT EXISTS idx_user_votes_package_type 
ON user_votes(package_name, vote_type);

-- Notify PostgREST to reload the schema cache
NOTIFY pgrst, 'reload config';
