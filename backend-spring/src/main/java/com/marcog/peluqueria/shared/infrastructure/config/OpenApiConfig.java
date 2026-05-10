package com.marcog.peluqueria.shared.infrastructure.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI peluqueriaOpenAPI() {
        return new OpenAPI()
            .info(new Info()
                .title("API Peluqueria Isabella")
                .version("2.0.0")
                .description("Sistema de gestion integral de peluqueria: citas, clientes, empleados, productos, finanzas y mas")
                .contact(new Contact().name("Marco").email("internetemg75@gmail.com")))
            .addSecurityItem(new SecurityRequirement().addList("Bearer"))
            .components(new Components()
                .addSecuritySchemes("Bearer",
                    new SecurityScheme()
                        .type(SecurityScheme.Type.HTTP)
                        .scheme("bearer")
                        .bearerFormat("JWT")
                        .description("Token JWT obtenido en /api/auth/login")));
    }
}
