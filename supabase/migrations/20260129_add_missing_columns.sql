-- Add missing columns for F-Droid sync
ALTER TABLE solutions ADD COLUMN IF NOT EXISTS category text;
ALTER TABLE solutions ADD COLUMN IF NOT EXISTS fdroid_synced boolean DEFAULT false;

-- Notify PostgREST to reload the schema cache
NOTIFY pgrst, 'reload config';
