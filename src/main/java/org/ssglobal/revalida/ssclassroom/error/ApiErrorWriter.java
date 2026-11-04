package org.ssglobal.revalida.ssclassroom.error;

import java.io.IOException;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;
import tools.jackson.databind.json.JsonMapper;

@Component
@RequiredArgsConstructor
public class ApiErrorWriter {

    private final JsonMapper jsonMapper;

    public void write(HttpServletResponse response, ErrorCode code) throws IOException {
        response.setStatus(code.getStatus());
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        jsonMapper.writeValue(response.getOutputStream(), ApiError.of(code, code.getDefaultMessage()));
    }
}
