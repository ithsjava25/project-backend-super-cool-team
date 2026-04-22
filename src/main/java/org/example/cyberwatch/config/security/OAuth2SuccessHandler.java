package org.example.cyberwatch.config.security;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.cyberwatch.features.staff.model.Staff;
import org.example.cyberwatch.features.staff.repository.StaffRepository;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.List;
import java.util.Optional;

/**
 * Anropas av Spring Security när Google-inloggningen lyckas.
 *
 * Flödet:
 * 1. Användaren loggar in via Google
 * 2. Google skickar tillbaka användarens email
 * 3. Vi kollar om emailen finns i staff-tabellen
 * → Ja: byt ut OAuth2User mot Staff som principal i SecurityContext, redirect till dashboard
 * → Nej: redirect med felmeddelande
 */
@Component
public class OAuth2SuccessHandler extends SimpleUrlAuthenticationSuccessHandler {

    private final StaffRepository staffRepository;
    private static final String ERROR_PATH = "/pages/login.html?error=unauthorized";

    public OAuth2SuccessHandler(StaffRepository staffRepository) {
        this.staffRepository = staffRepository;
        setDefaultTargetUrl("/pages/dashboard.html");
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request,
                                        HttpServletResponse response,
                                        Authentication authentication) throws IOException, ServletException {

        OAuth2User oAuth2User = (OAuth2User) authentication.getPrincipal();
        String email = oAuth2User.getAttribute("email");
        String picture = oAuth2User.getAttribute("picture");

        if (email == null) {
            response.sendRedirect(request.getContextPath() + ERROR_PATH);
            return;
        }

        Optional<Staff> staffOpt = staffRepository.findByEmail(email);
        if (staffOpt.isEmpty()) {
            response.sendRedirect(request.getContextPath() + ERROR_PATH);
            return;
        }

        // Byt ut OAuth2User mot Staff som principal så att alla efterföljande
        // anrop till authentication.getPrincipal() får ett Staff-objekt
        Staff staff = staffOpt.get();

        if (picture != null && !picture.isBlank()) {
            staff.setProfilePictureUrl(picture);
        }

        if (staff.getStatus() == null || staff.getStatus().isBlank()) {
            staff.setStatus("ONLINE");
        }

        staffRepository.save(staff);

        var authorities = List.of(new SimpleGrantedAuthority("ROLE_" + staff.getRole().name()));
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                staff, null, authorities
        );
        SecurityContextHolder.getContext().setAuthentication(auth);

        super.onAuthenticationSuccess(request, response, auth);
    }
}