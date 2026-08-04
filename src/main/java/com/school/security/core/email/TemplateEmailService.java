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

}