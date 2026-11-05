-- =========================================================
-- CLEANUP : vidange des donnees applicatives
-- Conserve : direction DSI, roles, permissions, statuts par defaut, priorites,
--           admin@edbm.com et superadmin@edbm.com
-- =========================================================
SET session_replication_role = 'replica';
DELETE FROM task_attachments;
DELETE FROM task_comments;
DELETE FROM comment_reactions;
DELETE FROM task_assignees;
DELETE FROM tasks;
DELETE FROM project_contributors;
DELETE FROM projects;
DELETE FROM user_project_permissions;
DELETE FROM notifications;
DELETE FROM message_attachments;
DELETE FROM messages;
DELETE FROM conversation_members;
DELETE FROM conversations;
DELETE FROM activities;
DELETE FROM users_roles WHERE users_users_id NOT IN (SELECT users_id FROM users WHERE email IN ('admin@edbm.com','superadmin@edbm.com'));
DELETE FROM users WHERE email NOT IN ('admin@edbm.com','superadmin@edbm.com');
DELETE FROM statuses WHERE name NOT IN ('A faire','En cours','Termine');
SET session_replication_role = 'origin';
