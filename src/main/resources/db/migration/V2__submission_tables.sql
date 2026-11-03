CREATE TABLE submission (
    submission_id  VARCHAR(30)  PRIMARY KEY
                   CHECK (left(submission_id, 4) = 'sub_'),
    assignment_id  VARCHAR(30)  NOT NULL REFERENCES assignment(assignment_id) ON DELETE CASCADE,
    learner_id     VARCHAR(30)  NOT NULL,                       
    status         VARCHAR(10)  NOT NULL DEFAULT 'assigned'
                   CHECK (status IN ('assigned', 'submitted', 'returned')),
    submitted_at   TIMESTAMPTZ,                                 
    UNIQUE (assignment_id, learner_id),                         
    CHECK ( (status = 'assigned'  AND submitted_at IS NULL)
         OR (status <> 'assigned' AND submitted_at IS NOT NULL) )
);

CREATE INDEX idx_submission_roster  ON submission (assignment_id, status, submission_id);  
CREATE INDEX idx_submission_learner ON submission (learner_id);                            

CREATE TABLE submission_artifact (
    artifact_id    VARCHAR(30)    PRIMARY KEY                   
                   CHECK (left(artifact_id, 4) = 'art_'),
    submission_id  VARCHAR(30)    NOT NULL REFERENCES submission(submission_id) ON DELETE CASCADE,
    kind           VARCHAR(10)    NOT NULL
                   CHECK (kind IN ('file', 'text', 'link')),
    name           VARCHAR(255),
    content_type   VARCHAR(100),
    size_bytes     BIGINT         CHECK (size_bytes BETWEEN 1 AND 52428800),   
    s3_key         VARCHAR(1024),
    body           TEXT           CHECK (char_length(body) <= 100000),
    url            VARCHAR(2048),
    CHECK (
        (kind = 'file' AND name IS NOT NULL AND content_type IS NOT NULL
                       AND size_bytes IS NOT NULL AND s3_key IS NOT NULL
                       AND body IS NULL AND url IS NULL)
     OR (kind = 'text' AND body IS NOT NULL
                       AND name IS NULL AND content_type IS NULL AND size_bytes IS NULL
                       AND s3_key IS NULL AND url IS NULL)
     OR (kind = 'link' AND url IS NOT NULL
                       AND name IS NULL AND content_type IS NULL AND size_bytes IS NULL
                       AND s3_key IS NULL AND body IS NULL)
    )
);
CREATE INDEX idx_artifact_submission ON submission_artifact (submission_id);