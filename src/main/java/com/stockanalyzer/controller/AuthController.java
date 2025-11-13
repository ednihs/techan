package com.stockanalyzer.controller;

import com.stockanalyzer.service.FivePaisaService;
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
    public String reauthenticate(@RequestParam String totp) {
        try {
            fivePaisaService.authenticate(totp);
            return "Authentication successful!";
        } catch (Exception e) {
            return "Authentication failed: " + e.getMessage();
        }
    }
}
