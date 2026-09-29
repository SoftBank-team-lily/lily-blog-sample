package com.lily.blog;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("local")
class PostApiTest {

    @Autowired
    MockMvc mvc;

    @Test
    void 글_목록_조회() throws Exception {
        mvc.perform(get("/api/posts"))
                .andExpect(status().isOk());
    }

    @Test
    void 글_생성_후_조회() throws Exception {
        String body = """
                {"title":"테스트","content":"내용","author":"tester"}
                """;

        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title").value("테스트"));
    }

    @Test
    void 없는_글_조회시_404() throws Exception {
        mvc.perform(get("/api/posts/99999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("NOT_FOUND"));
    }

    @Test
    void 제목_없으면_400() throws Exception {
        mvc.perform(post("/api/posts")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"\",\"content\":\"내용\",\"author\":\"tester\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void 버전_엔드포인트() throws Exception {
        mvc.perform(get("/version"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.color").exists());
    }
}
