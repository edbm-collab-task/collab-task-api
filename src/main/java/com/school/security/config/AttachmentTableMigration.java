package com.school.security.config;

import lombok.AllArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Fusionne les anciennes tables {@code task_attachments} et
 * {@code message_attachments} dans la table unique {@code attachments}.
 *
 * <p>La table {@code attachments} est créée par Hibernate à partir de l'entité
 * {@code Attachment}. Ce composant s'exécute après l'initialisation de la source
 * de données (donc après {@code data.sql}) et réalise, une seule fois :
 * <ol>
 *   <li>l'ajout de la contrainte {@code attachments_one_parent}, qui impose
 *       « exactement un parent » (tâche OU message). Hibernate ne l'ajoute pas
 *       sur une table existante ;</li>
 *   <li>la recopie des lignes des deux anciennes tables, si elles existent ;</li>
 *   <li>la suppression des deux anciennes tables (Hibernate ne supprime jamais
 *       une table dont l'entité a disparu).</li>
 * </ol>
 *
 * <p>La recopie est protégée par le marqueur {@code attachments-unified-v1} de
 * la table {@code app_migrations} : elle ne peut pas être rejouée. L'ensemble
 * est donc idempotent et convergent, quel que soit le nombre de démarrages.
 *
 * <p>Cette migration est écrite en Java (et non dans {@code data.sql}) parce
 * que les blocs conditionnels PostgreSQL {@code DO $$ ... $$} contiennent des
 * points-virgules que le séparateur de scripts de Spring coupe.
 */
@Component
@AllArgsConstructor
public class AttachmentTableMigration implements ApplicationRunner {

    private static final String MARKER = "attachments-unified-v1";

    private final JdbcTemplate jdbcTemplate;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        ensureOneParentConstraint();
        createIndexes();

        boolean legacyTask = tableExists("task_attachments");
        boolean legacyMessage = tableExists("message_attachments");

        /*
         * Le marqueur est posé même lorsqu'aucune table historique n'existe,
         * afin que la table app_migrations soit identique sur une base neuve et
         * sur une base migrée.
         */
        if (!markerApplied()) {
            if (legacyTask) {
                copyTaskAttachments();
            }
            if (legacyMessage) {
                copyMessageAttachments();
            }
            applyMarker();
        }

        if (!legacyTask && !legacyMessage) {
            return;
        }

        jdbcTemplate.execute("DROP TABLE IF EXISTS task_attachments");
        jdbcTemplate.execute("DROP TABLE IF EXISTS message_attachments");
    }

    private void ensureOneParentConstraint() {
        Boolean present = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM pg_constraint"
                        + " WHERE conname = 'attachments_one_parent')",
                Boolean.class);
        if (Boolean.TRUE.equals(present)) {
            return;
        }
        jdbcTemplate.execute("""
                ALTER TABLE attachments
                    ADD CONSTRAINT attachments_one_parent
                    CHECK ((task_id IS NOT NULL)::int + (message_id IS NOT NULL)::int = 1)
                """);
    }

    private void createIndexes() {
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_attachments_task"
                        + " ON attachments (task_id)");
        jdbcTemplate.execute(
                "CREATE INDEX IF NOT EXISTS idx_attachments_message"
                        + " ON attachments (message_id)");
    }

    private boolean tableExists(String table) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public." + table + "') IS NOT NULL",
                Boolean.class);
        return Boolean.TRUE.equals(exists);
    }

    private boolean markerApplied() {
        Boolean present = jdbcTemplate.queryForObject(
                "SELECT EXISTS (SELECT 1 FROM app_migrations WHERE key = ?)",
                Boolean.class,
                MARKER);
        return Boolean.TRUE.equals(present);
    }

    /**
     * Recopie les pièces jointes de tâche.
     *
     * <p>{@code task_attachments.name} contenait le chemin disque : le nom
     * stocké est réduit au dernier segment, comme pour les nouvelles lignes.
     */
    private void copyTaskAttachments() {
        jdbcTemplate.execute("""
                INSERT INTO attachments (task_id, message_id, original_name, stored_name,
                                         content_type, size, path, uploaded_at, uploaded_by)
                SELECT task_id, NULL, original_name, regexp_replace(name, '^.*/', ''),
                       content_type, size, path, uploaded_at, uploaded_by
                FROM task_attachments
                """);
    }

    /**
     * Recopie les pièces jointes de message.
     *
     * <p>{@code message_attachments.name} contenait le nom d'origine et
     * {@code url} le chemin disque : le nom stocké est extrait de l'URL. Ces
     * lignes n'avaient ni date ni auteur propres : on reprend
     * {@code created_at} et {@code sender_id} du message parent.
     */
    private void copyMessageAttachments() {
        jdbcTemplate.execute("""
                INSERT INTO attachments (task_id, message_id, original_name, stored_name,
                                         content_type, size, path, uploaded_at, uploaded_by)
                SELECT NULL, ma.message_id, ma.name, regexp_replace(ma.url, '^.*/', ''),
                       ma.type, ma.size, ma.url,
                       COALESCE(m.created_at, now()), m.sender_id
                FROM message_attachments ma
                LEFT JOIN messages m ON m.message_id = ma.message_id
                """);
    }

    private void applyMarker() {
        jdbcTemplate.update(
                "INSERT INTO app_migrations (key) VALUES (?) ON CONFLICT DO NOTHING",
                MARKER);
    }
}
