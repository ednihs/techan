package com.stockanalyzer.controller;

import com.stockanalyzer.dto.HoldingDTO;
import com.stockanalyzer.dto.OrderRequestDTO;
import com.stockanalyzer.dto.OrderResponseDTO;
import com.stockanalyzer.service.FivePaisaService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/portfolio")
@RequiredArgsConstructor
public class PortfolioController {

    private final FivePaisaService fivePaisaService;

    @GetMapping("/holdings")
    public ResponseEntity<List<HoldingDTO>> getHoldings() {
        return ResponseEntity.ok(fivePaisaService.getHoldings());
    }

    @PostMapping("/order")
    public ResponseEntity<OrderResponseDTO> placeOrder(@RequestBody OrderRequestDTO orderRequest) {
        return ResponseEntity.ok(fivePaisaService.placeOrder(orderRequest));
    }

    @DeleteMapping("/order/{orderId}")
    public ResponseEntity<OrderResponseDTO> cancelOrder(@PathVariable String orderId) {
        return ResponseEntity.ok(fivePaisaService.cancelOrder(orderId));
    }
}
