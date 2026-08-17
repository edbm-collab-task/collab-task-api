package com.school.security.controllers.api;

import com.school.security.dtos.responses.StatusResDto;
import com.school.security.dtos.requests.StatusReqDto;
import com.school.security.entities.Status;
import com.school.security.entities.Project;
import com.school.security.repositories.StatusRepository;
import com.school.security.repositories.ProjectRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/statuses")
public class StatusController {

    @Autowired
    private StatusRepository statusRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @GetMapping
    public ResponseEntity<List<StatusResDto>> getAll(
            @RequestParam(required = false) Long projectId) {
        List<Status> statuses;
        if (projectId != null) {
            statuses = statusRepository.findByProjectProjectIdOrProjectIsNullOrderBySortOrderAsc(projectId);
        } else {
            statuses = statusRepository.findAll();
        }
        List<StatusResDto> dtos = statuses.stream()
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @PostMapping
    public ResponseEntity<StatusResDto> create(@RequestBody StatusReqDto dto) {
        Status status = new Status();
        status.setName(dto.name());
        status.setSortOrder(dto.sortOrder());
        if (dto.projectId() != null) {
            Project project = projectRepository.findById(dto.projectId())
                    .orElseThrow(() -> new RuntimeException("Projet non trouvé"));
            status.setProject(project);
        }
        Status saved = statusRepository.save(status);
        return ResponseEntity.ok(toDto(saved));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<?> delete(@PathVariable Long id) {
        Status status = statusRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Statut non trouvé"));
        if (status.getProject() == null) {
            return ResponseEntity.badRequest().body(
                    Map.of("message", "Impossible de supprimer un statut par défaut"));
        }
        statusRepository.delete(status);
        return ResponseEntity.ok(Map.of("message", "Statut supprimé"));
    }

    private StatusResDto toDto(Status entity) {
        return new StatusResDto(
                entity.getStatusId(),
                entity.getName(),
                entity.getSortOrder(),
                entity.getProject() != null ? entity.getProject().getProjectId() : null
        );
    }
}
