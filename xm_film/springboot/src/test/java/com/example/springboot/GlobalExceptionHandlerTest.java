package com.example.springboot;

import com.example.springboot.controller.AuthController;
import com.example.springboot.common.JwtUtils;
import com.example.springboot.service.AdminService;
import com.example.springboot.service.CinemaService;
import com.example.springboot.service.UserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import com.example.springboot.exception.GlobalExceptionHandler;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AuthController.class)
@Import(GlobalExceptionHandler.class)
class GlobalExceptionHandlerTest {

    @Autowired
    MockMvc mockMvc;

    @Autowired
    ObjectMapper objectMapper;

    @MockBean
    AdminService adminService;

    @MockBean
    UserService userService;

    @MockBean
    CinemaService cinemaService;

    @MockBean
    JwtUtils jwtUtils;

    @Test
    void loginRejectsBlankUsernameWithParamInvalidCode() throws Exception {
        String body = """
                {"username":"","password":"123456","role":"USER"}
                """;

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").isNotEmpty());
    }
}
