-- V8 - Dev 3 - Link Submission Version 2: a linked file is copied to S3 and stored as kind 'file', so only 'file' and 'text' remain.
-- A file row may keep the learner's original link in url; text rows never have one.
ALTER TABLE submission_artifact DROP CONSTRAINT submission_artifact_kind_check;
ALTER TABLE submission_artifact DROP CONSTRAINT submission_artifact_check;

ALTER TABLE submission_artifact
    ADD CONSTRAINT submission_artifact_kind_check
    CHECK (kind IN ('file', 'text'));

ALTER TABLE submission_artifact
    ADD CONSTRAINT submission_artifact_content_check
    CHECK (
        (kind = 'file' AND name IS NOT NULL AND content_type IS NOT NULL
                       AND size_bytes IS NOT NULL AND s3_key IS NOT NULL
                       AND body IS NULL)
     OR (kind = 'text' AND body IS NOT NULL
                       AND name IS NULL AND content_type IS NULL AND size_bytes IS NULL
                       AND s3_key IS NULL AND url IS NULL)
    );
