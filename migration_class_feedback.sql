-- Creates the independent member feedback table for classes and coaches.
-- Apply manually to the target PostgreSQL database after reviewing the schema.

CREATE TABLE IF NOT EXISTS class_feedbacks (
    feedback_id   SERIAL PRIMARY KEY,
    class_id      INT NOT NULL,
    member_id     INT NOT NULL,
    coach_id      INT NOT NULL,
    class_rating  SMALLINT NOT NULL
        CHECK (class_rating BETWEEN 1 AND 5),
    coach_rating  SMALLINT NOT NULL
        CHECK (coach_rating BETWEEN 1 AND 5),
    comment       TEXT NULL,
    created_at    TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_class_feedback_class
        FOREIGN KEY (class_id) REFERENCES classes(class_id),
    CONSTRAINT fk_class_feedback_member
        FOREIGN KEY (member_id) REFERENCES members(user_id),
    CONSTRAINT fk_class_feedback_coach
        FOREIGN KEY (coach_id) REFERENCES coaches(user_id),
    CONSTRAINT uq_feedback_class_member UNIQUE (class_id, member_id)
);

CREATE INDEX IF NOT EXISTS idx_class_feedback_coach
    ON class_feedbacks(coach_id);
