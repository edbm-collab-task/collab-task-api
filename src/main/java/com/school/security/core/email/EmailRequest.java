package com.school.security.core.email;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@Data
public class EmailRequest {

    @NotEmpty
    private List<@Email @NotBlank String> emailAddress;

    @NotBlank
    private String subject;

    @NotBlank
    private String content;

    private List<MultipartFile> files;

}
