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
        return save(toSave, null);
    }

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

    @Override
    public ProjectResDto save(ProjectReqDto toSave, Long id) {
        if (id != null) {
            Optional<Project> projectOptional = this.projectRepository.findById(id);
            if (projectOptional.isPresent()) {
                Project projectToUpdate = projectOptional.get();
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

    @Override
    public List<ProjectResDto> findAll() {
        return this.projectRepository.findByIsActiveTrue().stream()
                .map(this.projectMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<ProjectResDto> findAllWithUser(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.ADMIN || role.getName() == RoleType.SUPER_ADMIN);

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

    @Override
    public List<ProjectResDto> findAllWithUserIncludingArchived(Long currentUserId) {
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new RuntimeException("User not found"));

        boolean isAdmin = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleType.ADMIN || role.getName() == RoleType.SUPER_ADMIN);

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

    @Override
    public ProjectResDto deleteById(Long id) {
        return archiver(id);
    }

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
