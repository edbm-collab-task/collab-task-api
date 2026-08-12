package com.school.security.services.contracts;

import com.school.security.dtos.requests.ProjectReqDto;
import com.school.security.dtos.responses.ProjectResDto;

public interface ProjectService extends Service<ProjectReqDto, ProjectResDto, Long> {
    ProjectResDto save(ProjectReqDto toSave, Long id);

    ProjectResDto archiver(Long id);
}
