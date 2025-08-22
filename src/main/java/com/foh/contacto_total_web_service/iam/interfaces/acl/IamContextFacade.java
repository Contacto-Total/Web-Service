package com.foh.contacto_total_web_service.iam.interfaces.acl;

import com.foh.contacto_total_web_service.iam.domain.model.aggregates.User;
import com.foh.contacto_total_web_service.iam.domain.model.commands.SignUpCommand;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetUserByEmailQuery;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetUserByIdQuery;
import com.foh.contacto_total_web_service.iam.domain.services.UserCommandService;
import com.foh.contacto_total_web_service.iam.domain.services.UserQueryService;
import org.apache.logging.log4j.util.Strings;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

/**
 * IamContextFacade provides a simplified interface for user management operations.
 * It allows creating users and fetching user details by username or user ID.
 */
@Service
public class IamContextFacade {
    private final UserCommandService userCommandService;
    private final UserQueryService userQueryService;

    public IamContextFacade(UserCommandService userCommandService, UserQueryService userQueryService) {
        this.userCommandService = userCommandService;
        this.userQueryService = userQueryService;
    }

    public Long createUser(String username, String email, String password) {
        var signUpCommand = new SignUpCommand(username, email, password, List.of("ROLE_USER"));
        var result = userCommandService.handle(signUpCommand);
        if (result.isEmpty()) return 0L;
        return result.get().getId();
    }

    public Long createUser(String username, String email, String password, List<String> roleNames) {
        //var roles = roleNames != null ? roleNames.stream().map(Role::toRoleFromName).toList() : new ArrayList<Role>();
        if (roleNames == null) roleNames = new ArrayList<>();
        var signUpCommand = new SignUpCommand(username, email, password, roleNames);
        var result = userCommandService.handle(signUpCommand);
        if (result.isEmpty()) return 0L;
        return result.get().getId();
    }

    public Long fetchUserIdByUsername(String username) {
        var getUserByUsernameQuery = new GetUserByEmailQuery(username);
        var result = userQueryService.handle(getUserByUsernameQuery);
        if (result.isEmpty()) return 0L;
        return result.get().getId();
    }

    public String fetchUsernameByUserId(Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        var result = userQueryService.handle(getUserByIdQuery);
        if (result.isEmpty()) return Strings.EMPTY;
        return result.get().getUsername();
    }

    public User fetchUserById(Long userId) {
        var getUserByIdQuery = new GetUserByIdQuery(userId);
        return userQueryService.handle(getUserByIdQuery).orElse(null);
    }

}
