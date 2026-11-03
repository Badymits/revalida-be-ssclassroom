
CREATE TABLE cohort (
    cohort_id  VARCHAR(30)   PRIMARY KEY
               CHECK (left(cohort_id, 4) = 'crs_'),
    name       VARCHAR(200)  NOT NULL
               CHECK (char_length(name) >= 1)
);

CREATE TABLE cohort_member (
    cohort_id     VARCHAR(30)   NOT NULL REFERENCES cohort(cohort_id) ON DELETE CASCADE,
    user_id       VARCHAR(30)   NOT NULL
     			  CHECK (left(user_id, 4) = 'usr_'),                      
    role          VARCHAR(10)   NOT NULL
                  CHECK (role IN ('trainer', 'learner')),
    display_name  VARCHAR(120)  NOT NULL
                  CHECK (char_length(display_name) >= 1),
    PRIMARY KEY (cohort_id, user_id)
);

CREATE INDEX idx_cohort_member_user ON cohort_member (user_id);   

CREATE TABLE assignment (
    assignment_id    VARCHAR(30)   PRIMARY KEY
                     CHECK (left(assignment_id, 4) = 'asg_'),
    cohort_id        VARCHAR(30)   NOT NULL REFERENCES cohort(cohort_id),
    title            VARCHAR(200)  NOT NULL
                     CHECK (char_length(title) >= 1),
    instructions     TEXT          NOT NULL DEFAULT ''
                     CHECK (char_length(instructions) <= 50000),
    submission_type  VARCHAR(10)   NOT NULL
                     CHECK (submission_type IN ('file', 'text', 'link')),
    points           INTEGER       NOT NULL DEFAULT 100         
                     CHECK (points BETWEEN 0 AND 1000),
    status           VARCHAR(10)   NOT NULL DEFAULT 'draft'
                     CHECK (status IN ('draft', 'published'))
);

CREATE INDEX idx_assignment_cohort ON assignment (cohort_id);