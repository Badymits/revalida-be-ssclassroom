package org.ssglobal.revalida.ssclassroom.api.test;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;
import org.ssglobal.revalida.ssclassroom.controller.ContentController;

@WebMvcTest(ContentController.class)
@WithMockUser
class ContentControllerTest {

    @Autowired MockMvc mvc;

    @Test
    void createAssignment_validJson_returns501() throws Exception {
        String json = """
            {
              "title": "Lab 1 — Build a REST endpoint",
              "instructions": "Implement GET /health ...",
              "submissionType": "file",
              "points": 100
            }
            """;

        mvc.perform(post("/api/v1/cohorts/c-123/assignments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(json))
           .andExpect(status().isNotImplemented());
    }
}