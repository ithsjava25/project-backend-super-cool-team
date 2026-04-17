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

/**
 * Anropas av Spring Security när Google-inloggningen lyckas.
 *
 * Flödet:
 * 1. Användaren loggar in via Google
 * 2. Google skickar tillbaka användarens email till oss
 * 3. Vi kollar om emailen finns i staff-tabellen
 * → Ja: redirect till dashboard (Spring Security hanterar sessionen automatiskt)
 * → Nej: redirect med felmeddelande
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StaffRepository staffRepository;

    private static final String DASHBOARD_PATH = "/pages/dashboard.html";
    private static final String ERROR_PATH = "/pages/login.html?error=unauthorized";

    public OAuth2SuccessHandler(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");

        if (staffRepository.findByEmail(email).isEmpty()) {
            response.sendRedirect(request.getContextPath() + ERROR_PATH);
            return;
        }

        response.sendRedirect(request.getContextPath() + DASHBOARD_PATH);
    }
}