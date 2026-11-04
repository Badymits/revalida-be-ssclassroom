-- =====================================================================
-- V3 - Dev 3 - Review & Grading: grade
-- A grade row is written only on Return; returned work is final.
-- Any user with the trainer role may return work (no per-cohort check).
-- =====================================================================

CREATE TABLE grade (
    submission_id    VARCHAR(30)    PRIMARY KEY REFERENCES submission(submission_id) ON DELETE CASCADE,
    points           INTEGER        NOT NULL CHECK (points >= 0), 
    private_comment  VARCHAR(5000)  NOT NULL DEFAULT '',
    returned_at      TIMESTAMPTZ    NOT NULL DEFAULT now(),
    returned_by      VARCHAR(30)    NOT NULL
    CHECK (left(submission_id, 4) = 'usr_')                        
);