package org.ssglobal.revalida.ssclassroom.error;

import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.http.HttpServletRequest;
import org.ssglobal.revalida.ssclassroom.enums.ErrorCode;

@RestController
public class ApiErrorController implements ErrorController {

    @RequestMapping("/error")
    public ResponseEntity<ApiError> handleError(HttpServletRequest request) {
        Object status = request.getAttribute(RequestDispatcher.ERROR_STATUS_CODE);
        int statusCode = status instanceof Integer value ? value : 500;

        ErrorCode code = ErrorCode.fromStatus(statusCode);
        return ResponseEntity.status(code.getStatus()).body(ApiError.of(code, code.getDefaultMessage()));
    }
}
