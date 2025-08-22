package com.foh.contacto_total_web_service.iam.domain.services;

import com.foh.contacto_total_web_service.iam.domain.model.entities.Role;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetAllRolesQuery;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetRoleByNameQuery;

import java.util.List;
import java.util.Optional;

public interface RoleQueryService {
    List<Role> handle(GetAllRolesQuery query);
    Optional<Role> handle(GetRoleByNameQuery query);
}
