CREATE TABLE app_user (
    user_id        VARCHAR(30)   PRIMARY KEY
                   CHECK (left(user_id, 4) = 'usr_'),
    first_name     VARCHAR(600)  NOT NULL,
    last_name      VARCHAR(600)  NOT NULL,
    username       VARCHAR(50)   NOT NULL UNIQUE
                   CHECK (char_length(username) >= 1),
    password_hash  VARCHAR(255)  NOT NULL,
    role           VARCHAR(10)   NOT NULL
                   CHECK (role IN ('trainer', 'learner'))
);

ALTER TABLE cohort_member
    DROP COLUMN role,
    DROP COLUMN display_name,
    ADD CONSTRAINT cohort_member_user_fk
        FOREIGN KEY (user_id) REFERENCES app_user(user_id) ON DELETE CASCADE;

ALTER TABLE submission
    ADD CONSTRAINT submission_learner_fk
        FOREIGN KEY (learner_id) REFERENCES app_user(user_id);

ALTER TABLE grade
    ADD CONSTRAINT grade_returned_by_fk
        FOREIGN KEY (returned_by) REFERENCES app_user(user_id);