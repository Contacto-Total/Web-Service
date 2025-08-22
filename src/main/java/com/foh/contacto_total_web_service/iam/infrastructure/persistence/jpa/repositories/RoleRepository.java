package com.foh.contacto_total_web_service.iam.infrastructure.persistence.jpa.repositories;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Role;
import com.foh.contacto_total_web_service.iam.domain.model.valueobjects.Roles;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RoleRepository extends JpaRepository<Role, Long>{

    boolean existsByName(Roles name);

    Optional<Role> findByName(Roles roles);
}
