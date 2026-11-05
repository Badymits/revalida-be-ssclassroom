package org.ssglobal.revalida.ssclassroom.api.test;

import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
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
           .andDo(print())
           .andExpect(status().isNotImplemented());
    }
    
    @Test
    void createAssignment_badEnum_returns400() throws Exception {
        mvc.perform(post("/api/v1/cohorts/c-123/assignments")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"x\",\"submissionType\":\"bogus\"}"))
           .andExpect(status().isBadRequest());
    }
    
    @Test
    void getAssignment_returns501() throws Exception {
        mvc.perform(get("/api/v1/assignments/a-1"))
           .andExpect(status().isNotImplemented());
    }

    @Test
    void updateAssignment_returns501() throws Exception {
        mvc.perform(patch("/api/v1/assignments/a-1")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"title\":\"Updated\"}"))
           .andExpect(status().isNotImplemented());
    }

    @Test
    void publishAssignment_returns501() throws Exception {
        mvc.perform(post("/api/v1/assignments/a-1/publish")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{}"))
           .andExpect(status().isNotImplemented());
    }
}