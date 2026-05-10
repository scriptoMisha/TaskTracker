package com.example.minitasks.task.dto;

import com.example.minitasks.task.TaskStatus;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

public record UpdateTaskRequest(
        @Size(min = 1, max = 200) String title,
        @Size(max = 5000) String description,
        TaskStatus status,
        @Min(1) @Max(5) Integer priority,
        LocalDate dueDate
) {
}
