package com.school.security.services.contracts;

import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.User;
import org.springframework.core.io.Resource;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface UserService extends Service<UserReqDto, UserResDto, Long> {

    UserResDto create(UserReqDto toSave);

    UserResDto addImageToUser(Long id, MultipartFile image);

    Resource getUserImage(Long id);

    UserResDto attachRole(String email, String name);

    UserResDto detachRole(String email, String name);

    UserDetailsService userDetailsService();

    User findByEmail(String email);

    List<UserResDto> findAllUserActive();

    List<UserResDto> findAllUserDisable();

    UserResDto updatePassword(String email, String newPassword);

    UserResDto getUserRestByEmail(String email);

    Long getAccountNoRole();

    void updateStatus(String email, Boolean status);

    void updateAccount(String email, Boolean isActive);

    List<UserResDto> findAllByRole(String roleType);

    List<UserResDto> findPotentialContributors(Long projectId);
}
