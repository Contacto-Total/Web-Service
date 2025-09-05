package com.foh.contacto_total_web_service;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;

@SpringBootApplication
public class ContactoTotalWebServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(ContactoTotalWebServiceApplication.class, args);
    }

}
