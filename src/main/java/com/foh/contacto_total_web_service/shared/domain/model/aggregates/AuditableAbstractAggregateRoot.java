package com.foh.contacto_total_web_service.shared.domain.model.aggregates;

import jakarta.persistence.*;
import lombok.Getter;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedBy;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@MappedSuperclass
@EntityListeners(AuditingEntityListener.class)
@Getter
public abstract class AuditableAbstractAggregateRoot<T extends AuditableAbstractAggregateRoot<T>> {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false)
    private LocalDateTime updatedAt;

    @CreatedBy
    @Column(nullable = false, updatable = false)
    private String createdBy;

    @LastModifiedBy
    @Column(nullable = false)
    private String lastModifiedBy;

    @Version
    private Long version;

    // Campos adicionales de auditoría
    @Column(name = "created_ip")
    private String createdFromIp;

    @Column(name = "last_modified_ip")
    private String lastModifiedFromIp;

    @Column(name = "is_active")
    private Boolean isActive = true;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
        isActive = true;
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }

    // Métodos de conveniencia para auditoría
    public boolean isCreatedBy(String username) {
        return createdBy != null && createdBy.equals(username);
    }

    public boolean wasModifiedBy(String username) {
        return lastModifiedBy != null && lastModifiedBy.equals(username);
    }

    public void markAsDeleted() {
        this.isActive = false;
    }

    public void markAsActive() {
        this.isActive = true;
    }

    public boolean isDeleted() {
        return !isActive;
    }
}
