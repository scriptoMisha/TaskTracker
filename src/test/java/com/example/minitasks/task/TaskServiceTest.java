package com.example.minitasks.task;

import com.example.minitasks.common.exceptions.ForbiddenException;
import com.example.minitasks.common.exceptions.NotFoundException;
import com.example.minitasks.task.dto.CreateTaskRequest;
import com.example.minitasks.task.dto.UpdateTaskRequest;
import com.example.minitasks.user.Role;
import com.example.minitasks.user.User;
import com.example.minitasks.user.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    TaskRepository tasks;
    @Mock
    TagRepository tagRepo;
    @Mock
    UserService userService;

    @InjectMocks
    TaskService service;

    private User owner;
    private User intruder;

    @BeforeEach
    void setUp() {
        owner = new User(UUID.randomUUID(), "owner@example.com", "h", Role.USER);
        intruder = new User(UUID.randomUUID(), "x@example.com", "h", Role.USER);
    }

    private Task taskOf(User u) {
        return new Task(UUID.randomUUID(), u, "Title", "desc", 3, null);
    }

    @Test
    void create_persists_andReturnsTask() {
        when(userService.getById(owner.getId())).thenReturn(owner);
        when(tasks.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

        Task t = service.create(owner.getId(),
                new CreateTaskRequest("Buy milk", null, 2, null, Set.of()));

        assertThat(t.getTitle()).isEqualTo("Buy milk");
        assertThat(t.getStatus()).isEqualTo(TaskStatus.TODO);
        assertThat(t.getPriority()).isEqualTo(2);
        verify(tasks).save(any(Task.class));
    }

    @Test
    void get_byOwner_returnsTask() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        assertThat(service.get(t.getId(), owner.getId(), false)).isSameAs(t);
    }

    @Test
    void get_byOther_throwsForbidden() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.get(t.getId(), intruder.getId(), false))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void get_byAdmin_isAllowed() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        assertThat(service.get(t.getId(), intruder.getId(), true)).isSameAs(t);
    }

    @Test
    void get_notFound_throws() {
        UUID id = UUID.randomUUID();
        when(tasks.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.get(id, owner.getId(), false))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void update_appliesNonNullFieldsOnly() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        Task updated = service.update(t.getId(), owner.getId(), false,
                new UpdateTaskRequest(null, null, TaskStatus.IN_PROGRESS, null, null));

        assertThat(updated.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
        assertThat(updated.getTitle()).isEqualTo("Title"); // unchanged
        assertThat(updated.getPriority()).isEqualTo(3);
    }

    @Test
    void delete_byNonOwner_throws_andDoesNotDelete() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.delete(t.getId(), intruder.getId(), false))
                .isInstanceOf(ForbiddenException.class);
        verify(tasks, never()).delete(any());
    }

    @Test
    void addTag_findsExistingOrCreates() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));
        lenient().when(tagRepo.findByName("urgent")).thenReturn(Optional.empty());
        when(tagRepo.save(any(Tag.class))).thenAnswer(inv -> inv.getArgument(0));

        Task updated = service.addTag(t.getId(), owner.getId(), false, "Urgent");

        assertThat(updated.getTags()).extracting(Tag::getName).contains("urgent");
    }

    @Test
    void removeTag_notAttached_throws() {
        Task t = taskOf(owner);
        when(tasks.findById(t.getId())).thenReturn(Optional.of(t));

        assertThatThrownBy(() -> service.removeTag(t.getId(), owner.getId(), false, "nope"))
                .isInstanceOf(NotFoundException.class);
    }
}
