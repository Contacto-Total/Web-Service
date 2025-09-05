package com.foh.contacto_total_web_service.iam.domain.services;


import com.foh.contacto_total_web_service.iam.domain.model.entities.Token;
import com.foh.contacto_total_web_service.iam.domain.model.queries.GetAllTokensByUsernameQuery;

import java.util.List;

public interface TokenQueryService {
    List<Token> handle(GetAllTokensByUsernameQuery query);
}