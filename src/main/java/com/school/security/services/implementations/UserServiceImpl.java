package com.school.security.services.implementations;

import com.school.security.core.email.EmailService;
import com.school.security.dtos.requests.UserReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.entities.Role;
import com.school.security.entities.User;
import com.school.security.enums.RoleType;
import com.school.security.exceptions.EntityException;
import com.school.security.mappers.UserMapper;
import com.school.security.repositories.*;
import com.school.security.services.contracts.UserService;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import static com.school.security.controllers.auth.AuthController.generateRandomString;

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

    @Override
    public UserResDto createOrUpdate(UserReqDto toSave) {
        Optional<User> userOptional = userRepository.findByEmail(toSave.email());

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
            attachRole(toSave.email(),RoleType.USER);
            return userMapper.toDto(userToSave);
        }
    }



    @Override
    public UserResDto create(UserReqDto toSave) {
        Optional<User> userOptional = userRepository.findByEmail(toSave.email());

        if(userOptional.isEmpty()){

            String code = generateRandomString(12);

            User user = new User();
            user.setEmail(toSave.email());
            user.setPwd(code);
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
            userRepository.save(toUpdate);

            return userMapper.toDto(toUpdate);
        }
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
        User user =
                userRepository
                        .findByEmail(email)
                        .orElseThrow(() -> new RuntimeException("User not found"));
        user.setPwd(passwordEncoder.encode(newPassword));
        User saved = userRepository.save(user);
        return this.userMapper.toDto(saved);
    }

    @Override
    public UserResDto getUserRestByEmail(String email) {
        Optional<User> users = this.userRepository.findByEmail(email);
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

}
