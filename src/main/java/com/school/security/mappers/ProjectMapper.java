package com.school.security.mappers;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.Project;
import com.school.security.repositories.UserRepository;
import org.springframework.stereotype.Component;

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
        project.setOwner(userRepository.getReferenceById(d.ownerId()));
        return project;
    }

    @Override
    public ProjectResDto toDto(Project entity) {
        return new ProjectResDto(
                entity.getProjectId(),
                entity.getTitle(),
                entity.getDescription(),
                entity.getStartDate(),
                entity.getEndDate(),
                entity.getIsActive(),
                entity.getOwner().getUsersId(),
                entity.getOwner().getFirstname() + " " + entity.getOwner().getLastname());
    }
}
