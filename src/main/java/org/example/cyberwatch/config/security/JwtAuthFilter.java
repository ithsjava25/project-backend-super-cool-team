package org.example.cyberwatch.config.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;

/**
 * Körs på varje inkommande HTTP-request och kontrollerar om det finns en giltig JWT-token.
 *
 * Om Authorization-headern innehåller "Bearer <token>" valideras tokenen och
 * användaren autentiseras i Spring Securitys SecurityContext för den aktuella requesten.
 * Utan giltig token fortsätter requesten oinloggad och nekas av SecurityConfig om
 * endpointen kräver autentisering.
 */
@Component
public class JwtAuthFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final StaffRepository staffRepository;

    public JwtAuthFilter(JwtService jwtService, StaffRepository staffRepository) {
        this.jwtService = jwtService;
        this.staffRepository = staffRepository;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    HttpServletResponse response,
                                    FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");

        // Om headern saknas eller inte börjar med "Bearer " — hoppa över filtret
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        // Plocka ut själva token-strängen efter "Bearer "
        String token = authHeader.substring(7);

        // Validera signaturen och att tokenen inte gått ut
        if (!jwtService.isTokenValid(token)) {
            filterChain.doFilter(request, response);
            return;
        }

        String email = jwtService.extractEmail(token);

        // Om användaren inte redan är autentiserad i denna request — slå upp i databasen
        if (email != null && SecurityContextHolder.getContext().getAuthentication() == null) {
            Staff staff = staffRepository.findByEmail(email).orElse(null);

            if (staff != null) {
                // Sätt upp autentiseringen med rätt roll (t.ex. ROLE_ADMIN, ROLE_HR)
                UsernamePasswordAuthenticationToken authToken =
                        new UsernamePasswordAuthenticationToken(
                                staff,
                                null,
                                List.of(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()))
                        );
                authToken.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                SecurityContextHolder.getContext().setAuthentication(authToken);
            }
        }

        filterChain.doFilter(request, response);
    }
}