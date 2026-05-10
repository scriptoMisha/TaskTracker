package com.example.minitasks.task.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.Set;

public record CreateTaskRequest(
        @NotBlank @Size(max = 200) String title,
        @Size(max = 5000) String description,
        @Min(1) @Max(5) Integer priority,
        LocalDate dueDate,
        Set<@NotBlank @Size(max = 50) String> tags
) {
    public int priorityOrDefault() {
        return priority == null ? 3 : priority;
    }
}
