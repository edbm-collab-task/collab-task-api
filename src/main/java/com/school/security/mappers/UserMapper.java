package com.school.security.mappers;

import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.RoleResDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import java.util.List;
import java.util.stream.Collectors;
import com.school.security.repositories.DirectionRepository;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Mapper utilisateur entre entité, DTOs de demande et DTOs de réponse.
 *
 * <p>Fonctionnement constaté (documenté, non modifié) :
 * <ul>
 *   <li>{@code fromDto} : l'email est trimé et mis en minuscule ; la {@code Direction}
 *       est résolue par {@code getReferenceById} ; le mot de passe est stocké en clair
 *       (codage BCrypt assuré par l'appelant dans {@link #toUpdate}).</li>
 *   <li>{@code toDto} : le libellé du rôle provient du premier élément de la liste
 *       {@code roles} ( {@code .getFirst() } ); {@code imagePath} est inclus.</li>
 *   <li>{@code toUserReq} : conversion partielle vers le DTO de demande.</li>
 *   <li>{@code toUpdate} : mise à jour partielle ; si le mot de passe est fourni,
 *       il est codé via {@code BCryptPasswordEncoder}.</li>
 *   <li>dépendance injected : {@code RoleMapper}, {@code DirectionRepository},
 *       {@code BCryptPasswordEncoder}.</li>
 * </ul>
 */
@Component
public class UserMapper implements Mapper<UserReqDto, User, UserResDto> {
    private final RoleMapper roleMapper;
    private final DirectionRepository directionRepository;
    private final BCryptPasswordEncoder passwordEncoder;

    public UserMapper(RoleMapper roleMapper,  DirectionRepository directionRepository, BCryptPasswordEncoder passwordEncoder) {
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
                entity.getIsActive(),
                entity.getCreatedAt(),
                entity.getRoles().getFirst().getName(),
                entity.getImagePath());
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

    public User toUpdate(UserReqDto toUpdate , User user){

        if (toUpdate.firstname() != null && !toUpdate.firstname().isBlank()){
            user.setFirstname(toUpdate.firstname());
        }
        if (toUpdate.lastname() != null && !toUpdate.lastname().isBlank()){
            user.setLastname(toUpdate.lastname());
        }
        if (toUpdate.gender() != null && !toUpdate.gender().name().isBlank()){
            user.setGender(toUpdate.gender());
        }
        if (toUpdate.number() != null && !toUpdate.number().isBlank()){
            user.setNumber(toUpdate.number());
        }
        if (toUpdate.job() != null && !toUpdate.job().isBlank()){
            user.setJob(toUpdate.job());
        }
        if (toUpdate.password() != null && !toUpdate.password().isBlank()) {
            user.setPwd(passwordEncoder.encode(toUpdate.password()));
        }
        if ((toUpdate.directionId() != null )){
            user.setDirection(directionRepository.getReferenceById(toUpdate.directionId()));
        }


        return user;
    }

    private List<RoleResDto> toRoleResDto(List<Role> roles) {
        return roles.stream().map(this.roleMapper::toDto).collect(Collectors.toList());
    }
}
