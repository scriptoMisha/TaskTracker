package com.example.minitasks.task;

import com.example.minitasks.task.dto.CreateTaskRequest;
import com.example.minitasks.task.dto.TaskResponse;
import com.example.minitasks.task.dto.UpdateTaskRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/tasks")
public class TaskController {

    private final TaskService taskService;

    public TaskController(TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping
    public Page<TaskResponse> list(Authentication auth,
                                   @RequestParam(required = false) TaskStatus status,
                                   @RequestParam(required = false) String tag,
                                   Pageable pageable) {
        UUID userId = currentUserId(auth);
        return taskService.search(userId, status, tag, pageable).map(TaskResponse::from);
    }

    @PostMapping
    public ResponseEntity<TaskResponse> create(Authentication auth,
                                               @Valid @RequestBody CreateTaskRequest req) {
        UUID userId = currentUserId(auth);
        Task created = taskService.create(userId, req);
        return ResponseEntity.status(HttpStatus.CREATED).body(TaskResponse.from(created));
    }

    @GetMapping("/{id}")
    public TaskResponse get(Authentication auth, @PathVariable UUID id) {
        return TaskResponse.from(taskService.get(id, currentUserId(auth), isAdmin(auth)));
    }

    @PatchMapping("/{id}")
    public TaskResponse update(Authentication auth,
                               @PathVariable UUID id,
                               @Valid @RequestBody UpdateTaskRequest req) {
        return TaskResponse.from(taskService.update(id, currentUserId(auth), isAdmin(auth), req));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(Authentication auth, @PathVariable UUID id) {
        taskService.delete(id, currentUserId(auth), isAdmin(auth));
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/tags")
    public TaskResponse addTag(Authentication auth,
                               @PathVariable UUID id,
                               @Valid @RequestBody TagBody body) {
        Task t = taskService.addTag(id, currentUserId(auth), isAdmin(auth), body.name());
        return TaskResponse.from(t);
    }

    @DeleteMapping("/{id}/tags/{tagName}")
    public ResponseEntity<Void> removeTag(Authentication auth,
                                          @PathVariable UUID id,
                                          @PathVariable String tagName) {
        taskService.removeTag(id, currentUserId(auth), isAdmin(auth), tagName);
        return ResponseEntity.noContent().build();
    }

    private static UUID currentUserId(Authentication auth) {
        return (UUID) auth.getPrincipal();
    }

    private static boolean isAdmin(Authentication auth) {
        for (GrantedAuthority a : auth.getAuthorities()) {
            if ("ROLE_ADMIN".equals(a.getAuthority())) return true;
        }
        return false;
    }

    public record TagBody(@NotBlank @Size(max = 50) String name) {
    }
}
