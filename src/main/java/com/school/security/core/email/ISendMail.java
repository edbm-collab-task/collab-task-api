package com.school.security.core.email;

import jakarta.mail.MessagingException;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface ISendMail {

    void senderSimpleEmail(String to, String subject, String emailContent);

    void sendHtmlEmail(String emailAddress, String subject, String htmlMsg) throws MessagingException;

    void sendHtmlEmailToMultiple(List<String> emailAddress, List<String> emailAddressCopy, String subject, String htmlMsg) throws MessagingException;

    void sendEmailWithPathAttachment(List<String> emailAddresses, String subject, String emailContent, List<Attachement> attachmentPaths) throws MessagingException;

    void sendEmailWithFileAttachment(List<String> emailAddress, String subject, String content, List<MultipartFile> files) throws IOException;
}