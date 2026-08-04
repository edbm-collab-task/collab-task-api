package com.school.security.core.email;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.AddressException;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.env.Environment;
import org.springframework.core.io.FileSystemResource;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
public class EmailService implements ISendMail{

    public static final String EMAIL_ADDRESS_INVALID = "Email address invalid : ";
    public static final String ERROR_SENDING_EMAIL_TO = "Error sending email to ";
    private static final String MAIL_FROM = "mail.from";
    private final JavaMailSender emailSender;
    private final Environment env;
    private final TemplateEmailService templateEmailService;

    public EmailService(JavaMailSender emailSender, Environment env, TemplateEmailService templateEmailService) {
        this.emailSender = emailSender;
        this.env = env;
        this.templateEmailService = templateEmailService;
    }

    public static boolean isValidEmailAddress(String email) {
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
            return true;
        } catch (AddressException ex) {
            return false;
        }
    }

    public void sendRecoveryCodeEmail(
            String email,
            String username,
            int code
    ){

        String html =
                templateEmailService.generateRecoveryCodeEmail(
                        username,
                        code
                );


        sendHtmlEmail(
                email,
                "Code de récupération de votre compte Collab Task",
                html
        );

    }

    @Override
    public void senderSimpleEmail(String to, String subject, String emailContent) {
        if (isValidEmailAddress(to)){
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(env.getProperty(MAIL_FROM));
            message.setTo(to);
            message.setSubject(subject);
            message.setText(emailContent);
            emailSender.send(message);
        }else {
            throw new IllegalArgumentException(EMAIL_ADDRESS_INVALID + to);
        }
    }

    @Override
    public void sendHtmlEmail(String emailAddress, String subject, String htmlMsg) {
        try {
            if (isValidEmailAddress(emailAddress)) {
                MimeMessage message = emailSender.createMimeMessage ();
                MimeMessageHelper helper = new MimeMessageHelper (message, true, "utf-8");
                String from = env.getProperty(MAIL_FROM);
                if (from != null && !from.isEmpty()) {
                    helper.setFrom(from);
                }else {
                    throw new MessagingException("Insert the sender's email address");
                }
                helper.setTo (InternetAddress.parse (emailAddress));
                helper.setSubject (subject);
                helper.setText(htmlMsg, true);
                this.emailSender.send (message);
            }else {
                throw new IllegalArgumentException(EMAIL_ADDRESS_INVALID + emailAddress);
            }
        } catch (MessagingException | MailException e) {
            log.error("Erreur lors de l'envoi de l'email à {} : {}", emailAddress, e.getMessage(), e);
            throw new EmailSendingException(ERROR_SENDING_EMAIL_TO + emailAddress, e);
        }
    }

    @Override
    public void sendHtmlEmailToMultiple(List<String> emailAddresses, List<String> emailAddressCopy, String subject, String htmlMsg) {
        try {
            MimeMessage message = emailSender.createMimeMessage ();
            MimeMessageHelper helper = new MimeMessageHelper (message, true, "utf-8");
            createMimeMessage(helper, emailAddresses, subject, htmlMsg);
            if (emailAddressCopy!=null) {
                checkMailValid(emailAddressCopy);
                helper.setCc (emailAddressCopy.toArray(new String[0]));
            }

            this.emailSender.send (message);
        }catch (MessagingException | MailException e) {
            throw new EmailSendingException(ERROR_SENDING_EMAIL_TO + emailAddresses + emailAddressCopy, e);
        }
    }

    private void checkMailValid(List<String> emailAddresses) {
        List<String> invalidEmails = new ArrayList<>();
        for (String email : emailAddresses) {
            if (!isValidEmailAddress(email)) {
                invalidEmails.add(email);
            }
        }
        if (!invalidEmails.isEmpty()) {
            throw new IllegalArgumentException("e-mail address invalids : " + String.join(", ", invalidEmails));
        }
    }

    @Override
    public void sendEmailWithPathAttachment(List<String> emailAddresses, String subject, String emailContent, List<Attachement> attachmentPaths) {
        try {
            MimeMessage message = emailSender.createMimeMessage ();
            MimeMessageHelper helper = new MimeMessageHelper (message, true);
            createMimeMessage(helper, emailAddresses, subject, emailContent);
            for (Attachement attachment : attachmentPaths) {
                Path path = Paths.get(attachment.getFilePath());
                if (path.toFile().exists() && path.toFile().canRead()) {
                    FileSystemResource fileResource = new FileSystemResource(path.toFile());
                    helper.addAttachment(attachment.getFileName(), fileResource);
                } else {
                    throw new MessagingException("File not found or inaccessible : " + path);
                }
            }
            emailSender.send(message);
        }catch (MessagingException | MailException e) {
            throw new EmailSendingException(ERROR_SENDING_EMAIL_TO + emailAddresses, e);
        }
    }

    @Override
    public void sendEmailWithFileAttachment(List<String> emailAddresses, String subject, String emailContent, List<MultipartFile> files) throws IOException {
        try {
            MimeMessage message = emailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper (message, true);
            createMimeMessage(helper, emailAddresses, subject, emailContent);
            addAttachments(helper, files);

            emailSender.send(message);
        }catch (MessagingException | MailException e) {
            throw new EmailSendingException(ERROR_SENDING_EMAIL_TO + emailAddresses, e);
        }
    }

    private void createMimeMessage(MimeMessageHelper helper, List<String> emailAddresses, String subject, String emailContent) throws MessagingException {
        checkMailValid(emailAddresses);
        helper.setTo(emailAddresses.toArray(new String[0]));
        helper.setSubject (subject);
        helper.setText (emailContent, true);
        String from = env.getProperty(MAIL_FROM);
        if (from != null && !from.isEmpty()) {
            helper.setFrom(from);
        }else {
            throw new MessagingException("Insert the sender's email address");
        }
    }

    private void addAttachments(MimeMessageHelper helper, List<MultipartFile> files) throws MessagingException, IOException {

        for (MultipartFile multipartFile : files) {
            if (!multipartFile.isEmpty()) {
                File tempFile = File.createTempFile("attachment-", multipartFile.getOriginalFilename());
                multipartFile.transferTo(tempFile);

                FileSystemResource fileResource = new FileSystemResource(tempFile);
                helper.addAttachment(Objects.requireNonNull(multipartFile.getOriginalFilename()), fileResource);
            } else {
                throw new MessagingException("Empty file or inaccessible: " + multipartFile.getOriginalFilename());
            }
        }

    }



}