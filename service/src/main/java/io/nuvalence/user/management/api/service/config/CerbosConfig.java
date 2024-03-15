package io.nuvalence.user.management.api.service.config;

import dev.cerbos.sdk.CerbosBlockingClient;
import dev.cerbos.sdk.CerbosClientBuilder;
import io.nuvalence.auth.access.AuthorizationHandler;
import io.nuvalence.auth.access.cerbos.CerbosAuthorizationHandler;
import io.nuvalence.user.management.api.service.service.EmployerProfileLinkService;
import io.nuvalence.user.management.api.service.service.IndividualProfileLinkService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.Scope;
import org.springframework.context.annotation.ScopedProxyMode;

/**
 * Configures CerbosAuthorizationHandler.
 */
@Configuration
@RequiredArgsConstructor
@Profile("!test")
public class CerbosConfig {
    private final IndividualProfileLinkService individualProfileLinkService;
    private final EmployerProfileLinkService employerProfileLinkService;

    @Value("${cerbos.uri}")
    private String cerbosUri;

    /**
     * Initializes a CerbosAuthorizationHandler as a singleton bean.
     *
     * @return AuthorizationHandler
     * @throws CerbosClientBuilder.InvalidClientConfigurationException if cerbos URI is invalid
     */
    @Bean
    @Scope(value = "request", proxyMode = ScopedProxyMode.TARGET_CLASS)
    public AuthorizationHandler getAuthorizationHandler()
            throws CerbosClientBuilder.InvalidClientConfigurationException {
        final CerbosBlockingClient cerbosClient =
                new CerbosClientBuilder(cerbosUri).withPlaintext().buildBlockingClient();

        return new CerbosAuthorizationHandler(
                cerbosClient,
                new CerbosPrincipalAttributeProviderImpl(
                        individualProfileLinkService, employerProfileLinkService));
    }
}
