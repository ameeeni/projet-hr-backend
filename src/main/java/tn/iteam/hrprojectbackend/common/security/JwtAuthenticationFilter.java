package tn.iteam.hrprojectbackend.common.security;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Filtre JWT : extrait les authorities DIRECTEMENT depuis le token IAM.
 * Token IAM contient : subject=username, authorities=["ROLE_ADMIN","ROLE_HR","ROLE_MANAGER","ROLE_EMPLOYEE"]
 * Pas besoin de charger l'utilisateur depuis la base HR.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private final JwtTokenProvider jwtTokenProvider;

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request,
                                    @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws ServletException, IOException {

        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            filterChain.doFilter(request, response);
            return;
        }

        String token = authHeader.substring(7);
        try {
            if (!jwtTokenProvider.isTokenValid(token)) {
                log.warn("Token JWT invalide ou expiré");
                filterChain.doFilter(request, response);
                return;
            }

            String username = jwtTokenProvider.extractUsername(token);
            if (username != null && SecurityContextHolder.getContext().getAuthentication() == null) {
                List<String> rawAuthorities = jwtTokenProvider.extractAuthorities(token);
                if (rawAuthorities != null && !rawAuthorities.isEmpty()) {
                    List<SimpleGrantedAuthority> authorities = rawAuthorities.stream()
                            .map(role -> role.startsWith("ROLE_") ? role : "ROLE_" + role)
                            // Mapper ROLE_RH (IAM) → ROLE_HR (HR Backend)
                            .map(role -> role.equals("ROLE_RH") ? "ROLE_HR" : role)
                            .map(SimpleGrantedAuthority::new)
                            .collect(Collectors.toList());

                    // Ajouter aussi les permissions en tant qu'autorités (sans préfixe ROLE_)
                    List<String> permissions = jwtTokenProvider.extractPermissions(token);
                    if (permissions != null && !permissions.isEmpty()) {
                        permissions.stream()
                                // Ne pas ajouter ROLE_ aux permissions, elles sont déjà correctes
                                .map(perm -> {
                                    // Mapper HR_* permissions vers le format attendu
                                    if (perm.startsWith("HR_")) {
                                        return perm;
                                    }
                                    return perm;
                                })
                                .map(SimpleGrantedAuthority::new)
                                .forEach(authorities::add);
                    }

                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(username, null, authorities);
                    authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(authentication);
                    log.info("✅ Utilisateur '{}' authentifié avec {} autorités : {}", username, authorities.size(), authorities);
                } else {
                    log.warn("Token valide mais aucun rôle trouvé pour l'utilisateur '{}'", username);
                }
            }
        } catch (Exception e) {
            log.error("Erreur lors du traitement du token JWT : {}", e.getMessage());
            SecurityContextHolder.clearContext();
        }

        filterChain.doFilter(request, response);
    }
}
