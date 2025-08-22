package com.foh.contacto_total_web_service.shared.domain.services;

public interface DomainEventPublisherService {
    void publish(Object event);
}
