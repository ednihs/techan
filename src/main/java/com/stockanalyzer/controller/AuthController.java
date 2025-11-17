package com.stockanalyzer.controller;

import com.stockanalyzer.exception.AuthenticationFailedException;
import com.stockanalyzer.service.FivePaisaService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AuthController {

    private final FivePaisaService fivePaisaService;

    public AuthController(FivePaisaService fivePaisaService) {
        this.fivePaisaService = fivePaisaService;
    }

    @GetMapping("/reauthenticate")
    public ResponseEntity<String> reauthenticate(@RequestParam String totp) {
        try {
            fivePaisaService.authenticate(totp);
            return ResponseEntity.ok("Authentication successful!");
        } catch (AuthenticationFailedException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Authentication failed: " + e.getMessage());
        }
    }
}
