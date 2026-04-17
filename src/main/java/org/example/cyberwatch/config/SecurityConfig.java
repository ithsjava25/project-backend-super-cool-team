package org.example.cyberwatch.config;

import org.example.cyberwatch.config.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

/**
 * Huvudkonfiguration för Spring Security.
 *
 * Autentiseringsflöde:
 * - Inloggning sker via Google OAuth2 (/oauth2/authorization/google)
 * - Efter lyckad Google-inloggning sparar Spring Security sessionen automatiskt via cookie
 * - Alla efterföljande API-anrop autentiseras via sessions-cookie
 * - CSRF-skydd via XSRF-TOKEN cookie som JS läser och skickar som X-XSRF-TOKEN header
 */
@Configuration
@EnableWebSecurity
@EnableMethodSecurity
public class SecurityConfig {

    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF via cookie — JS läser XSRF-TOKEN och skickar som X-XSRF-TOKEN header
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                )

                .authorizeHttpRequests(auth -> auth
                        // Google OAuth2-flödets endpoints måste vara öppna
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // Statiska filer och frontend-sidor
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/pages/**",
                                "/css/**",
                                "/js/**"
                        ).permitAll()
                        // Endast HR, CEO, CTO & ADMIN får hantera staff
                        .requestMatchers("/api/staff/**").hasAnyRole("HR", "CEO", "CTO", "ADMIN")
                        // Endast HR, CEO, CTO & ADMIN får hantera forms
                        .requestMatchers("/api/forms/**").hasAnyRole("HR", "CEO", "CTO", "ADMIN")
                        // Alla inloggade kan komma åt tickets
                        .requestMatchers("/api/tickets/**").authenticated()
                        // Alla andra endpoints kräver inloggning
                        .anyRequest().authenticated()
                )

                // Konfigurera Google OAuth2-inloggning med vår egen success handler
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )

                // Redirect till login vid 401
                .exceptionHandling(ex -> ex
                        .authenticationEntryPoint((request, response, authException) -> {
                            if (request.getRequestURI().startsWith("/api/")) {
                                response.sendError(401);
                            } else {
                                response.sendRedirect("/pages/login.html");
                            }
                        })
                )

                .build();
    }
}