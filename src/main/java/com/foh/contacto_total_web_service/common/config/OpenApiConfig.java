package com.foh.contacto_total_web_service.common.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.servers.Server;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI controlTotalServicePlatformOpenApi() {
        return new OpenAPI()
                .addServersItem(new Server().url("https://24a0226028e5.ngrok-free.app"))
                .info(new Info()
                        .title("Maintenance Service Platform API")
                        .description("Control Total Service Platform application REST API documentation.")
                        .version("v1.0.0")
                        .license(new License().name("Apache 2.0").url("https://springdoc.org")));
    }

}
