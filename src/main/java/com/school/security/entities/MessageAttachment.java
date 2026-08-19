package com.school.security.entities;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;

@Entity
@Table(name = "message_attachments")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MessageAttachment implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "attachment_id")
    private Long attachmentId;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(
            name = "message_id",
            nullable = false
    )
    private Message message;

    @Column(
            nullable = false,
            length = 255
    )
    private String name;

    @Column(
            nullable = false,
            length = 100
    )
    private String type;

    @Column(nullable = false)
    private Long size;

    @Column(
            nullable = false,
            length = 500
    )
    private String url;
}