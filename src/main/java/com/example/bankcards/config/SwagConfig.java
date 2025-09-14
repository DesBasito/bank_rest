package com.example.bankcards.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class SwagConfig {
    @Value("${server.port:8080}")
    private String serverPort;

    @Value("${server.servlet.context-path}")
    private String contextPath;

    @Bean
    public OpenAPI customOpenAPI() {
        return new OpenAPI()
                .info(getApiInfo())
                .servers(getServers())
                .addSecurityItem(new SecurityRequirement().addList("Bearer Authentication"))
                .components(new Components()
                        .addSecuritySchemes("Bearer Authentication", createAPIKeyScheme()));
    }

    private Info getApiInfo() {
        return new Info()
                .title("API Системы Управления Банковскими Картами")
                .description("""
                        REST API для управления банковскими картами с функциями:
                        
                        - Аутентификация и авторизация пользователей
                        - Управление картами (создание, просмотр, блокировка)
                        - Переводы между картами
                        - Администрирование системы
                        
                        **Роли пользователей:**
                        - USER: базовые операции с собственными картами
                        - ADMIN: полный доступ к системе
                        
                        **Безопасность:**
                        - JWT токены для аутентификации
                        - Шифрование номеров карт
                        - Маскирование чувствительных данных
                        """)
                .version("1.0.0")
                .contact(new Contact()
                        .name("Abu")
                        .email("out1of1mind1exception@gmail.com")
                        .url("https://github.com/DesBasito"));
    }

    private List<Server> getServers() {
        return List.of(
                new Server()
                        .url("http://localhost:" + this.serverPort + this.contextPath)
                        .description("Локальный сервер разработки"));
    }

    private SecurityScheme createAPIKeyScheme() {
        return new SecurityScheme()
                .type(SecurityScheme.Type.HTTP)
                .bearerFormat("JWT")
                .scheme("bearer")
                .description("Enter JWT token in format: Bearer {token}")
                .name("Authorization")
                .in(SecurityScheme.In.HEADER);
    }
}
