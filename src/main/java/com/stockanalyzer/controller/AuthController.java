package com.stockanalyzer.controller;

import com.stockanalyzer.service.FivePaisaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@RestController
public class AuthController {

    private final FivePaisaService fivePaisaService;

    @GetMapping("/5paisa/login-url")
    public ResponseEntity<String> getLoginUrl() {
        return ResponseEntity.ok(fivePaisaService.getLoginUrl());
    }

    @PostMapping("/5paisa/submit-token")
    public ResponseEntity<String> submitToken(@RequestBody String requestToken) {
        try {
            fivePaisaService.generateAccessToken(requestToken);
            return ResponseEntity.ok("Access token generated successfully!");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                 .body("Error generating token: " + e.getMessage());
        }
    }
}
