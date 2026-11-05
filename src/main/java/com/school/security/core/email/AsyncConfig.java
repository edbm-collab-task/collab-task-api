package com.school.security.core.email;

import lombok.extern.slf4j.Slf4j;
import org.springframework.aop.interceptor.AsyncUncaughtExceptionHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.AsyncConfigurer;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Arrays;
import java.util.concurrent.Executor;

/**
 * Configuration de l'exécution asynchrone des envois d'email.
 *
 * <p>L'envoi SMTP est bloquant (handshake TLS + allers-retours réseau). Exécuté
 * sur le thread de la requête HTTP, il ajoute plusieurs secondes au temps de
 * réponse des endpoints qui déclenchent un email — typiquement l'ajout d'un
 * contributeur. {@link EmailService#sendHtmlEmail} est annoté
 * {@code @Async("mailExecutor")} pour découpler l'envoi de la réponse HTTP.
 *
 * <p>Le pool est volontairement petit et sa file d'attente bornée : la
 * notification d'un email reste best-effort, on ne veut pas qu'un SMTP lent
 * sature la mémoire en accumulant des tâches. Une file pleine fait
 * {@code TaskRejectedException} vers
 * {@link #mailExceptionHandler()}, qui journalise et abandonne.
 *
 * <p>À noter : les appels internes à {@code sendHtmlEmail} (envoi d'un code de
 * récupération ou d'un mot de passe) restent synchrones, l'auto-invocation
 * contournant le proxy AOP. C'est le comportement souhaité, l'utilisateur
 * attend son code avant de poursuivre.
 */
@Slf4j
@Configuration
public class AsyncConfig implements AsyncConfigurer {

    /** Nom du bean executor référencé par {@code @Async("mailExecutor")}. */
    public static final String MAIL_EXECUTOR = "mailExecutor";

    @Override
    public Executor getAsyncExecutor() {
        return mailExecutor();
    }

    @Override
    public AsyncUncaughtExceptionHandler getAsyncUncaughtExceptionHandler() {
        return mailExceptionHandler();
    }

    /**
     * Pool dédié aux envois d'email, séparé de l'executor par défaut afin qu'une
     * rafale d'emails ne bloque pas les autres traitements asynchrones
     * (notifications de compte, tâches planifiées).
     */
    @Bean(name = MAIL_EXECUTOR)
    public ThreadPoolTaskExecutor mailExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(2);
        executor.setMaxPoolSize(4);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("mail-");
        // L'arrêt de l'application ne doit pas attendre l'envoi des emails en
        // cours : on interrompt, l'email non parti est perdu (best-effort).
        executor.setWaitForTasksToCompleteOnShutdown(false);
        executor.initialize();
        return executor;
    }

    /**
     * Les exceptions levées dans un {@code @Async} renvoyant {@code void} ne
     * sont jamais remontées à l'appelant : sans ce handler elles seraient
     * silencieuses.
     */
    @Bean
    public AsyncUncaughtExceptionHandler mailExceptionHandler() {
        return (ex, method, params) ->
                log.error("Echec asynchrone de {}({}) : {}", method.getName(), Arrays.toString(params),
                        ex.getMessage(), ex);
    }
}
