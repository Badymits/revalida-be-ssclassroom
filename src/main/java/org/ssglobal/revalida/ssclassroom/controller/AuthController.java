package org.ssglobal.revalida.ssclassroom.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.ssglobal.revalida.ssclassroom.dto.Auth.CohortOptionDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.LoginRequestDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.LoginResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.MeResponseDto;
import org.ssglobal.revalida.ssclassroom.dto.Auth.RegisterRequestDto;
import org.ssglobal.revalida.ssclassroom.service.AuthService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/** Identity endpoints. /auth/** is public (see MySecurityContext); /me needs a token. */
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/auth/login")
    public ResponseEntity<LoginResponseDto> login(@Valid @RequestBody LoginRequestDto request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/auth/register")
    public ResponseEntity<MeResponseDto> register(@Valid @RequestBody RegisterRequestDto request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.register(request));
    }

    @GetMapping("/auth/cohorts")
    public ResponseEntity<List<CohortOptionDto>> cohorts() {
        return ResponseEntity.ok(authService.listCohorts());
    }

    @GetMapping("/me")
    public ResponseEntity<MeResponseDto> me(Principal principal) {
        return ResponseEntity.ok(authService.me(principal.getName()));
    }
}
