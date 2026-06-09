package com.sistema.gestao.sistemagestao.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI openAPI() {
        return new OpenAPI()
                .info(new Info()
                        .title("Sistema de Gestão Hoteleira")
                        .description("API REST para gerenciamento de hóspedes, imóveis e reservas")
                        .version("1.0.0")
                        .contact(new Contact()
                                .name("WelingtonNem21")
                                .email("welingtonalvesper@gmail.com")));
    }
}
