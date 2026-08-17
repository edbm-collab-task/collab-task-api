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

        return new ChatUserResponse(
                user.getUsersId(),
                user.getFirstname(),
                user.getLastname(),
                user.getEmail(),
                user.getImagePath(),
                Boolean.TRUE.equals(user.getStatus())
        );
    }
}