
CREATE TABLE idempotency_key (
                                 idem_key       VARCHAR(100) PRIMARY KEY,
                                 user_id        VARCHAR(30)  NOT NULL,
                                 submission_id  VARCHAR(30)  NOT NULL REFERENCES submission(submission_id) ON DELETE CASCADE,
                                 created_at     TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Index foreign key and user lookups for performance
CREATE INDEX idx_idempotency_submission ON idempotency_key (submission_id);
CREATE INDEX idx_idempotency_user       ON idempotency_key (user_id);


ALTER TABLE submission_artifact DROP CONSTRAINT IF EXISTS submission_artifact_kind_check;
ALTER TABLE submission_artifact DROP CONSTRAINT IF EXISTS submission_artifact_content_check;

-- Expand allowed kinds to include 'link'
ALTER TABLE submission_artifact
    ADD CONSTRAINT submission_artifact_kind_check
        CHECK (kind IN ('file', 'text', 'link'));

-- Enforce valid column combinations for file, text, and link
ALTER TABLE submission_artifact
    ADD CONSTRAINT submission_artifact_content_check
        CHECK (
            -- File rule
            (kind = 'file' AND name IS NOT NULL AND content_type IS NOT NULL
                AND size_bytes IS NOT NULL AND s3_key IS NOT NULL
                AND body IS NULL AND url IS NULL)

                -- Text rule
                OR (kind = 'text' AND body IS NOT NULL
                AND name IS NULL AND content_type IS NULL AND size_bytes IS NULL
                AND s3_key IS NULL AND url IS NULL)

                -- Link rule (NEW)
                OR (kind = 'link' AND url IS NOT NULL
                AND name IS NOT NULL AND content_type IS NOT NULL AND size_bytes IS NOT NULL
                AND s3_key IS NOT NULL AND body IS NULL)
            );