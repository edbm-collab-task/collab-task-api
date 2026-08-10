package com.school.security.mappers;

import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import java.util.List;
import java.util.stream.Collectors;

import com.school.security.enums.Gender;
import com.school.security.repositories.DirectionRepository;
import org.springframework.stereotype.Component;

@Component
public class UserMapper implements Mapper<UserReqDto, User, UserResDto> {
    private final RoleMapper roleMapper;
    private final DirectionRepository directionRepository;

    public UserMapper(RoleMapper roleMapper,  DirectionRepository directionRepository) {
        this.roleMapper = roleMapper;
        this.directionRepository = directionRepository;
    }

    @Override
    public User fromDto(UserReqDto d) {
        User user = new User();
        user.setEmail(d.email());
        user.setNumber(d.number());
        user.setFirstname(d.firstname());
        user.setGender(d.gender());
        user.setDirection(directionRepository.getReferenceById(d.directionId()));
        user.setJob(d.job());
        user.setPwd(d.password());
        user.setLastname(d.lastname());
        user.setStatus(d.status());
        return user;
    }

    @Override
    public UserResDto toDto(User entity) {
        return new UserResDto(
                entity.getUsersId(),
                entity.getFirstname(),
                entity.getLastname(),
                entity.getEmail(),
                entity.getNumber(),
                entity.getDirection().getName(),
                entity.getJob(),
                entity.getGender(),
                entity.getStatus(),
                entity.getCreatedAt(),
                entity.getRoles().getFirst().getName());
    }

    public UserReqDto toUserReq(User entity){
     return  new UserReqDto(
             entity.getFirstname(),
             entity.getLastname(),
             entity.getJob(),
             entity.getDirection().getDirectionId(),
             entity.getEmail(),
             entity.getNumber(),
             entity.getStatus(),
             entity.getPassword(),
             entity.getGender()
     );
    }

    private List<RoleResDto> toRoleResDto(List<Role> roles) {
        return roles.stream().map(this.roleMapper::toDto).collect(Collectors.toList());
    }
}
