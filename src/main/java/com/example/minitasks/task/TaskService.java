package com.example.minitasks.task;

import com.example.minitasks.common.exceptions.ForbiddenException;
import com.example.minitasks.common.exceptions.NotFoundException;
import com.example.minitasks.task.dto.CreateTaskRequest;
import com.example.minitasks.task.dto.UpdateTaskRequest;
import com.example.minitasks.user.User;
import com.example.minitasks.user.UserService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@Transactional
public class TaskService {

    private final TaskRepository tasks;
    private final TagRepository tags;
    private final UserService userService;

    public TaskService(TaskRepository tasks, TagRepository tagRepo, UserService userService) {
        this.tasks = tasks;
        this.tags = tagRepo;
        this.userService = userService;
    }

    public Task create(UUID currentUserId, CreateTaskRequest req) {
        User owner = userService.getById(currentUserId);
        Task task = new Task(
                UUID.randomUUID(),
                owner,
                req.title(),
                req.description(),
                req.priorityOrDefault(),
                req.dueDate()
        );
        if (req.tags() != null) {
            for (String rawName : req.tags()) {
                task.addTag(findOrCreateTag(rawName));
            }
        }
        return tasks.save(task);
    }

    @Transactional(readOnly = true)
    public Task get(UUID taskId, UUID currentUserId, boolean isAdmin) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
        ensureCanAccess(task, currentUserId, isAdmin);
        return task;
    }

    @Transactional(readOnly = true)
    public Page<Task> search(UUID currentUserId, TaskStatus status, String tagName, Pageable pageable) {
        String normalizedTag = tagName == null ? null : Tag.normalize(tagName);
        return tasks.search(currentUserId, status, normalizedTag, pageable);
    }

    public Task update(UUID taskId, UUID currentUserId, boolean isAdmin, UpdateTaskRequest req) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
        ensureCanAccess(task, currentUserId, isAdmin);
        if (req.title() != null) task.rename(req.title());
        if (req.description() != null) task.describe(req.description());
        if (req.status() != null) task.changeStatus(req.status());
        if (req.priority() != null) task.setPriority(req.priority());
        if (req.dueDate() != null) task.setDueDate(req.dueDate());
        return task;
    }

    public void delete(UUID taskId, UUID currentUserId, boolean isAdmin) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
        ensureCanAccess(task, currentUserId, isAdmin);
        tasks.delete(task);
    }

    public Task addTag(UUID taskId, UUID currentUserId, boolean isAdmin, String rawName) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
        ensureCanAccess(task, currentUserId, isAdmin);
        task.addTag(findOrCreateTag(rawName));
        return task;
    }

    public void removeTag(UUID taskId, UUID currentUserId, boolean isAdmin, String rawName) {
        Task task = tasks.findById(taskId)
                .orElseThrow(() -> new NotFoundException("TASK_NOT_FOUND", "Task not found"));
        ensureCanAccess(task, currentUserId, isAdmin);
        String normalized = Tag.normalize(rawName);
        if (!task.removeTagByName(normalized)) {
            throw new NotFoundException("TAG_NOT_ON_TASK", "Tag is not attached to this task");
        }
    }

    private Tag findOrCreateTag(String rawName) {
        String normalized = Tag.normalize(rawName);
        return tags.findByName(normalized).orElseGet(() -> tags.save(Tag.of(normalized)));
    }

    private void ensureCanAccess(Task task, UUID currentUserId, boolean isAdmin) {
        if (!isAdmin && !task.isOwnedBy(currentUserId)) {
            throw new ForbiddenException("TASK_FORBIDDEN", "You cannot access this task");
        }
    }
}
