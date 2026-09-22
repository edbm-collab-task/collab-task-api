package com.school.security.services.implementations;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.Project;
import com.school.security.entities.ProjectContributor;
import com.school.security.entities.User;
import com.school.security.entities.UserProjectPermission;
import com.school.security.enums.PermissionType;
import com.school.security.enums.RoleType;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.ProjectMapper;
import com.school.security.repositories.ProjectContributorRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.UserProjectPermissionRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.ProjectService;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Gère les règles métier liées aux projets.
 *
 * <p>L'archivage est utilisé à la place d'une suppression physique afin de
 * conserver l'historique : un projet archivé reste en base, marqué par
 * {@code isActive = false}. L'archivage est réversible (unarchiver). Les listes
 * n'exposent que les projets actifs, à l'exception des administrateurs qui voient
 * également les projets archivés. À la création, l'owner est automatiquement
 * ajouté comme contributeur et reçoit la permission par projet
 * {@code MANAGE_PROJECT_CONTRIBUTORS}.
 */
@Service
@Transactional
@AllArgsConstructor
public class ProjectServiceImpl implements ProjectService {

    private ProjectRepository projectRepository;
    private ProjectMapper projectMapper;
    private UserRepository userRepository;
    private ProjectContributorRepository contributorRepository;
    private UserProjectPermissionRepository userProjectPermissionRepository;

    @Override
    public ProjectResDto createOrUpdate(ProjectReqDto toSave) {
        return save(toSave, null, null);
    }

    /**
     * Crée un projet en rattachant l'owner donné (l'utilisateur courant).
     *
     * <p>L'owner est automatiquement enregistré comme contributeur et reçoit la
     * permission par projet {@code MANAGE_PROJECT_CONTRIBUTORS}. Cette permission
     * est celle qui lui permettra ensuite de gérer les contributeurs ou de
     * transférer la propriété ({@code PermissionEvaluator.hasProjectPermission}).
     */
    @Override
    public ProjectResDto createWithOwner(ProjectReqDto toSave, Long ownerId) {
        validateDates(toSave.startDate(), toSave.endDate(), null);
        Project project = this.projectMapper.fromDto(toSave);
        this.projectMapper.setOwner(project, ownerId);
        Project saved = this.projectRepository.save(project);

        ProjectContributor ownerAsContributor = new ProjectContributor();
        ownerAsContributor.setProject(saved);
        ownerAsContributor.setUser(userRepository.getReferenceById(ownerId));
        ownerAsContributor.setAddedAt(LocalDateTime.now());
        contributorRepository.save(ownerAsContributor);

        UserProjectPermission perm = new UserProjectPermission();
        perm.setUser(userRepository.getReferenceById(ownerId));
        perm.setProject(saved);
        perm.setPermissionName(PermissionType.MANAGE_PROJECT_CONTRIBUTORS);
        userProjectPermissionRepository.save(perm);

        return this.projectMapper.toDto(saved, ownerId);
    }

    /**
     * Transfère la propriété d'un projet à un autre utilisateur.
     *
     * <p>Toute l'opération est réalisée dans une seule transaction : la permission
     * par projet {@code MANAGE_PROJECT_CONTRIBUTORS} est retirée à l'ancien owner
     * et accordée au nouveau, l'ancien owner quitte la liste des contributeurs et
     * le nouveau y est ajouté uniquement s'il n'en faisait pas déjà partie.
     */
    @Override
    @Transactional
    public void transferOwnership(Long projectId, Long oldOwnerId, Long newOwnerId) {
        Project project = projectRepository.getReferenceById(projectId);
        project.setOwner(userRepository.getReferenceById(newOwnerId));
        projectRepository.save(project);

        userProjectPermissionRepository.deleteByUserIdAndProjectIdAndPermission(
                oldOwnerId, projectId, PermissionType.MANAGE_PROJECT_CONTRIBUTORS);

        UserProjectPermission perm = new UserProjectPermission();
        perm.setUser(userRepository.getReferenceById(newOwnerId));
        perm.setProject(project);
        perm.setPermissionName(PermissionType.MANAGE_PROJECT_CONTRIBUTORS);
        userProjectPermissionRepository.save(perm);

        contributorRepository.deleteByProjectProjectIdAndUserUsersId(projectId, oldOwnerId);

        if (!contributorRepository.existsByProjectProjectIdAndUserUsersId(projectId, newOwnerId)) {
            ProjectContributor newContributor = new ProjectContributor();
            newContributor.setProject(project);
            newContributor.setUser(userRepository.getReferenceById(newOwnerId));
            newContributor.setAddedAt(LocalDateTime.now());
            contributorRepository.save(newContributor);
        }
    }

    /**
     * Crée ou met à jour un projet : si {@code id} est renseigné et existe,
     * les champs modifiables sont appliqués sur l'entité existante, sinon un
     * nouveau projet est créé. Les dates sont contrôlées avant enregistrement.
     *
     * <p>Pour une mise à jour ({@code id} présent), seuls l'utilisateur qui
     * possède le projet (le {@code currentUserId}) peut modifier ; tout autre
     * utilisateur reçoit une {@code EntityException}. La création (sans
     * {@code id}) n'applique aucune vérification d'owner.
     */
    @Override
    public ProjectResDto save(ProjectReqDto toSave, Long id, Long currentUserId) {
        if (id != null) {
            Optional<Project> projectOptional = this.projectRepository.findById(id);
            if (projectOptional.isPresent()) {
                Project projectToUpdate = projectOptional.get();
                if (!projectToUpdate.getOwner().getUsersId().equals(currentUserId)) {
                    throw new EntityException("Seul le chef de projet peut modifier ce projet");
                }
                validateDates(
                        toSave.startDate(),
                        toSave.endDate(),
                        projectToUpdate.getStartDate()
                );
                projectToUpdate.setTitle(toSave.title());
                projectToUpdate.setDescription(toSave.description());
                projectToUpdate.setStartDate(toSave.startDate());
                projectToUpdate.setEndDate(toSave.endDate());
                return this.projectMapper.toDto(this.projectRepository.save(projectToUpdate));
            }
        }
        Project projectToSave = this.projectMapper.fromDto(toSave);
        return this.projectMapper.toDto(this.projectRepository.save(projectToSave));
    }

    /**
     * Retourne uniquement les projets actifs afin que les projets archivés
     * restent exclus des opérations courantes.
     */
    @Override
    public List<ProjectResDto> findAll() {
        return this.projectRepository.findByIsActiveTrue().stream()
                .map(this.projectMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Liste les projets selon le rôle de l'utilisateur courant : un administrateur
     * (ADMIN/SUPER_ADMIN) voit tous les projets actifs, un utilisateur simple ne
     * voit que les projets dont il est owner ou contributeur.
     */
    @Override
    public List<ProjectResDto> findAllWithUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleType.ADMIN.name()) || role.getName().equals(RoleType.SUPER_ADMIN.name()));

        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findByIsActiveTrue();
        } else {
            projects = projectRepository.findActiveByOwnerOrContributor(currentUserId);
        }

        return projects.stream()
                .map(p -> projectMapper.toDto(p, currentUserId))
                .collect(Collectors.toList());
    }

    /**
     * Même visibilité que {@code findAllWithUser} mais inclut cette fois les
     * projets archivés : réservé à la consultation d'historique. Les
     * administrateurs voient l'intégralité des projets (actifs et archivés), les
     * utilisateurs simples uniquement ceux dont ils sont owner ou contributeur.
     */
    @Override
    public List<ProjectResDto> findAllWithUserIncludingArchived(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName().equals(RoleType.ADMIN.name()) || role.getName().equals(RoleType.SUPER_ADMIN.name()));

        List<Project> projects;
        if (isAdmin) {
            projects = projectRepository.findAll();
        } else {
            projects = projectRepository.findAllByOwnerOrContributor(currentUserId);
        }

        return projects.stream()
                .map(p -> projectMapper.toDto(p, currentUserId))
                .collect(Collectors.toList());
    }

    @Override
    public ProjectResDto findById(Long id) {
        Optional<Project> projectOptional = this.projectRepository.findById(id);
        if (projectOptional.isPresent()) {
            return this.projectMapper.toDto(projectOptional.get());
        }
        throw new EntityException("Project not found");
    }

    @Override
    public ProjectResDto findByIdWithUser(Long id, Long currentUserId) {
        Optional<Project> projectOptional = this.projectRepository.findById(id);
        if (projectOptional.isPresent()) {
            return this.projectMapper.toDto(projectOptional.get(), currentUserId);
        }
        throw new EntityException("Project not found");
    }

    /**
     * "Supprime" un projet en le basculant en archivé : aucune suppression
     * physique, l'enregistrement demeure dans la base avec {@code isActive = false}.
     */
    @Override
    public ProjectResDto deleteById(Long id) {
        return archiver(id);
    }

    /**
     * Archive un projet (soft delete) afin de conserver l'historique tout en le
     * retirant des listes et statistiques courantes. Opération réversible via
     * {@code unarchiver}.
     */
    @Override
    public ProjectResDto archiver(Long id) {
        Optional<Project> projectOptional = this.projectRepository.findById(id);
        if (projectOptional.isPresent()) {
            Project project = projectOptional.get();
            project.setIsActive(false);
            return this.projectMapper.toDto(this.projectRepository.save(project));
        }
        throw new EntityException("Project not found");
    }

    @Override
    public ProjectResDto unarchiver(Long id) {
        Optional<Project> projectOptional = this.projectRepository.findById(id);
        if (projectOptional.isPresent()) {
            Project project = projectOptional.get();
            project.setIsActive(true);
            return this.projectMapper.toDto(this.projectRepository.save(project));
        }
        throw new EntityException("Project not found");
    }

    /**
     * Contrôle les dates d'un projet : la date de début est obligatoire et doit
     * rester dans le futur, la date de fin ne peut pas précéder la date de début.
     *
     * <p>La règle "pas de date de début passée" n'est appliquée qu'en cas de
     * modification de cette date (par rapport à l'originale). Cela permet de
     * mettre à jour un projet existant dont la date de début est déjà dans le
     * passé sans être bloqué, tant qu'on ne la modifie pas.
     */
    private void validateDates(LocalDate startDate, LocalDate endDate, LocalDate originalStartDate) {
        if (startDate == null) {
            throw new BadRequestException(
                    "La date de début est obligatoire."
            );
        }
        boolean startChanged = originalStartDate == null || !originalStartDate.equals(startDate);
        if (startChanged && startDate.isBefore(LocalDate.now())) {
            throw new BadRequestException(
                    "La date de début ne peut pas être dans le passé."
            );
        }
        if (endDate != null && startDate.isAfter(endDate)) {
            throw new BadRequestException(
                    "La date de début doit être antérieure ou égale à la date de fin."
            );
        }
    }
}
