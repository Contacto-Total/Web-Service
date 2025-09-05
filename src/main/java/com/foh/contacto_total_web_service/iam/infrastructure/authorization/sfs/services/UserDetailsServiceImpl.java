package com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.services;

import com.foh.contacto_total_web_service.iam.domain.model.queries.GetUserByEmailQuery;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetUserByIdQuery;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetUserByUsernameQuery;
import com.foh.contacto_total_web_service.iam.domain.services.UserQueryService;
import com.foh.contacto_total_web_service.iam.infrastructure.authorization.sfs.model.UserDetailsImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service("defaultUserDetailsService")
public class UserDetailsServiceImpl implements UserDetailsService {

    @Autowired
    private UserQueryService userQueryService;

    @Override
    @Transactional
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var getUserByUsernameQuery = new GetUserByUsernameQuery(username);
        var user = userQueryService.handle(getUserByUsernameQuery)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with username: " + username));

        return  UserDetailsImpl.build(user);
    }

    @Transactional
    public UserDetails loadUserById(Long id) {
        var getUserByIdQuery = new GetUserByIdQuery(id);
        var user = userQueryService.handle(getUserByIdQuery)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + id));

        return UserDetailsImpl.build(user);
    }
}