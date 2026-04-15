package org.example.cyberwatch.config.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Optional;

/**
 * Anropas av Spring Security när Google-inloggningen lyckas.
 *
 * Flödet:
 * 1. Användaren loggar in via Google
 * 2. Google skickar tillbaka användarens email till oss
 * 3. Vi kollar om emailen finns i staff-tabellen
 * → Ja: generera en JWT-token och skicka den till frontend
 * → Nej: redirect med felmeddelande (emailen är inte registrerad i systemet)
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final JwtService jwtService;
    private final StaffRepository staffRepository;

    // Byt ut mot riktiga frontend-URL när vi driftsätter
    private static final String FRONTEND_URL = "http://localhost:8080/pages/auth-callback.html";

    public OAuth2SuccessHandler(JwtService jwtService, StaffRepository staffRepository) {
        this.jwtService = jwtService;
        this.staffRepository = staffRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        // Hämta Google-användaren och plocka ut emailen
        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        Optional<Staff> staffOptional = staffRepository.findByEmail(email);

        if (staffOptional.isEmpty()) {
            // Emailen finns inte i systemet — användaren är inte anställd här
            response.sendRedirect(FRONTEND_URL + "?error=unauthorized");
            return;
        }

        // Emailen finns — generera en JWT-token med email och roll
        Staff staff = staffOptional.get();
        String token = jwtService.generateToken(staff.getEmail(), staff.getRole().name());

        // Skicka token till frontend via query-parameter i redirect-URL:en
        // Frontend sparar token och skickar den som "Authorization: Bearer <token>" på varje request
        response.sendRedirect(FRONTEND_URL + "?token=" + token);
    }
}