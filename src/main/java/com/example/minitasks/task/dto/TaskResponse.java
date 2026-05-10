package com.example.minitasks.task.dto;

import com.example.minitasks.task.Tag;
import com.example.minitasks.task.Task;
import com.example.minitasks.task.TaskStatus;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

public record TaskResponse(
        UUID id,
        UUID ownerId,
        String title,
        String description,
        TaskStatus status,
        int priority,
        LocalDate dueDate,
        Set<String> tags,
        OffsetDateTime createdAt,
        OffsetDateTime updatedAt
) {
    public static TaskResponse from(Task t) {
        return new TaskResponse(
                t.getId(),
                t.getOwner().getId(),
                t.getTitle(),
                t.getDescription(),
                t.getStatus(),
                t.getPriority(),
                t.getDueDate(),
                t.getTags().stream().map(Tag::getName).collect(Collectors.toUnmodifiableSet()),
                t.getCreatedAt(),
                t.getUpdatedAt()
        );
    }
}
