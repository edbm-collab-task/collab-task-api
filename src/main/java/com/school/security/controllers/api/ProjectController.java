package com.school.security.controllers.api;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.entities.User;
import com.school.security.mappers.ProjectMapper;
import com.school.security.repositories.UserRepository;
import com.school.security.securities.utils.SecurityUtils;
import com.school.security.services.contracts.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;
    private final ProjectMapper projectMapper;
    private final UserRepository userRepository;

    public ProjectController(ProjectService projectService, ProjectMapper projectMapper, UserRepository userRepository) {
        this.projectService = projectService;
        this.projectMapper = projectMapper;
        this.userRepository = userRepository;
    }

    private Long getCurrentUserId() {
        String email = SecurityUtils.getCurrentUsername();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        return user.getUsersId();
    }

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findAllProjects() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUser(currentUserId);
    }

    @GetMapping("/all")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findAllProjectsIncludingArchived() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUserIncludingArchived(currentUserId);
    }

    @GetMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto getProjectById(@PathVariable Long id) {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findByIdWithUser(id, currentUserId);
    }

    @PostMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto createProject(@Valid @RequestBody ProjectReqDto projectReqDto) {
        Long currentUserId = getCurrentUserId();
        return this.projectService.createWithOwner(projectReqDto, currentUserId);
    }

    @PutMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto updateProject(
            @Valid @RequestBody ProjectReqDto toSave, @PathVariable Long id) {
        return this.projectService.save(toSave, id);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto archiveProject(@PathVariable Long id) {
        return this.projectService.deleteById(id);
    }

    @PatchMapping("/{id}/unarchive")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public ProjectResDto unarchiveProject(@PathVariable Long id) {
        return this.projectService.unarchiver(id);
    }

    @GetMapping("/archived")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_PROJECTS')")
    public List<ProjectResDto> findArchivedProjects() {
        Long currentUserId = getCurrentUserId();
        return this.projectService.findAllWithUserIncludingArchived(currentUserId)
                .stream()
                .filter(p -> !p.isActive())
                .collect(java.util.stream.Collectors.toList());
    }
}
