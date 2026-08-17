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

INSERT INTO priorities (name)
SELECT priority_name
FROM (
    VALUES
        ('Basse'),
        ('Moyenne'),
        ('Haute'),
        ('Urgente')
) AS priorities_to_insert(priority_name)
WHERE NOT EXISTS (
    SELECT 1
    FROM priorities
    WHERE priorities.name = priorities_to_insert.priority_name
);

INSERT INTO statuses (name, sort_order)
SELECT status_name, status_order
FROM (
    VALUES
        ('A faire', 1),
        ('En cours', 2),
        ('Termine', 3)
) AS statuses_to_insert(status_name, status_order)
WHERE NOT EXISTS (
    SELECT 1
    FROM statuses
    WHERE statuses.name = statuses_to_insert.status_name
);
