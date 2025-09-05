package com.foh.contacto_total_web_service.iam.interfaces.rest.controllers;

import com.foh.contacto_total_web_service.iam.domain.model.queries.GetAllTokensByUsernameQuery;
import com.foh.contacto_total_web_service.iam.domain.services.TokenQueryService;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.TokenResource;
import com.foh.contacto_total_web_service.iam.interfaces.rest.transform.TokenResourceFromEntityAssembler;
import com.foh.contacto_total_web_service.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(value = "/api/v1/tokens", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Tokens", description = "Token Endpoints")
public class TokensController {

    @Autowired
    private TokenQueryService tokenQueryService;

    @Operation(summary = "List tokens", description = "Return JWT tokens by username")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Tokens received",
                    content = @Content(array = @ArraySchema(schema = @Schema(implementation = TokenResource.class)))
            ),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @GetMapping("/{username}")
    public ResponseEntity<?> listTokens(@PathVariable String username) {
        var getAllTokensByUsernameQuery = new GetAllTokensByUsernameQuery(username);
        var tokens = tokenQueryService.handle(getAllTokensByUsernameQuery);
        var tokenResources = tokens.stream().map(TokenResourceFromEntityAssembler::toResourceFromEntity);
        return ResponseEntity.ok(tokenResources);
    }
}
