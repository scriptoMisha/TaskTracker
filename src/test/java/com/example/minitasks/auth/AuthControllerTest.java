package com.example.minitasks.auth;

import com.example.minitasks.auth.dto.LoginRequest;
import com.example.minitasks.auth.dto.RegisterRequest;
import com.example.minitasks.auth.dto.TokenResponse;
import com.example.minitasks.common.GlobalExceptionHandler;
import com.example.minitasks.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(
        controllers = AuthController.class,
        excludeFilters = @ComponentScan.Filter(
                type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, JwtAuthFilter.class}))
@AutoConfigureMockMvc(addFilters = false)
@Import(GlobalExceptionHandler.class)
class AuthControllerTest {

    @Autowired
    MockMvc mvc;
    @Autowired
    ObjectMapper json;

    @MockitoBean
    AuthService authService;

    @Test
    void register_success_returns201_andTokens() throws Exception {
        when(authService.register(any(RegisterRequest.class)))
                .thenReturn(TokenResponse.bearer("acc", "ref"));

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest("a@b.com", "Password1"))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").value("acc"))
                .andExpect(jsonPath("$.refreshToken").value("ref"))
                .andExpect(jsonPath("$.tokenType").value("Bearer"));
    }

    @Test
    void register_blankEmail_returns400_withErrors() throws Exception {
        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new RegisterRequest("", "Password1"))))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_FAILED"))
                .andExpect(jsonPath("$.errors").isArray());
    }

    @Test
    void login_success_returns200() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenReturn(TokenResponse.bearer("acc", "ref"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@b.com", "Password1"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").value("acc"));
    }

    @Test
    void login_wrongPassword_returns401() throws Exception {
        when(authService.login(any(LoginRequest.class)))
                .thenThrow(new BadCredentialsException("bad"));

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json.writeValueAsString(new LoginRequest("a@b.com", "Password1"))))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.code").value("BAD_CREDENTIALS"));
    }
}
