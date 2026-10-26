package com.school.security.mappers;

import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Direction;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.exceptions.EntityException;
import com.school.security.repositories.DirectionRepository;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper utilisateur entre entité, DTOs de demande et DTOs de réponse.
 *
 * <p>La direction est facultative : {@code directionId} peut être absent
 * ({@code null}, aucune modification), explicitement vide ({@code Optional.empty()},
 * retrait de la direction) ou renseigné ({@code Optional.of(id)}, direction
 * résolue et validée en base).
 */
@Component
public class UserMapper implements Mapper<UserReqDto, User, UserResDto> {
    private final RoleMapper roleMapper;
    private final DirectionRepository directionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserMapper(
            RoleMapper roleMapper,
            DirectionRepository directionRepository,
            BCryptPasswordEncoder passwordEncoder) {
        this.roleMapper = roleMapper;
        this.directionRepository = directionRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User fromDto(UserReqDto d) {
        User user = new User();
        user.setEmail(d.email() != null ? d.email().trim().toLowerCase() : null);
        user.setNumber(d.number());
        user.setFirstname(d.firstname());
        user.setGender(d.gender());
        user.setDirection(resolveDirection(d.directionId()));
        user.setJob(d.job());
        user.setPwd(d.password());
        user.setLastname(d.lastname());
        user.setStatus(d.status());
        return user;
    }

    @Override
    public UserResDto toDto(User entity) {
        Direction direction = entity.getDirection();
        return new UserResDto(
                entity.getUsersId(),
                entity.getFirstname(),
                entity.getLastname(),
                entity.getEmail(),
                entity.getNumber(),
                direction != null ? direction.getName() : null,
                entity.getJob(),
                entity.getGender(),
                entity.getStatus(),
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getRoles().getFirst().getName(),
                entity.getRoles().getFirst().getCodeRole(),
                entity.getImagePath());
    }

    public UserReqDto toUserReq(User entity) {
        Direction direction = entity.getDirection();
        return new UserReqDto(
                entity.getFirstname(),
                entity.getLastname(),
                entity.getJob(),
                direction != null
                        ? Optional.of(direction.getDirectionId())
                        : Optional.empty(),
                entity.getEmail(),
                entity.getNumber(),
                entity.getStatus(),
                entity.getPassword(),
                entity.getGender(),
                entity.getRoles().getFirst().getName());
    }

    public User toUpdate(UserReqDto toUpdate, User user) {

        if (toUpdate.firstname() != null && !toUpdate.firstname().isBlank()) {
            user.setFirstname(toUpdate.firstname());
        }
        if (toUpdate.lastname() != null && !toUpdate.lastname().isBlank()) {
            user.setLastname(toUpdate.lastname());
        }
        if (toUpdate.gender() != null && !toUpdate.gender().name().isBlank()) {
            user.setGender(toUpdate.gender());
        }
        if (toUpdate.number() != null && !toUpdate.number().isBlank()) {
            user.setNumber(toUpdate.number());
        }
        if (toUpdate.job() != null && !toUpdate.job().isBlank()) {
            user.setJob(toUpdate.job());
        }
        if (toUpdate.password() != null && !toUpdate.password().isBlank()) {
            user.setPwd(passwordEncoder.encode(toUpdate.password()));
        }
        if (toUpdate.directionId() != null) {
            user.setDirection(resolveDirection(toUpdate.directionId()));
        }

        return user;
    }

    /**
     * Résout une direction à partir de son identifiant. Un identifiant absent ou
     * vide ne déclenche aucune requête ; un identifiant fourni mais inconnu est
     * rejeté.
     */
    public Direction resolveDirection(Optional<Long> directionId) {
        if (directionId == null || directionId.isEmpty()) {
            return null;
        }
        Long id = directionId.get();
        return directionRepository
                .findById(id)
                .orElseThrow(() -> new EntityException("Direction not found with ID " + id));
    }

    private List<RoleResDto> toRoleResDto(List<Role> roles) {
        return roles.stream().map(this.roleMapper::toDto).collect(Collectors.toList());
    }
}
