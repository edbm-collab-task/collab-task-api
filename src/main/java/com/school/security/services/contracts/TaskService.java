package com.school.security.services.contracts;

import com.school.security.dtos.requests.TaskReqDto;
import com.school.security.dtos.responses.TaskResDto;
import java.util.List;

public interface TaskService extends Service<TaskReqDto, TaskResDto, Long> {
    TaskResDto save(TaskReqDto toSave, Long id);

    TaskResDto archiver(Long id);

    TaskResDto changerStatut(Long taskId, Long statusId);

    List<TaskResDto> findByProject(Long projectId);
}
