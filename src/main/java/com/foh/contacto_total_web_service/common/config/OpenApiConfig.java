package com.foh.contacto_total_web_service.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI controlTotalServicePlatformOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Maintenance Service Platform API")
                        .description("Control Total Service Platform application REST API documentation.")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")))
                        .servers(List.of(
                                new Server().url("https://martin-set-gelding.ngrok-free.app").description("Hola"),
                                new Server().url("https://perfect-charmed-colt.ngrok-free.app").description("Ngrok HTTPS"),
                                new Server().url("http://localhost:8080").description("Local Dev")
                        ));
    }

}
