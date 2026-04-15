package org.example.cyberwatch.config;

import org.example.cyberwatch.config.security.JwtAuthFilter;
import org.example.cyberwatch.config.security.OAuth2SuccessHandler;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Huvudkonfiguration för Spring Security.
 *
 * Autentiseringsflöde:
 * - Inloggning sker via Google OAuth2 (/oauth2/authorization/google)
 * - Efter lyckad Google-inloggning genereras en intern JWT-token (OAuth2SuccessHandler)
 * - Alla efterföljande API-anrop autentiseras via JWT-token i Authorization-headern (JwtAuthFilter)
 * - Sessioner används inte — varje request är självständig (STATELESS)
 */
@Configuration
@EnableWebSecurity
// Aktiverar @PreAuthorize på service-metoder
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthFilter jwtAuthFilter;
    private final OAuth2SuccessHandler oAuth2SuccessHandler;

    public SecurityConfig(JwtAuthFilter jwtAuthFilter, OAuth2SuccessHandler oAuth2SuccessHandler) {
        this.jwtAuthFilter = jwtAuthFilter;
        this.oAuth2SuccessHandler = oAuth2SuccessHandler;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        return http
                // CSRF inaktiveras eftersom vi använder JWT istället för sessions/cookies
                .csrf(csrf -> csrf.disable())

                // Inga server-side sessioner — varje request autentiseras via JWT
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS))

                .authorizeHttpRequests(auth -> auth
                        // Google OAuth2-flödets endpoints måste vara öppna
                        .requestMatchers("/oauth2/**", "/login/**").permitAll()
                        // Endast ADMIN, HR, CEO & CTO får hantera staff
                        .requestMatchers("/api/staff/**").hasAnyRole("HR", "CEO", "CTO", "ADMIN")
                        // Endast ADMIN, HR, CEO & CTO får hantera forms
                        .requestMatchers("/api/forms/**").hasAnyRole("HR", "CEO", "CTO", "ADMIN")
                        // Alla kan komma åt tickets
                        .requestMatchers("/api/tickets/**").authenticated()
                        // Alla andra endpoints kräver inloggning, djupare hantering av vem som får göra vad sköts i Service-lagret
                        // Tillåt statiska filer och frontend-sidor
                        .requestMatchers(
                                "/",
                                "/index.html",
                                "/pages/**",
                                "/css/**",
                                "/js/**",
                                "/oauth2/**",
                                "/login/**"
                        ).permitAll()

                        // Endast ADMIN får hantera staff
                        .requestMatchers("/api/staff/**").hasRole("ADMIN")

                        // Alla andra endpoints kräver inloggning
                        .anyRequest().authenticated()
                )

                // Konfigurera Google OAuth2-inloggning med vår egen success handler
                .oauth2Login(oauth2 -> oauth2
                        .successHandler(oAuth2SuccessHandler)
                )

                // Kör JWT-filtret innan Spring Securitys eget autentiseringsfilter
                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class)

                .build();
    }
}