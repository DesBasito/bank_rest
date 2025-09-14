package com.example.bankcards.controller;

import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/cards")
@SecurityRequirement(name = "Bearer Authentication")
@RequiredArgsConstructor
public class TransactionController {
}
