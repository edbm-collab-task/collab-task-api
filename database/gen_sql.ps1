$ErrorActionPreference = "Stop"

$csvPath = "C:\Users\Administrateur\Desktop\Projet Stage\inventaire_taches.csv"
$outClean = "C:\Users\Administrateur\Desktop\Projet Stage\cleanup.sql"
$outSeed = "C:\Users\Administrateur\Desktop\Projet Stage\seed_test_data.sql"

$BCRYPT = '$2a$10$RRKXYSbGEEAKbipHpQvFAO9DLUQnVZVLUt1fD9yQcqO8JwKRN//2.'  # bcrypt de "557619994006"

function Remove-Diacritics([string]$s) {
    if ([string]::IsNullOrEmpty($s)) { return $s }
    $formD = $s.Normalize([System.Text.NormalizationForm]::FormD)
    $sb = New-Object System.Text.StringBuilder
    foreach ($ch in $formD.ToCharArray()) {
        if ([System.Globalization.CharUnicodeInfo]::GetUnicodeCategory($ch) -ne [System.Globalization.UnicodeCategory]::NonSpacingMark) {
            [void]$sb.Append($ch)
        }
    }
    return $sb.ToString()
}

# Accents construits par codes (le .ps1 est lu en ANSI par PowerShell 5.1)
$ACC_e   = [char]0x00E9   # e aigu
$ACC_egr = [char]0x00E8   # e grave
$ACC_cir = [char]0x00EA   # e circonflexe
$ACC_a   = [char]0x00E0   # a grave
$ACC_o   = [char]0x00F4   # o circonflexe
$C_Amelioration = "Am" + $ACC_e + "lioration"
$C_Amele        = "Am" + $ACC_e + "lioration"
$C_Nouvelle     = "Nouvelle fonctionnalit" + $ACC_e
$C_NouvelleUX   = "Nouvelle fonctionnalit" + $ACC_e + " (UX)"
$C_Interop      = "Interop" + $ACC_e + "rabilit" + $ACC_e
$C_Iteration4   = "Orinasa It" + $ACC_e + "ration 4"

# ---------------------------------------------------------------
# Normalisation des categories (prefixe de titre entre crochets)
# ---------------------------------------------------------------
function Normalize-Cat([string]$c) {
    if ([string]::IsNullOrWhiteSpace($c)) { return $null }
    $m = @{
        "Bug"                                        = "Bug"
        "Bug / Amelioration"                         = "Bug / $C_Amelioration"
        "Amelioration"                               = $C_Amelioration
        "Amelioration (formulaire)"                  = $C_Amelioration
        "Amelioration (UX)"                          = "$C_Amelioration (UX)"
        "Amelioration / acces"                       = $C_Amelioration
        "Amelioration / bug potentiel"               = "Bug / $C_Amelioration"
        "Amelioration / Etude de faisabilite"        = $C_Amelioration
        "Amelioration /bug (document)"               = "Bug / $C_Amelioration"
        "Amelioration reglementaire"                 = $C_Amelioration
        "Nouvelle fonctionnalite"                    = $C_Nouvelle
        "NOuvelle fonctionnalite (UX)"               = $C_NouvelleUX
        "Analyse /Suivi"                             = "Analyse / Suivi"
        "Assistance technique / Support utilisateur" = "Support utilisateur"
        "Gestion manuelle de compte"                 = "Gestion manuelle"
        "Gestion manuelle de dossiers"               = "Gestion manuelle"
        "Gestion manuelle de paiement"               = "Gestion manuelle"
        "Interoperabilite / Interfaces externes"     = $C_Interop
        "XROAD"                                      = $C_Interop
        "SERAPI RCS"                                 = $C_Interop
        "SRNE"                                       = $C_Interop
        "ORINASA ITERATION 4"                        = $C_Iteration4
        "Optimisation / performance"                 = "Optimisation"
        "Prise en main"                              = "Prise en main"
        "Site web d'information"                     = "Site web"
        "Etat de versement / Reporting"              = "Reporting"
        "Test"                                       = "Test"
    }
    $key = ((Remove-Diacritics $c).Trim()).ToLower()
    foreach ($k in $m.Keys) {
        $kk = ((Remove-Diacritics $k).Trim()).ToLower()
        if ($kk -eq $key) { return $m[$k] }
    }
    return $c.Trim()
}

# ---------------------------------------------------------------
# Normalisation du statut
# ---------------------------------------------------------------
$ACC_u = [char]0x00FB  # u circonflexe
$ACC_i = [char]0x00EE  # i circonflexe

function Normalize-Status([string]$s) {
    if ([string]::IsNullOrWhiteSpace($s)) { return "A faire" }
    $clean = $s.Replace([char]0x2019, [char]0x0027).Replace([char]0x2018, [char]0x0027)
    $low = ((Remove-Diacritics $clean).Trim()).ToLower()
    switch ($low) {
        "a faire"   { return "A faire" }
        "continu"   { return "En cours" }
        "en cours"  { return "En cours" }
        "termine"   { return "Termine" }
        "termines"  { return "Termine" }
        "ok"         { return "OK" }
        "ok - a verifier"             { return "OK - " + $ACC_a + " v" + $ACC_e + "rifier" }
        "ok - pour la creation"       { return "OK - pour la cr" + $ACC_e + "ation" }
        "analyse effectuee developpement des api en cours" { return "Analyse effectu" + $ACC_e + "e / D" + $ACC_e + "veloppement des API en cours" }
        "la passation avec l'equipe it de l'instat a ete effectuee." { return "La passation avec l'" + $ACC_e + "quipe IT de l'INSTAT a " + $ACC_e + "t" + $ACC_e + " effectu" + $ACC_e + "e." }
        "a discuter avec bo apres la maj effectuee concernant ces informations" { return "A discuter avec BO apr" + $ACC_egr + "s la MAJ effectu" + $ACC_e + "e concernant ces informations" }
        default { return $s.Trim() }
    }
}

function Normalize-Prio([string]$p) {
    if ([string]::IsNullOrWhiteSpace($p)) { return "Moyenne" }
    $low = ((Remove-Diacritics $p).Trim()).ToLower()
    switch ($low) {
        "basse"    { return "Basse" }
        "moyenne"  { return "Moyenne" }
        "haute"    { return "Haute" }
        "urgente"  { return "Urgente" }
        "urgent"   { return "Urgente" }
        default    { return $p.Trim() }
    }
}

# ---------------------------------------------------------------
# Normalisation du responsable -> email utilisateur fictif
# ---------------------------------------------------------------
function Normalize-Resp([string]$r) {
    if ([string]::IsNullOrWhiteSpace($r)) { return $null }
    $low = ((Remove-Diacritics $r) -split "`n")[0].Trim().ToLower()
    switch -Regex ($low) {
        "^marko"                          { return "marko@edbm.com" }
        "^tsiaro"                         { return "tsiaro@edbm.com" }
        "^veronique"                      { return "veronique@edbm.com" }
        "^ugd"                            { return "ugd@edbm.com" }
        "^e-tech"                         { return "etech@edbm.com" }
        "^instat"                         { return "instat@edbm.com" }
        "^equipe developpement"           { return "equipe-dev@edbm.com" }
        "^equipe technique"               { return "equipe-technique@edbm.com" }
        "^support technique"              { return "support-technique@edbm.com" }
        default                           { return $null }
    }
}

# ---------------------------------------------------------------
# Lecture du CSV
# ---------------------------------------------------------------
$rows = New-Object System.Collections.Generic.List[object]
Get-Content $csvPath -Encoding UTF8 | Select-Object -Skip 1 | ForEach-Object {
    $f = $_ -split ';'
    for ($i = $f.Count; $i -lt 8; $i++) { $f += "" }
    $rows.Add([pscustomobject]@{
        feuille = $f[0].Trim()
        cat     = $f[1].Trim()
        titre   = $f[2].Trim()
        desc    = $f[3].Trim()
        resp    = $f[4].Trim()
        prio    = $f[5].Trim()
        stat    = $f[6].Trim()
        comm    = $f[7].Trim()
    })
}

$esc = { param($v) ([string]$v).Replace("'", "''") }

# ---------------------------------------------------------------
# cleanup.sql
# ---------------------------------------------------------------
$cl = New-Object System.Collections.Generic.List[string]
$cl.Add("-- =========================================================")
$cl.Add("-- CLEANUP : vidange des donnees applicatives")
$cl.Add("-- Conserve : direction DSI, roles, permissions, statuts par defaut, priorites,")
$cl.Add("--           admin@edbm.com et superadmin@edbm.com")
$cl.Add("-- =========================================================")
$cl.Add("SET session_replication_role = 'replica';")
foreach ($tbl in @("task_attachments","task_comments","comment_reactions","task_assignees","tasks","project_contributors","projects","user_project_permissions","notifications","message_attachments","messages","conversation_members","conversations","activities")) {
    $cl.Add("DELETE FROM $tbl;")
}
$cl.Add("DELETE FROM users_roles WHERE users_users_id NOT IN (SELECT users_id FROM users WHERE email IN ('admin@edbm.com','superadmin@edbm.com'));")
$cl.Add("DELETE FROM users WHERE email NOT IN ('admin@edbm.com','superadmin@edbm.com');")
$cl.Add("DELETE FROM statuses WHERE name NOT IN ('A faire','En cours','Termine');")
$cl.Add("SET session_replication_role = 'origin';")
[System.IO.File]::WriteAllLines($outClean, $cl, [System.Text.UTF8Encoding]::new($true))

# ---------------------------------------------------------------
# seed_test_data.sql
# ---------------------------------------------------------------
$s = New-Object System.Collections.Generic.List[string]
$s.Add("-- =========================================================")
$s.Add("-- SEED TEST DATA : import des taches reelles (95) depuis l'inventaire")
$s.Add("-- Projets = 7 feuilles ; categories = prefixe [..] du titre ;")
$s.Add("-- commentaires -> task_comments (auteur admin)")
$s.Add("-- =========================================================")

# --- 2. Utilisateurs fictifs par responsable ----------------------
$userDefs = @(
    @{ email="equipe-dev@edbm.com"; first="Equipe"; last="Developpement"; job="Equipe developpement"; num="0340000010" },
    @{ email="equipe-technique@edbm.com"; first="Equipe"; last="Technique"; job="Equipe technique"; num="0340000011" },
    @{ email="support-technique@edbm.com"; first="Support"; last="Technique"; job="Support technique"; num="0340000012" },
    @{ email="etech@edbm.com"; first="E"; last="Tech"; job="Equipe E-tech"; num="0340000013" },
    @{ email="instat@edbm.com"; first="INSTAT"; last="Service"; job="Relais INSTAT"; num="0340000014" },
    @{ email="marko@edbm.com"; first="Marko"; last="Responsable"; job="Responsable metier"; num="0340000015" },
    @{ email="tsiaro@edbm.com"; first="Tsiaro"; last="Responsable"; job="Responsable metier"; num="0340000016" },
    @{ email="ugd@edbm.com"; first="UGD"; last="Service"; job="Unite de Gouvernance des Donnees"; num="0340000017" },
    @{ email="veronique@edbm.com"; first="Veronique"; last="Responsable"; job="Responsable metier"; num="0340000018" }
)
$s.Add("")
$s.Add("-- 2. Utilisateurs fictifs (un par responsable/equipe), mdp 12 chiffres en BCrypt")
foreach ($u in $userDefs) {
    $s.Add("INSERT INTO users (created_at, email, firstname, gender, is_active, job, lastname, number, pwd, status, direction_id, image_path)")
    $s.Add("SELECT CURRENT_DATE, '$($u.email)', '$($u.first)', 'M', TRUE, '$($u.job)', '$($u.last)', '$($u.num)', '$BCRYPT', FALSE, d.direction_id, NULL")
    $s.Add("FROM direction d WHERE d.name = 'DSI'")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM users u WHERE u.email = '$($u.email)');")
    $s.Add("INSERT INTO users_roles (users_users_id, roles_roles_id)")
    $s.Add("SELECT u.users_id, r.roles_id FROM users u, roles r")
    $s.Add("WHERE u.email = '$($u.email)' AND r.name = 'USER'")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM users_roles ur WHERE ur.users_users_id = u.users_id AND ur.roles_roles_id = r.roles_id);")
}

# --- 3. Projets (7 feuilles) ---------------------------------------
$projDefs = @(
    @{ id="Orinasa"; title="Orinasa" },
    @{ id="srne"; title="SRNE" },
    @{ id="e-work"; title="E-Work" },
    @{ id="madazef"; title="MADAZEF" },
    @{ id="lsvisa"; title="LS-VISA" },
    @{ id="Orinasa Iteration 4"; title="Orinasa Iteration 4" },
    @{ id="Autres"; title="Autres" }
)
$s.Add("")
$s.Add("-- 3. Projets (7 feuilles = 7 projets)")
foreach ($p in $projDefs) {
    $s.Add("INSERT INTO projects (title, description, start_date, end_date, owner_id, direction_id, is_active, created_at)")
    $s.Add("SELECT '$($p.title)', 'Projet de l''inventaire des taches en cours', CURRENT_DATE, NULL, u.users_id, d.direction_id, TRUE, NOW()")
    $s.Add("FROM users u, direction d")
    $s.Add("WHERE u.email = 'admin@edbm.com' AND d.name = 'DSI'")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM projects p WHERE p.title = '$($p.title)');")
}

# --- 3bis. Statuts specifiques par projet --------------------------
# (doit etre apres les projets pour referencer project_id)
$newStatuses = @(
    "OK",
    ("OK - " + $ACC_a + " v" + $ACC_e + "rifier"),
    ("OK - pour la cr" + $ACC_e + "ation"),
    ("Analyse effectu" + $ACC_e + "e / D" + $ACC_e + "veloppement des API en cours"),
    ("La passation avec l'" + $ACC_e + "quipe IT de l'INSTAT a " + $ACC_e + "t" + $ACC_e + " effectu" + $ACC_e + "e."),
    ("A discuter avec BO apr" + $ACC_egr + "s la MAJ effectu" + $ACC_e + "e concernant ces informations")
)
$statusOrder = @{}
for ($i = 0; $i -lt $newStatuses.Count; $i++) { $statusOrder[$newStatuses[$i]] = 4 + $i }

$statusPerProj = @{}
foreach ($r in $rows) {
    $ns = Normalize-Status $r.stat
    if ($ns -ne "A faire" -and $ns -ne "En cours" -and $ns -ne "Termine") {
        $key = "$($r.feuille)|$ns"
        $statusPerProj[$key] = $ns
    }
}
$s.Add("")
$s.Add("-- 3bis. Statuts specifiques aux projets (project_id renseigne -> supprimables dans l'app)")
$s.Add("-- GET /statuses?projectId=X -> statuts du projet + globaux (project_id NULL)")
foreach ($k in ($statusPerProj.Keys | Sort-Object)) {
    $parts = $k -split '\|'
    $prTitle = ($projDefs | Where-Object { $_.id -eq $parts[0] } | Select-Object -First 1).title
    $name = & $esc $statusPerProj[$k]
    $order = $statusOrder[$statusPerProj[$k]]
    $s.Add("INSERT INTO statuses (name, sort_order, project_id)")
    $s.Add("SELECT '$name', $order, p.project_id FROM projects p")
    $s.Add("WHERE p.title = '$prTitle'")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM statuses st WHERE st.name = '$name' AND st.project_id = p.project_id);")
}

# --- 4. Taches -----------------------------------------------------
$s.Add("")
$s.Add("-- 4. Taches ($($rows.Count)) avec prefixe de titre [Categorie]")
$s.Add("ALTER TABLE tasks ALTER COLUMN description TYPE TEXT;")

foreach ($r in $rows) {
    $normCat = Normalize-Cat $r.cat
    if ($normCat) { $title = "[$normCat] $($r.titre)" } else { $title = $r.titre }
    if ($title.Length -gt 255) { $title = $title.Substring(0, 252) + "..." }
    $normStat = Normalize-Status $r.stat
    $normPrio = Normalize-Prio $r.prio

    $titleEsc = & $esc $title
    $descEsc = & $esc $r.desc
    $normStatEsc = & $esc $normStat
    $normPrioEsc = & $esc $normPrio
    $prTitle = ($projDefs | Where-Object { $_.id -eq $r.feuille } | Select-Object -First 1).title

    $s.Add("INSERT INTO tasks (title, description, due_date, project_id, priority_id, status_id, parent_task_id, is_active, created_at, completed_at)")
    $s.Add("SELECT '$titleEsc', NULLIF('$descEsc',''), NULL, p.project_id, pr.priority_id, st.status_id, NULL, TRUE, NOW(), NULL")
    $s.Add("FROM projects p, priorities pr, statuses st")
    $s.Add("WHERE p.title = '$prTitle' AND pr.name = '$normPrioEsc' AND st.name = '$normStatEsc'")
    $s.Add("AND (st.project_id = p.project_id OR st.project_id IS NULL)")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM tasks t WHERE t.title = '$titleEsc' AND t.project_id = p.project_id);")

    $e = Normalize-Resp $r.resp
    if ($e) {
        $s.Add("INSERT INTO task_assignees (task_id, user_id)")
        $s.Add("SELECT t.task_id, u.users_id FROM tasks t, users u")
        $s.Add("WHERE t.title = '$titleEsc' AND u.email = '$e'")
        $s.Add("AND NOT EXISTS (SELECT 1 FROM task_assignees ta WHERE ta.task_id = t.task_id AND ta.user_id = u.users_id);")
    }

    if ($r.comm -ne '') {
        $commEsc = & $esc $r.comm
        $s.Add("INSERT INTO task_comments (content, task_id, author_id, parent_comment_id, created_at, updated_at)")
        $s.Add("SELECT '$commEsc', t.task_id, u.users_id, NULL, NOW(), NULL")
        $s.Add("FROM tasks t, users u")
        $s.Add("WHERE t.title = '$titleEsc' AND u.email = 'admin@edbm.com'")
        $s.Add("AND NOT EXISTS (SELECT 1 FROM task_comments tc WHERE tc.task_id = t.task_id AND tc.content = '$commEsc');")
    }
}

# --- 5. Contributeurs ----------------------------------------------
$s.Add("")
$s.Add("-- 5. Contributeurs projet (responsables presents dans le projet)")
$projResps = @{}
foreach ($r in $rows) {
    $e = Normalize-Resp $r.resp
    if ($e) { $projResps["$($r.feuille)|$e"] = $true }
}
foreach ($k in $projResps.Keys) {
    $parts = $k -split '\|'
    $prTitle = ($projDefs | Where-Object { $_.id -eq $parts[0] } | Select-Object -First 1).title
    $s.Add("INSERT INTO project_contributors (project_id, user_id, added_at)")
    $s.Add("SELECT p.project_id, u.users_id, NOW() FROM projects p, users u")
    $s.Add("WHERE p.title = '$prTitle' AND u.email = '$($parts[1])'")
    $s.Add("AND NOT EXISTS (SELECT 1 FROM project_contributors pc WHERE pc.project_id = p.project_id AND pc.user_id = u.users_id);")
}

# --- 6. Verification -----------------------------------------------
$s.Add("")
$s.Add("-- 6. Verification")
$s.Add("SELECT (SELECT COUNT(*) FROM tasks) AS nb_taches, (SELECT COUNT(*) FROM users WHERE email NOT IN ('admin@edbm.com','superadmin@edbm.com')) AS nb_utilisateurs, (SELECT COUNT(*) FROM projects) AS nb_projets;")

[System.IO.File]::WriteAllLines($outSeed, $s, [System.Text.UTF8Encoding]::new($true))

Write-Output "OK cleanup.sql + seed_test_data.sql generes"
Write-Output ("Taches attendues : " + $rows.Count)