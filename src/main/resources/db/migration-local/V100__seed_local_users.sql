-- V100 - Dev 4 - LOCAL PROFILE ONLY seed data (loaded via spring.flyway.locations in application-local.yml).
-- Never ships to other environments.
--
-- All seeded users share the password: test77
-- (BCrypt hash generated for 'test77' at cost 11.)
--
--   usr_T1   role trainer   (no cohort_member row - trainers are exempt)
--   usr_L1   role learner   cohort "JAVA FULL STACK"
--   usr_L2   role learner   cohort "JAVA FULL STACK"
--   usr_L3   role learner   cohort "PYTHON MACHINE LEARNING"

INSERT INTO cohort (cohort_id, name) VALUES
    ('crs_JAVA_FULL_STACK',         'JAVA FULL STACK'),
    ('crs_PYTHON_MACHINE_LEARNING', 'PYTHON MACHINE LEARNING')
ON CONFLICT (cohort_id) DO NOTHING;

INSERT INTO app_user (user_id, first_name, last_name, username, password_hash, role) VALUES
    ('usr_T1', 'Test', 'T1', 'trainer01', '$2a$11$...your_bcrypt_hash_here...', 'trainer'),
    ('usr_L1', 'Test', 'L1', 'learner01', '$2a$11$...your_bcrypt_hash_here...', 'learner'),
    ('usr_L2', 'Test', 'L2', 'learner02', '$2a$11$...your_bcrypt_hash_here...', 'learner'),
    ('usr_L3', 'Test', 'L3', 'learner03', '$2a$11$...your_bcrypt_hash_here...', 'learner')
ON CONFLICT (user_id) DO NOTHING;

-- Learners only.
INSERT INTO cohort_member (cohort_id, user_id) VALUES
    ('crs_JAVA_FULL_STACK',         'usr_L1'),
    ('crs_JAVA_FULL_STACK',         'usr_L2'),
    ('crs_PYTHON_MACHINE_LEARNING', 'usr_L3')
ON CONFLICT DO NOTHING;