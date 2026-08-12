package com.school.security.controllers.api;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import com.school.security.services.contracts.ProjectService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/projects")
public class ProjectController {

    private final ProjectService projectService;

    public ProjectController(ProjectService projectService) {
        this.projectService = projectService;
    }

    @GetMapping
    public List<ProjectResDto> findAllProjects() {
        return this.projectService.findAll();
    }

    @GetMapping("/{id}")
    public ProjectResDto getProjectById(@PathVariable Long id) {
        return this.projectService.findById(id);
    }

    @PostMapping
    public ProjectResDto createProject(@Valid @RequestBody ProjectReqDto projectReqDto) {
        return this.projectService.createOrUpdate(projectReqDto);
    }

    @PutMapping("/{id}")
    public ProjectResDto updateProject(
            @Valid @RequestBody ProjectReqDto toSave, @PathVariable Long id) {
        return this.projectService.save(toSave, id);
    }

    @DeleteMapping("/{id}")
    public ProjectResDto archiveProject(@PathVariable Long id) {
        return this.projectService.deleteById(id);
    }
}
