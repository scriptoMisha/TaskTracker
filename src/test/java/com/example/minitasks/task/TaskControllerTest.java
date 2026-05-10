package com.example.minitasks.task;

import com.example.minitasks.auth.JwtService;
import com.example.minitasks.common.GlobalExceptionHandler;
import com.example.minitasks.common.exceptions.ForbiddenException;
import com.example.minitasks.config.SecurityConfig;
import com.example.minitasks.task.dto.CreateTaskRequest;
import com.example.minitasks.user.Role;
import com.example.minitasks.user.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = TaskController.class)
@Import({GlobalExceptionHandler.class, SecurityConfig.class})
class TaskControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @MockitoBean
    TaskService taskService;

    @MockitoBean
    JwtService jwtService;

    private UUID userId;
    private Authentication userAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        userAuth = new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_USER")));
    }

    private Task fakeTaskOwnedBy(UUID ownerId) {
        User owner = new User(ownerId, "o@x.com", "h", Role.USER);
        return new Task(UUID.randomUUID(), owner, "T", "d", 3, null);
    }

    @Test
    void create_returns201_andBody() throws Exception {
        Task t = fakeTaskOwnedBy(userId);
        when(taskService.create(eq(userId), any(CreateTaskRequest.class))).thenReturn(t);

        mvc.perform(post("/api/tasks")
                        .with(authentication(userAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateTaskRequest("Buy milk", null, 2, null, Set.of()))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("T"))
                .andExpect(jsonPath("$.ownerId").value(userId.toString()));
    }

    @Test
    void create_invalidPriority_returns400() throws Exception {
        mvc.perform(post("/api/tasks")
                        .with(authentication(userAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateTaskRequest("Buy", null, 99, null, Set.of()))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"));
    }

    @Test
    void getOthersTask_returns403() throws Exception {
        UUID taskId = UUID.randomUUID();
        when(taskService.get(eq(taskId), eq(userId), anyBoolean()))
                .thenThrow(new ForbiddenException("TASK_FORBIDDEN", "nope"));

        mvc.perform(get("/api/tasks/{id}", taskId)
                        .with(authentication(userAuth)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value("TASK_FORBIDDEN"));
    }

    @Test
    void create_blankTitle_returns400() throws Exception {
        mvc.perform(post("/api/tasks")
                        .with(authentication(userAuth))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(
                                new CreateTaskRequest("", null, 3, null, Set.of()))))
                .andExpect(status().isBadRequest());
    }
}
