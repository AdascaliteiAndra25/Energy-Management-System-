package org.example.config;

import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.Contact;

import io.swagger.v3.oas.models.OpenAPI;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI authorizationOpenAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Authorization API")
                        .description("API for authentication and authorization in the Energy Management System")
                        .version("1.0")
                        .contact(new Contact()
                                .name("Energy System Team")
                                .email("support@energysystem.com")));
    }
}
