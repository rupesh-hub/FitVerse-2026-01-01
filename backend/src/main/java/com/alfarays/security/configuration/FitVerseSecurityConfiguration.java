package com.alfarays.security.configuration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;

import java.util.Arrays;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(securedEnabled = true)
public class FitVerseSecurityConfiguration {

    @Value("${application.cors.origins}")
    private List<String> allowedOrigins;

    private final String[] publicUris = {"/messages", "/messages/**", "/actuator", "/actuator/**"};

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.cors(
                config -> config.configurationSource(
                        request -> {
                            final CorsConfiguration configuration = new CorsConfiguration();
                            configuration.setAllowCredentials(true);
                            configuration.setAllowedOrigins(allowedOrigins);
                            configuration.setAllowedHeaders(Arrays.asList("Origin", "Content-Type", "Accept", "Authorization"));
                            configuration.setAllowedMethods(Arrays.asList("GET", "POST", "DELETE", "PUT", "PATCH"));
                            return configuration;
                        }
                )
        );

        http.csrf(config -> config.ignoringRequestMatchers(publicUris));
        http.authorizeHttpRequests(config -> config.requestMatchers(publicUris)
                .permitAll()
                .anyRequest()
                .authenticated());

        http.formLogin(Customizer.withDefaults());
        http.httpBasic(Customizer.withDefaults());

        return http.build();
    }

}
