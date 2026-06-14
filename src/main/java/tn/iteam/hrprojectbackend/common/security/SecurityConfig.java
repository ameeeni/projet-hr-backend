package tn.iteam.hrprojectbackend.common.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@RequiredArgsConstructor
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
            .csrf(AbstractHttpConfigurer::disable)
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                // Swagger UI
                .requestMatchers(
                    "/swagger-ui/**",
                    "/swagger-ui.html",
                    "/swagger-ui/",
                    "/v3/api-docs/**",
                    "/v3/api-docs",
                    "/swagger-resources/**",
                    "/webjars/**"
                ).permitAll()
                // Debug endpoints (temporaire pour diagnostiquer les problèmes d'authentification)
                .requestMatchers("/api/debug/**").permitAll()
                // MCP endpoints publics (appelés par le client MCP / IA)
                .requestMatchers("/mcp/**", "/actuator/**").permitAll()
                // Endpoints RH - utilise HR au lieu de RH, EMPLOYEE au lieu de ADMIN
                .requestMatchers("/api/employees/**").hasAnyRole("HR", "MANAGER", "EMPLOYEE")
                .requestMatchers("/api/departments/**").hasAnyRole("HR", "MANAGER")
                .requestMatchers("/api/teams/**").hasAnyRole("HR", "MANAGER")
                .requestMatchers("/api/dashboard/**").hasAnyRole("HR", "MANAGER", "EMPLOYEE")
                // Congés : tous les rôles authentifiés
                .requestMatchers("/api/leaves/**").authenticated()
                .anyRequest().authenticated()
            )
            .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
