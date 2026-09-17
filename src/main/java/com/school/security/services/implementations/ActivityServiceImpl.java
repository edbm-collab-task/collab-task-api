package com.school.security.services.implementations;

import com.school.security.dtos.responses.ActivityResDto;
import com.school.security.entities.Activity;
import com.school.security.entities.Project;
import com.school.security.entities.User;
import com.school.security.enums.ActivityType;
import com.school.security.repositories.ActivityRepository;
import com.school.security.repositories.ProjectRepository;
import com.school.security.repositories.UserRepository;
import com.school.security.services.contracts.ActivityService;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Service de suivi des activités liées aux projets et aux tâches.
 *
 * <p>Responsabilité constatée (documentée, non modifiée) :
 * <ul>
 *   <li>{@code logActivity} enregistre une nouvelle activité ; si le projet
 *       ou l'utilisateur est introuvable, l'opération est silencieusement
 *   ignorée (aucun {@code Exception} levé).</li>
 *   <li>{@code findByProjectId} retourne les activités d'un projet triées
 *       par date de création décroissante ; cette donnée alimente les
 *   tableaux de bord et les rapports d'activité.</li>
 *   <li>les activités lient un projet, un utilisateur, un type ({@link
 *   ActivityType}), une description optionnelle et une référence de tâche.</li>
 * </ul>
 */
@Service
@Transactional
@AllArgsConstructor
public class ActivityServiceImpl implements ActivityService {

    private ActivityRepository activityRepository;
    private ProjectRepository projectRepository;
    private UserRepository userRepository;

    /**
     * Retourne les activités d'un projet triées par date de création décroissante.
     *
     * <p>Ces données alimentent les tableaux de bord et les rapports d'activité.
     * Le résultat inclut le projet, l'utilisateur, le type d'activité, la description
     * et la référence de tâche associée.</p>
     */
    @Override
    public List<ActivityResDto> findByProjectId(Long projectId) {
        return activityRepository.findByProjectProjectIdOrderByCreatedAtDesc(projectId)
                .stream()
                .map(this::toDto)
                .collect(Collectors.toList());
    }

    /**
     * Enregistre une nouvelle activité liée à un projet et un utilisateur.
     *
     * <p>Comportement constaté : si le projet ou l'utilisateur est introuvable,
     * l'opération est silencieusement ignorée (pas d'exception levée, pas de
     * création d'activité).</p>
     */
    @Override
    public void logActivity(Long projectId, Long userId, ActivityType type, String description, Long taskId) {
        Project project = projectRepository.findById(projectId).orElse(null);
        User user = userRepository.findById(userId).orElse(null);
        if (project == null || user == null) return;

        Activity activity = new Activity();
        activity.setProject(project);
        activity.setUser(user);
        activity.setType(type);
        activity.setDescription(description);
        activity.setTaskId(taskId);
        activityRepository.save(activity);
    }

    private ActivityResDto toDto(Activity entity) {
        return new ActivityResDto(
                entity.getActivityId(),
                entity.getProject().getProjectId(),
                entity.getProject().getTitle(),
                entity.getUser().getUsersId(),
                entity.getUser().getFirstname() + " " + entity.getUser().getLastname(),
                entity.getType(),
                entity.getDescription(),
                entity.getTaskId(),
                entity.getCreatedAt()
        );
    }
}
