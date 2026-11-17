-- For Grade Draft (T4)

CREATE TABLE grade_draft (
    submission_id    VARCHAR(30)    PRIMARY KEY REFERENCES submission(submission_id) ON DELETE CASCADE,
    points           INTEGER        CHECK (points >= 0),
    private_comment  VARCHAR(5000)  NOT NULL DEFAULT '',
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT now(),
    updated_by       VARCHAR(30)    NOT NULL CHECK (left(updated_by, 4) = 'usr_')
);

-- for App_User (not encrypted anymore)

ALTER TABLE app_user
    ALTER COLUMN first_name TYPE VARCHAR(50),
    ALTER COLUMN last_name  TYPE VARCHAR(50);
    
    