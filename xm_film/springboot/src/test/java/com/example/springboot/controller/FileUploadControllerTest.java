package com.example.springboot.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * 上传门禁：非管理员只能传图片，且单张不超过 2MB。
 * 用 standaloneSetup 直接测控制器分支，不启动完整 Web 上下文。
 */
class FileUploadControllerTest {

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        FileUploadController controller = new FileUploadController();
        ReflectionTestUtils.setField(controller, "uploadDir", "target/test-uploads");
        ReflectionTestUtils.setField(controller, "accessPrefix", "http://localhost/files/");
        mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
    }

    @Test
    void userCannotUploadVideo() throws Exception {
        MockMultipartFile file = new MockMultipartFile("file", "clip.mp4", "video/mp4", new byte[]{1});

        mockMvc.perform(multipart("/api/v1/files/upload").file(file).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value("仅支持上传图片"));
    }

    @Test
    void userCannotUploadImageOverTwoMegabytes() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "big.jpg", "image/jpeg", new byte[3 * 1024 * 1024]);

        mockMvc.perform(multipart("/api/v1/files/upload").file(file).requestAttr("role", "USER"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value("400"))
                .andExpect(jsonPath("$.msg").value("图片不能超过 2MB"));
    }
}
