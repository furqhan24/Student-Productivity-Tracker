-- Run this once after normalize_activity_category.sql.
-- It preserves ACTIVITY and adds Activity -> SubActivity tracking.

CREATE TABLE sub_activity (
    subactivity_id NUMBER PRIMARY KEY,
    subactivity_name VARCHAR2(100) NOT NULL,
    activity_id NUMBER NOT NULL,
    CONSTRAINT fk_sub_activity_activity
        FOREIGN KEY (activity_id) REFERENCES activity(activity_id),
    CONSTRAINT uq_sub_activity_name
        UNIQUE (activity_id, subactivity_name)
);

-- Keep old rows compatible by giving each existing activity a General subactivity.
INSERT INTO sub_activity (subactivity_id, subactivity_name, activity_id)
SELECT ROW_NUMBER() OVER (ORDER BY activity_id), 'General', activity_id
FROM activity;

ALTER TABLE schedule ADD subactivity_id NUMBER;
ALTER TABLE daily_log ADD subactivity_id NUMBER;

UPDATE schedule s
SET subactivity_id = (
    SELECT sa.subactivity_id
    FROM sub_activity sa
    WHERE sa.activity_id = s.activity_id
      AND sa.subactivity_name = 'General'
)
WHERE subactivity_id IS NULL;

UPDATE daily_log dl
SET subactivity_id = (
    SELECT sa.subactivity_id
    FROM sub_activity sa
    WHERE sa.activity_id = dl.activity_id
      AND sa.subactivity_name = 'General'
)
WHERE subactivity_id IS NULL;

ALTER TABLE schedule ADD CONSTRAINT fk_schedule_sub_activity
    FOREIGN KEY (subactivity_id) REFERENCES sub_activity(subactivity_id);

ALTER TABLE daily_log ADD CONSTRAINT fk_daily_log_sub_activity
    FOREIGN KEY (subactivity_id) REFERENCES sub_activity(subactivity_id);

-- Optional starter subactivities. Users can add more from the app without code changes.
INSERT INTO sub_activity (subactivity_id, subactivity_name, activity_id)
SELECT (SELECT NVL(MAX(subactivity_id), 0) FROM sub_activity) + ROW_NUMBER() OVER (ORDER BY a.activity_name, seed.subactivity_name),
       seed.subactivity_name,
       a.activity_id
FROM activity a
JOIN (
    SELECT 'Study' AS activity_name, 'DBMS' AS subactivity_name FROM dual UNION ALL
    SELECT 'Study', 'Operating Systems' FROM dual UNION ALL
    SELECT 'Study', 'Java' FROM dual UNION ALL
    SELECT 'Study', 'Mathematics' FROM dual UNION ALL
    SELECT 'Exercise', 'Cardio' FROM dual UNION ALL
    SELECT 'Exercise', 'Weight Training' FROM dual UNION ALL
    SELECT 'Exercise', 'Running' FROM dual UNION ALL
    SELECT 'Entertainment', 'Gaming' FROM dual UNION ALL
    SELECT 'Entertainment', 'YouTube' FROM dual UNION ALL
    SELECT 'Entertainment', 'Instagram' FROM dual UNION ALL
    SELECT 'Gaming', 'Gaming' FROM dual UNION ALL
    SELECT 'Social Media', 'YouTube' FROM dual UNION ALL
    SELECT 'Social Media', 'Instagram' FROM dual
) seed ON LOWER(seed.activity_name) = LOWER(a.activity_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM sub_activity existing
    WHERE existing.activity_id = a.activity_id
      AND LOWER(existing.subactivity_name) = LOWER(seed.subactivity_name)
);
