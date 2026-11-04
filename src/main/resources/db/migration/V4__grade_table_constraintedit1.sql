ALTER TABLE grade DROP CONSTRAINT grade_submission_id_check;

ALTER TABLE grade
    ADD CONSTRAINT grade_returned_by_check
    CHECK (left(returned_by, 4) = 'usr_');