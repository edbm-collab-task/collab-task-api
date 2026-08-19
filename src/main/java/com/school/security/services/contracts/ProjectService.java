package com.school.security.services.contracts;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;
import java.util.List;

public interface ProjectService extends Service<ProjectReqDto, ProjectResDto, Long> {
    ProjectResDto save(ProjectReqDto toSave, Long id);

    ProjectResDto archiver(Long id);

    ProjectResDto createWithOwner(ProjectReqDto toSave, Long ownerId);

    ProjectResDto findByIdWithUser(Long id, Long currentUserId);

    List<ProjectResDto> findAllWithUser(Long currentUserId);
}
