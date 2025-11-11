package com.stockanalyzer.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class OrderResponseDTO {
    private String orderId;
    private String status;
    private String message;
}
