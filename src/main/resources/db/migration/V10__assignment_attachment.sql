CREATE TABLE assignment_attachment (
    attachment_id  VARCHAR(30)   PRIMARY KEY
                   CHECK (left(attachment_id, 4) = 'att_'),
    assignment_id  VARCHAR(30)   NOT NULL REFERENCES assignment(assignment_id) ON DELETE CASCADE,
    kind           VARCHAR(10)   NOT NULL
                   CHECK (kind IN ('file', 'image', 'link')),
    name           VARCHAR(255),
    content_type   VARCHAR(100),
    size_bytes     BIGINT        CHECK (size_bytes BETWEEN 1 AND 15728640),
    s3_key         VARCHAR(1024),
    url            VARCHAR(2048),
    sort_order     INTEGER       NOT NULL DEFAULT 0,
    CHECK (
        (kind IN ('file', 'image') AND name IS NOT NULL AND content_type IS NOT NULL
                                   AND size_bytes IS NOT NULL AND s3_key IS NOT NULL
                                   AND url IS NULL)
     OR (kind = 'link' AND url IS NOT NULL
                       AND content_type IS NULL AND size_bytes IS NULL AND s3_key IS NULL)
    )
);

CREATE INDEX idx_attachment_assignment ON assignment_attachment (assignment_id, sort_order);
