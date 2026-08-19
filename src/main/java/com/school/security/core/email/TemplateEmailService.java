package com.school.security.core.email;

import org.thymeleaf.context.Context;
import org.thymeleaf.TemplateEngine;
import org.springframework.stereotype.Service;

@Service
public class TemplateEmailService {


    private final TemplateEngine templateEngine;


    public TemplateEmailService(TemplateEngine templateEngine) {
        this.templateEngine = templateEngine;
    }


    public String generateRecoveryCodeEmail(
            String username,
            int code
    ){

        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("code", code);


        return templateEngine.process(
                "code",
                context
        );
    }

    public String createPwd(
            String username,
            String code
    ){

        Context context = new Context();

        context.setVariable("username", username);
        context.setVariable("code", code);


        return templateEngine.process(
                "pwd",
                context
        );
    }

    public String generateContributorAddedEmail(String userName, String projectTitle, String ownerName) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("projectTitle", projectTitle);
        context.setVariable("ownerName", ownerName);
        return templateEngine.process("contributor_added", context);
    }

    public String generateTaskAssignedEmail(String userName, String taskTitle, String projectTitle, String message, String priorityName, String priorityColor) {
        Context context = new Context();
        context.setVariable("userName", userName);
        context.setVariable("taskTitle", taskTitle);
        context.setVariable("projectTitle", projectTitle);
        context.setVariable("message", message);
        context.setVariable("priorityName", priorityName);
        context.setVariable("priorityColor", priorityColor);
        context.setVariable("title", message.contains("urgente") ? "Tâche urgente assignée" : "Tâche assignée");
        return templateEngine.process("task_assigned", context);
    }

}