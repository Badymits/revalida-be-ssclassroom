
INSERT INTO cohort (cohort_id, name) VALUES
    ('crs_CYBER_CLOUD_SECURITY', 'Cyber Security Cloud Security'),
    ('crs_PHP_FUNDAMENTALS',     'PHP Fundamentals'),
    ('crs_CPP_PROGRAMMING',      'C++ Programming')
ON CONFLICT (cohort_id) DO NOTHING;

ALTER TABLE assignment ADD COLUMN IF NOT EXISTS created_at   TIMESTAMPTZ NOT NULL DEFAULT now();
ALTER TABLE assignment ADD COLUMN IF NOT EXISTS published_at TIMESTAMPTZ;

UPDATE assignment SET published_at = created_at WHERE status = 'published' AND published_at IS NULL;