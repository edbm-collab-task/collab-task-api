package com.school.security.mappers;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.Project;
import com.school.security.repositories.UserRepository;
import org.springframework.stereotype.Component;

/**
 * Mapper projet entre entité, DTO de demande et DTO de réponse.
 *
 * <p>Fonctionnement constaté (documenté, non modifié) :
 * <ul>
 *   <li>{@code fromDto} : seules les champs simples {@code title}, {@code description},
 *       {@code startDate}, {@code endDate} sont copiés.</li>
 *   <li>{@code setOwner} : résout l'entité {@code User} par {@code getReferenceById}.</li>
 *   <li>{@code toDto(currentUserId)} : inclut un champ {@code isOwner} comparant
 *       {@code currentUserId} à l'id du propriétaire.</li>
 * </ul>
 */
@Component
public class ProjectMapper implements Mapper<ProjectReqDto, Project, ProjectResDto> {

    private final UserRepository userRepository;

    public ProjectMapper(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public Project fromDto(ProjectReqDto d) {
        Project project = new Project();
        project.setTitle(d.title());
        project.setDescription(d.description());
        project.setStartDate(d.startDate());
        project.setEndDate(d.endDate());
        return project;
    }

    public void setOwner(Project project, Long ownerId) {
        project.setOwner(userRepository.getReferenceById(ownerId));
    }

    @Override
    public ProjectResDto toDto(Project entity) {
        return toDto(entity, null);
    }

    public ProjectResDto toDto(Project entity, Long currentUserId) {
        return new ProjectResDto(
                entity.getProjectId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getIsActive(),
                entity.getOwner().getUsersId(),
                entity.getOwner().getFirstname() + " " + entity.getOwner().getLastname(),
                currentUserId != null && currentUserId.equals(entity.getOwner().getUsersId()));
    }
}
