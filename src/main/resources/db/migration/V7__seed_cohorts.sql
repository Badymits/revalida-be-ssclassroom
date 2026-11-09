-- V7 - Dev 4 - Reference data: starter cohorts so learners can register.
INSERT INTO cohort (cohort_id, name) VALUES
    ('crs_JAVA_FULL_STACK', 'Java Full Stack'),
    ('crs_PYTHON_MACHINE_LEARNING', 'Python Machine Learning')
ON CONFLICT (cohort_id) DO NOTHING;
