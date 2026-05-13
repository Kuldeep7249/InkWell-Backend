package com.inkwell.media.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileAccessSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() throws Exception {
        Path uploadDir = Paths.get("target/test-uploads");
        Files.createDirectories(uploadDir);
        Files.writeString(uploadDir.resolve("public-image.txt"), "test-image");
    }

    @Test
    void filesEndpointIsPublic() throws Exception {
        mockMvc.perform(get("/files/public-image.txt"))
                .andExpect(status().isOk());
    }

    @Test
    void uploadEndpointStillRequiresAuthentication() throws Exception {
        mockMvc.perform(multipart("/api/media/upload")
                        .file("file", "test-image".getBytes()))
                .andExpect(status().isUnauthorized());
    }
}
