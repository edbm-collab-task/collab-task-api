package com.school.security.controllers.api;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import com.school.security.services.contracts.TaskService;
import java.util.List;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public List<TaskResDto> findAllTasks() {
        return this.taskService.findAll();
    }

    @GetMapping("/{id}")
    public TaskResDto getTaskById(@PathVariable Long id) {
        return this.taskService.findById(id);
    }

    @GetMapping("/project/{projectId}")
    public List<TaskResDto> findTasksByProject(@PathVariable Long projectId) {
        return this.taskService.findByProject(projectId);
    }

    @PostMapping
    public TaskResDto createTask(@RequestBody TaskReqDto taskReqDto) {
        return this.taskService.createOrUpdate(taskReqDto);
    }

    @PutMapping("/{id}")
    public TaskResDto updateTask(@RequestBody TaskReqDto toSave, @PathVariable Long id) {
        return this.taskService.save(toSave, id);
    }

    @DeleteMapping("/{id}")
    public TaskResDto archiveTask(@PathVariable Long id) {
        return this.taskService.deleteById(id);
    }

    @PatchMapping("/{taskId}/status")
    public TaskResDto changeTaskStatus(
            @PathVariable Long taskId, @RequestParam Long statusId) {
        return this.taskService.changerStatut(taskId, statusId);
    }
}
