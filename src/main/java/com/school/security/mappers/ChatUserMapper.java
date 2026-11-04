package com.school.security.mappers;

import com.school.security.dtos.responses.ChatUserResponse;
import com.school.security.entities.User;
import org.springframework.stereotype.Component;

@Component
public class ChatUserMapper {

    public ChatUserResponse toResponse(User user) {

        if (user == null) {
            return null;
        }

        boolean online = Boolean.TRUE.equals(user.getStatus());
        String email = user.getEmail();
        if (email != null && (email.equalsIgnoreCase("admin@edbm.com") || email.equalsIgnoreCase("superadmin@edbm.com"))) {
            online = false;
        }

        return new ChatUserResponse(
                user.getUsersId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getImagePath(),
                online
        );
    }
}