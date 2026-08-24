package com.school.security.controllers.api;


import com.school.security.dtos.requests.AttachRoleReqDto;
import com.school.security.dtos.requests.PwdReqDto;
import com.school.security.dtos.responses.UserResDto;
import com.school.security.services.contracts.UserService;

import java.io.IOException;
import java.nio.file.Files;
import java.util.List;
import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/users")

public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping
    @PreAuthorize("@permissionEvaluator.hasPermission('VIEW_USERS')")
    public List<UserResDto> getAllUsers() {
        return this.userService.findAll();
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public UserResDto deleteById(@PathVariable Long id) {
        return this.userService.deleteById(id);
    }

    @GetMapping("/{id}")
    public UserResDto getUser(@PathVariable Long id) {
        return this.userService.findById(id);
    }

    @GetMapping("/email")
    public UserResDto getUserByEmail(@RequestParam String email) {
        return this.userService.getUserRestByEmail(email);
    }

    @PutMapping("/pwd")
    public UserResDto updatePassword(@RequestBody PwdReqDto pwdReqDto) {
        return userService.updatePassword(pwdReqDto.email(), pwdReqDto.password());
    }

    @PutMapping("/role")
    @PreAuthorize("@permissionEvaluator.hasPermission('MANAGE_USERS')")
    public ResponseEntity<UserResDto> updateRole(@RequestBody AttachRoleReqDto attachRoleRegDto) {
        UserResDto userResDto =
                userService.attachRole(attachRoleRegDto.email(), attachRoleRegDto.role());
        return ResponseEntity.ok(userResDto);
    }

    @DeleteMapping("/role")
    public ResponseEntity<UserResDto> deleteRole(@RequestBody AttachRoleReqDto attachRoleRegDto) {
        UserResDto userResDto =
                userService.detachRole(attachRoleRegDto.email(), attachRoleRegDto.role());
        return ResponseEntity.ok(userResDto);
    }

    // Pour activer et désactiver une compte

    @PutMapping("/account")
    public ResponseEntity<?> updateAccountStatus(
            @RequestParam String email,
            @RequestParam Boolean isActive
    ) {

        userService.updateAccount(email, isActive);

        return ResponseEntity.ok(
                Map.of("message", "Account  updated successfully")
        );
    }

    @GetMapping("/active")
    public ResponseEntity<List<UserResDto>> findAllUserActive() {
        return ResponseEntity.ok(userService.findAllUserActive());
    }

    @GetMapping("/disable")
    public ResponseEntity<List<UserResDto>> findAllUsersDisable() {
        return ResponseEntity.ok(userService.findAllUserDisable());
    }

    @GetMapping("/admins")
    public ResponseEntity<List<UserResDto>> findAllAdmins() {
        return ResponseEntity.ok(userService.findAllByRole(com.school.security.enums.RoleType.ADMIN));
    }

    @PostMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<UserResDto> addImageToUser(
            @PathVariable Long id,
            @RequestParam("image") MultipartFile image
    )
    {
        return ResponseEntity.ok(
                userService.addImageToUser(id, image)
        );
    }

    @GetMapping("/{id}/image")
    public ResponseEntity<Resource> getUserImage(
            @PathVariable Long id
    ) throws IOException {

        Resource image = userService.getUserImage(id);

        String contentType = Files.probeContentType(
                image.getFile().toPath()
        );

        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(
                        MediaType.parseMediaType(contentType)
                )
                .body(image);
    }

}
