INSERT INTO roles (name)
SELECT role_name
FROM (
    VALUES
        ('USER'),
        ('ADMIN'),
        ('SUPER_ADMIN')
) AS roles_to_insert(role_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM roles
    WHERE roles.name = roles_to_insert.role_name
);
