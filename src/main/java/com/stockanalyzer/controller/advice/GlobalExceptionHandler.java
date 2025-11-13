package com.stockanalyzer.controller.advice;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.NoHandlerFoundException;

import jakarta.servlet.http.HttpServletRequest;

@ControllerAdvice
@Slf4j
public class GlobalExceptionHandler {

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<String> handleNotFound(HttpServletRequest request, NoHandlerFoundException ex) {
        log.warn("Request for non-existent endpoint: {} {}", request.getMethod(), request.getRequestURI());
        return new ResponseEntity<>("Endpoint not found", HttpStatus.NOT_FOUND);
    }
}
