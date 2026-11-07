-- V6 - Dev 4 - Identity: a learner belongs to exactly one cohort.
CREATE UNIQUE INDEX uq_cohort_member_user ON cohort_member (user_id);