package com.foh.contacto_total_web_service.iam.interfaces.rest.controllers;

import com.foh.contacto_total_web_service.iam.domain.model.commands.SignOutCommand;
import com.foh.contacto_total_web_service.iam.domain.services.TokenQueryService;
import com.foh.contacto_total_web_service.iam.domain.services.UserCommandService;
import com.foh.contacto_total_web_service.iam.domain.services.UserQueryService;
import com.foh.contacto_total_web_service.iam.interfaces.rest.resources.*;
import com.foh.contacto_total_web_service.iam.interfaces.rest.transform.*;
import com.foh.contacto_total_web_service.shared.interfaces.rest.resources.ErrorResponseResource;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping(value = "/api/v1/authentication", produces = MediaType.APPLICATION_JSON_VALUE)
@Tag(name = "Authentication", description = "Authentication and Authorization Endpoints")
public class AuthenticationController {

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthenticationController.class);

    @Autowired
    private UserCommandService userCommandService;

    @Autowired
    private UserQueryService userQueryService;

    @Autowired
    private TokenQueryService tokenQueryService;



    @Operation(summary = "User Sign In", description = "Authenticate user and return JWT tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful",
                    content = @Content(schema = @Schema(implementation = AuthenticatedUserResource.class))),
            @ApiResponse(responseCode = "401", description = "Invalid credentials",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @PostMapping("/sign-in")
    public ResponseEntity<?> signIn(@Valid @RequestBody SignInResource signInResource) {
        try {
            var signInCommand = SignInCommandFromResourceAssembler.toCommandFromResource(signInResource);
            var signInResult = userCommandService.handle(signInCommand);

            if (signInResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseResource("User not found", "USER_NOT_FOUND"));
            }

            var result = signInResult.get();
            var user = result.getLeft();
            var tokens = result.getRight();

            var authenticatedUserResource = AuthenticatedUserResourceFromEntityAssembler.toResourceFromEntity(
                    user, tokens.get("accessToken"), tokens.get("refreshToken"), tokens.get("tokenType")
            );

            return ResponseEntity.ok(authenticatedUserResource);

        } catch (BadCredentialsException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseResource("Invalid credentials", "INVALID_CREDENTIALS"));
        } catch (Exception e) {
            LOGGER.error("Authentication error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Authentication failed", "AUTH_FAILED"));
        }
    }

    @Operation(summary = "User Sign Up", description = "Register a new user")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "201", description = "User registered successfully",
                    content = @Content(schema = @Schema(implementation = RegistratedUserResource.class))),
            @ApiResponse(responseCode = "400", description = "Invalid input or user already exists",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @PostMapping("/sign-up")
    public ResponseEntity<?> signUp(@Valid @RequestBody SignUpResource signUpResource) {
        try {
            var signUpCommand = SignUpCommandFromResourceAssembler.toCommandFromResource(signUpResource);
            var user = userCommandService.handle(signUpCommand);
            if (user.isEmpty()) {
                return ResponseEntity.badRequest()
                        .body(new ErrorResponseResource("Registration failed", "REGISTRATION_FAILED"));
            }
            var registratedUserResource = new RegistratedUserResource(user.get().getUsername(), "User registered successfully");
            return ResponseEntity.status(HttpStatus.CREATED).body(registratedUserResource);

        } catch (RuntimeException e) {
            return ResponseEntity.badRequest()
                    .body(new ErrorResponseResource(e.getMessage(), "REGISTRATION_ERROR"));
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Registration failed", "INTERNAL_ERROR"));
        }
    }

    @Operation(summary = "Refresh Token", description = "Generate new access token using refresh token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully",
                    content = @Content(schema = @Schema(implementation = RefreshedTokenResource.class))),
            @ApiResponse(responseCode = "401", description = "Invalid refresh token",
                    content = @Content(schema = @Schema(implementation = ErrorResponseResource.class)))
    })
    @PostMapping("/refresh")
    public ResponseEntity<?> refreshToken(@Valid @RequestBody RefreshTokenResource refreshTokenResource) {
        try {
            var refreshTokenCommand = RefreshTokenCommandFromResourceAssembler.toCommandFromResource(refreshTokenResource);

            var refreshTokenResult = userCommandService.handle(refreshTokenCommand);

            if (refreshTokenResult.isEmpty()) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ErrorResponseResource("Invalid refresh token", "INVALID_REFRESH_TOKEN"));
            }

            var tokens = refreshTokenResult.get().getRight();

            var refreshedTokenResource = new RefreshedTokenResource(
                    tokens.get("accessToken"),
                    tokens.get("refreshToken"),
                    tokens.get("tokenType")
            );

            return ResponseEntity.ok(refreshedTokenResource);

        } catch (Exception e) {
            LOGGER.error("Token refresh error", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseResource("Token refresh failed", "TOKEN_REFRESH_FAILED"));
        }
    }

    @Operation(summary = "Logout", description = "Logout user and invalidate tokens")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Logout successful"),
            @ApiResponse(responseCode = "401", description = "Unauthorized")
    })
    @PostMapping("/sign-out")
    public ResponseEntity<?> signOut(HttpServletRequest request) {
        try {
            String authHeader = request.getHeader("Authorization");
            var command = new SignOutCommand(authHeader);
            userCommandService.handle(command);

            Map<String, String> response = new HashMap<>();
            response.put("message", "Sign out successful");
            return ResponseEntity.ok(response);

        } catch (Exception e) {
            LOGGER.error("Logout error", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(new ErrorResponseResource("Sign out failed", "LOGOUT_FAILED"));
        }
    }


    @Operation(summary = "Validate Token", description = "Validate JWT token")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token is valid"),
            @ApiResponse(responseCode = "401", description = "Invalid token")
    })
    @PostMapping("/validate")
    public ResponseEntity<?> validateToken(@Valid @RequestBody ValidateTokenResource validateTokenResource) {
        try {

            var command = ValidateTokenCommandFromResourceAssembler.toCommandFromResource(validateTokenResource);
            var validateTokenResult = userCommandService.handle(command);

            if (validateTokenResult.isPresent()) {
                return ResponseEntity.ok(new ValidatedTokenResource(true, validateTokenResult.get()));
            } else {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ValidatedTokenResource(false, null));
            }

        } catch (Exception e) {
            LOGGER.error("Token validation failed", e);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(new ErrorResponseResource("Token validation failed", "TOKEN_VALIDATION_FAILED"));
        }
    }
}