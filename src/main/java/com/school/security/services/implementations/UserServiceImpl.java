package com.school.security.services.implementations;

import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.exceptions.BadRequestException;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.UserMapper;
import com.school.security.repositories.*;
import com.school.security.securities.services.FileStorageService;
import com.school.security.services.contracts.UserService;
import org.springframework.core.io.Resource;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import static com.school.security.controllers.auth.AuthController.generateRandomNumericString;

/**
 * Gère les règles métier liées aux utilisateurs : création de comptes, encodage
 * des mots de passe (BCrypt), attribution des rôles, activation/désactivation,
 * photos de profil.
 *
 * <p>Contrairement aux projets et aux tâches, la suppression d'un utilisateur est
 * physique (deleteById). Deux flux de création coexistent : createOrUpdate pour
 * l'auto-inscription ou la mise à jour par email, et create pour la création par
 * un administrateur (mot de passe temporaire envoyé par email, réactivation
 * automatique d'un compte désactivé portant le même email).
 */
@Service
@Transactional
@AllArgsConstructor
public class UserServiceImpl implements UserService {
    private UserMapper userMapper;
    private BCryptPasswordEncoder passwordEncoder;
    private RoleRepository roleRepository;
    private UserRepository userRepository;
    private DirectionRepository directionRepository;
    private EmailService emailService;
    private final FileStorageService fileStorageService;
    private final TaskRepository taskRepository;

    /**
     * Auto-inscription ou mise à jour : si l'email normalisé existe déjà, le
     * compte existant est mis à jour ; sinon un nouvel utilisateur est créé,
     * marqué hors ligne (status = false), daté du jour et rattaché au rôle par
     * défaut USER.
     */
    @Override
    public UserResDto createOrUpdate(UserReqDto toSave) {
        validatePasswordLength(toSave.password());
        validateNameContainsNoDigits(toSave.firstname(), "Le nom");
        validateNameContainsNoDigits(toSave.lastname(), "Le prénom");

        String email = normalizeEmail(toSave.email());
        Optional<User> userOptional = userRepository.findByEmail(email);

        if (userOptional.isPresent()) {
            User user = userOptional.get();
            User toUpdate = userMapper.toUpdate(toSave,user);
            userRepository.save(toUpdate);
            return userMapper.toDto(toUpdate);

        } else {
            User user = userMapper.fromDto(toSave);

            if (toSave.password() != null && !toSave.password().isBlank()) {
                user.setPwd(passwordEncoder.encode(toSave.password()));
            }
            user.setStatus(false);
            user.setCreatedAt(LocalDate.now());
            var userToSave = userRepository.save(user);
            attachRole(email,RoleType.USER);
            return userMapper.toDto(userToSave);
        }
    }



    /**
     * Création d'un compte par un administrateur : un mot de passe temporaire de
     * 12 chiffres est généré aléatoirement et communiqué par email. Si un compte
     * existe déjà avec cet email, il est mis à jour et réactivé
     * ({@code isActive = true}) s'il avait été désactivé.
     */
    @Override
    public UserResDto create(UserReqDto toSave) {
        validateNameContainsNoDigits(toSave.firstname(), "Le nom");
        validateNameContainsNoDigits(toSave.lastname(), "Le prénom");

        Optional<User> userOptional = userRepository.findByEmail(normalizeEmail(toSave.email()));

        if(userOptional.isEmpty()){

            String code = generateRandomNumericString(12);

            User user = new User();
            user.setEmail(normalizeEmail(toSave.email()));
            user.setPwd(passwordEncoder.encode(code));
            user.setCreatedAt(LocalDate.now());
            user.setDirection(directionRepository.getReferenceById(toSave.directionId()));
            user.setFirstname(toSave.firstname());
            user.setLastname(toSave.lastname());
            User savedUser = userRepository.save(user);

            attachRole(savedUser.getEmail(), RoleType.USER);
            emailService.createPwdForUser(
                    savedUser.getEmail(),
                    savedUser.getFirstname(),
                    code
            );

            return userMapper.toDto(user);

        }else {

            User user = userOptional.get();
            User toUpdate = userMapper.toUpdate(toSave,user);
            if (!toUpdate.getIsActive()){
                toUpdate.setIsActive(true);
            }
            userRepository.save(toUpdate);

            return userMapper.toDto(toUpdate);
        }
    }

    @Override
    public UserResDto addImageToUser(Long id, MultipartFile image) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityException(
                                "User not found with ID " + id
                        )
                );

        if (image != null && !image.isEmpty()) {

            // Supprimer l'ancienne image
            if (user.getImagePath() != null
                    && !user.getImagePath().isBlank()) {

                fileStorageService.deleteUserImage(
                        user.getImagePath()
                );
            }

            // Sauvegarder la nouvelle image
            String imagePath =
                    fileStorageService.saveUserImage(image);

            user.setImagePath(imagePath);
        }

        User savedUser = userRepository.save(user);

        return userMapper.toDto(savedUser);
    }

    @Override
    public Resource getUserImage(Long id) {

        User user = userRepository.findById(id)
                .orElseThrow(() ->
                        new EntityException(
                                "User not found with ID " + id
                        )
                );

        if (user.getImagePath() == null || user.getImagePath().isBlank()) {
            throw new EntityException(
                    "User does not have an image"
            );
        }

        return fileStorageService.loadUserImage(user.getImagePath());
    }

    @Override
    public List<UserResDto> findAll() {
        return this.userRepository.findAll().stream()
                .map(this.userMapper::toDto)
                .collect(Collectors.toList());
    }



    @Override
    public UserResDto findById(Long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            User user = userOptional.get();
            return this.userMapper.toDto(user);
        } else {
            throw new EntityException("User not found with ID " + id);
        }
    }

    /**
     * Supprime physiquement l'utilisateur (par opposition aux projets et aux
     * tâches qui sont archivés) : l'enregistrement disparaît définitivement de la
     * base.
     */
    @Override
    public UserResDto deleteById(Long id) {
        Optional<User> userOptional = this.userRepository.findById(id);
        if (userOptional.isPresent()) {
            User userToDelete = userOptional.get();
            this.userRepository.deleteById(id);
            return this.userMapper.toDto(userToDelete);
        } else {
            throw new EntityException("Unable to delete user: user not found with ID " + id);
        }
    }

    /**
     * Rattache le rôle {@code name} à l'utilisateur identifié par son email en
     * REMPLAÇANT l'ensemble des rôles existants (clear() puis add()) : dans la
     * pratique, un utilisateur ne porte donc qu'un seul rôle à la fois.
     */
    @Override
    public UserResDto attachRole(String email, RoleType name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        Optional<Role> optionalRole = roleRepository.findByName(name);

        if (optionalUser.isPresent() && optionalRole.isPresent()) {
            User user = optionalUser.get();
            Role role = optionalRole.get();
            user.getRoles().clear();
            user.addRole(role);
            return this.userMapper.toDto(userRepository.save(user));
        } else {
            throw new EntityException("User or Role not found");
        }
    }

    @Override
    public UserResDto detachRole(String email, RoleType name) {
        Optional<User> optionalUser = userRepository.findByEmail(email);
        Optional<Role> optionalRole = roleRepository.findByName(name);

        if (optionalUser.isPresent() && optionalRole.isPresent()) {
            User user = optionalUser.get();
            Role role = optionalRole.get();
            user.removeRole(role);
            return this.userMapper.toDto(userRepository.save(user));
        } else {
            throw new EntityException("User or Role not found");
        }
    }

    @Override
    public UserDetailsService userDetailsService() {
        return email ->
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new UsernameNotFoundException("User not found"));
    }

    @Override
    public User findByEmail(String email) {
        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException("Invalid email or password")
                );
    }

    @Override
    public List<UserResDto> findAllUserActive() {
        return userRepository.findByIsActiveTrue()
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public List<UserResDto> findAllUserDisable() {
        return userRepository.findByIsActiveFalse()
                .stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    @Override
    public UserResDto updatePassword(String email, String newPassword) {
        validatePasswordLength(newPassword);

        User user =
                userRepository
                        .findByEmail(normalizeEmail(email))
                        .orElseThrow(() -> new RuntimeException("User not found"));

        // La récupération de mot de passe est refusée pour un compte désactivé :
        // elle ne doit pas permettre de restaurer l'accès d'un utilisateur bloqué.
        if (Boolean.FALSE.equals(user.getIsActive())) {
            throw new BadRequestException("Ce compte est désactivé, la récupération du mot de passe n'est pas possible.");
        }

        user.setPwd(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        return this.userMapper.toDto(saved);
    }

    private void validatePasswordLength(String password) {
        if (password != null && !password.isBlank() && password.length() < 8) {
            throw new BadRequestException("Le mot de passe doit contenir au moins 8 caractères");
        }
    }

    private void validateNameContainsNoDigits(String name, String fieldLabel) {
        if (name != null && !name.isBlank() && name.chars().anyMatch(Character::isDigit)) {
            throw new BadRequestException(fieldLabel + " ne doit pas contenir de chiffres");
        }
    }

    private String normalizeEmail(String email) {
        return email == null ? null : email.trim().toLowerCase();
    }

    @Override
    public UserResDto getUserRestByEmail(String email) {
        Optional<User> users = this.userRepository.findByEmail(normalizeEmail(email));
        if (users.isPresent()) {
            var user = users.get();
            return this.userMapper.toDto(user);
        } else {
            throw new RuntimeException("User not found ");
        }
    }

    @Override
    public Long getAccountNoRole() {
        return this.userRepository.findAll().stream()
                .filter((user -> user.getRoles().isEmpty()))
                .count();
    }

    /**
     * Bascule le statut de présence en ligne ({@code status}) d'un utilisateur.
     * Appelé lors du login (true) et du logout (false) par l'AuthController.
     */
    @Override
    public void updateStatus(String email, Boolean status) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setStatus(status);
        userRepository.save(user);
    }

    @Override
    public void updateAccount(String email, Boolean isActive) {

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        user.setIsActive(isActive);

        userRepository.save(user);
    }

    @Override
    public List<UserResDto> findAllByRole(RoleType roleType) {
        return userRepository.findAll().stream()
                .filter(user -> user.getRoles().stream()
                        .anyMatch(role -> role.getName() == roleType))
                .map(userMapper::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Liste les utilisateurs actifs proposables pour un projet : il s'agit des
     * comptes actifs qui ne sont PAS encore assignés à une tâche du projet
     * (les assignations sont lues depuis la table {@code task_assignees}, et non
     * depuis la liste des contributeurs du projet).
     *
     * <p>Le comportement actuel ne vérifie pas l'appartenance de l'utilisateur au
     * projet ; il retourne simplement tous les comptes actifs non encore
     * assignés.
     */
    @Override
    public List<UserResDto> findPotentialContributors(Long projectId) {
        var existingAssigneeIds = taskRepository.findAssigneeIdsByProjectId(List.of(projectId));
        var allUsers = userRepository.findByIsActiveTrue().stream()
                .map(userMapper::toDto)
                .collect(Collectors.toList());
        
        return allUsers.stream()
                .filter(user -> existingAssigneeIds.stream()
                        .noneMatch(id -> id.equals(user.id())))
                .collect(Collectors.toList());
    }

}
