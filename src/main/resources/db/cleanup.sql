-- Delete duplicate emails keeping only the most recent entry
DELETE FROM users a USING (
    SELECT MAX(id) as max_id, email
    FROM users
    GROUP BY email
    HAVING COUNT(*) > 1
) b
WHERE a.email = b.email 
AND a.id != b.max_id;

-- Add unique constraint
ALTER TABLE users ADD CONSTRAINT uk_users_email UNIQUE (email); 