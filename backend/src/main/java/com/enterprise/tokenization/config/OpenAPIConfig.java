package com.enterprise.tokenization.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Contact;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import io.swagger.v3.oas.models.servers.Server;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

/**
 * Configuration for OpenAPI/Swagger documentation.
 * Provides interactive API documentation with JWT authentication support.
 */
@Slf4j
@Configuration
public class OpenAPIConfig {

    @Value("${spring.application.name:enterprise-asset-tokenization}")
    private String applicationName;

    @Value("${server.port:8080}")
    private String serverPort;

    /**
     * Configures OpenAPI documentation with JWT Bearer token authentication.
     *
     * @return Configured OpenAPI instance
     */
    @Bean
    public OpenAPI customOpenAPI() {
        log.info("Configuring OpenAPI documentation");

        final String securitySchemeName = "Bearer Authentication";

        return new OpenAPI()
                // API Information
                .info(new Info()
                        .title("Enterprise Asset Tokenization Platform API")
                        .version("1.0.0")
                        .description("""
                                RESTful API for Enterprise Asset Tokenization on Hyperledger Besu.

                                This platform enables secure tokenization of enterprise assets on a private
                                blockchain network using ERC-1155 standard. Features include:

                                - User authentication and authorization with JWT
                                - Asset minting, burning, and transfer operations
                                - Role-based access control (ADMIN, MINTER, BURNER, COMPLIANCE_OFFICER)
                                - Compliance tracking and audit trails
                                - Real-time blockchain interaction via Web3j

                                **Authentication:**
                                1. Register a user account or login with existing credentials at `/api/auth/login`
                                2. Copy the JWT token from the response
                                3. Click the 'Authorize' button above and enter: `Bearer <your-token>`
                                4. All subsequent API calls will include the token automatically

                                **Base URL:** `http://localhost:8080`
                                """)
                        .contact(new Contact()
                                .name("Enterprise Tokenization Team")
                                .email("support@enterprise-tokenization.com")
                                .url("https://github.com/enterprise/asset-tokenization-platform")
                        )
                        .license(new License()
                                .name("Apache 2.0")
                                .url("https://www.apache.org/licenses/LICENSE-2.0.html")
                        )
                )

                // Server Configuration
                .servers(List.of(
                        new Server()
                                .url("http://localhost:" + serverPort)
                                .description("Local Development Server"),
                        new Server()
                                .url("https://api-staging.enterprise-tokenization.com")
                                .description("Staging Environment"),
                        new Server()
                                .url("https://api.enterprise-tokenization.com")
                                .description("Production Environment")
                ))

                // Security Configuration - JWT Bearer Token
                .addSecurityItem(new SecurityRequirement()
                        .addList(securitySchemeName)
                )
                .components(new Components()
                        .addSecuritySchemes(securitySchemeName,
                                new SecurityScheme()
                                        .name(securitySchemeName)
                                        .type(SecurityScheme.Type.HTTP)
                                        .scheme("bearer")
                                        .bearerFormat("JWT")
                                        .description("""
                                                Enter your JWT token in the format: `Bearer <token>`

                                                Tokens can be obtained by authenticating at:
                                                - POST /api/auth/login
                                                - POST /api/auth/register

                                                Token expires after 24 hours by default.
                                                """)
                        )
                );
    }
}