package com.conectatech.sgs_backend.security;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableWebSecurity
@RequiredArgsConstructor
public class SecurityConfig {

        private final JwtAuthenticationFilter jwtAuthFilter;
        private final AuthenticationProvider authenticationProvider;

        @Bean
        public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
                http
                                .csrf(AbstractHttpConfigurer::disable)
                                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                                .authorizeHttpRequests(auth -> auth
                                                // Endpoint Sudo (Requiere autenticación previa)
                                                .requestMatchers("/api/v1/auth/sudo").authenticated()

                                                // Rutas públicas: Autenticación, Swagger y ruta interna de errores de
                                                // Spring
                                                .requestMatchers(
                                                                "/api/v1/auth/login",
                                                                "/api/v1/auth/register",
                                                                "/api/v1/auth/**",
                                                                "/api/v1/health",
                                                                "/error",
                                                                "/v3/api-docs/**",
                                                                "/scalar.html")
                                                .permitAll()

                                                // Módulo de Administración
                                                .requestMatchers("/api/v1/usuarios/**", "/api/v1/catalogos/**")
                                                .hasRole("ADMINISTRADOR")

                                                // Módulo de Reportes
                                                .requestMatchers("/api/v1/reportes/**").hasRole("ADMINISTRADOR")

                                                // Módulo de Incidentes
                                                .requestMatchers(HttpMethod.POST, "/api/v1/incidentes",
                                                                "/api/v1/incidentes/**")
                                                .hasAnyRole("ADMINISTRADOR", "OPERADOR", "CIUDADANO")
                                                .requestMatchers(HttpMethod.PATCH, "/api/v1/incidentes",
                                                                "/api/v1/incidentes/**")
                                                .hasAnyRole("ADMINISTRADOR", "OPERADOR")
                                                .requestMatchers(HttpMethod.DELETE, "/api/v1/incidentes",
                                                                "/api/v1/incidentes/**")
                                                .hasRole("ADMINISTRADOR")
                                                .requestMatchers(HttpMethod.GET, "/api/v1/incidentes",
                                                                "/api/v1/incidentes/**")
                                                .hasAnyRole("ADMINISTRADOR", "OPERADOR", "INSPECTOR")

                                                // Cualquier otra ruta exige token válido
                                                .anyRequest().authenticated())
                                .sessionManagement(session -> session
                                                .sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                                .authenticationProvider(authenticationProvider)
                                .addFilterBefore(jwtAuthFilter, UsernamePasswordAuthenticationFilter.class);

                return http.build();
        }

        @Bean
        public CorsConfigurationSource corsConfigurationSource() {
                CorsConfiguration config = new CorsConfiguration();

                // Permitir tanto desarrollo local como producción en Vercel
                config.setAllowedOrigins(List.of(
                                "http://localhost:5173",
                                "https://sgs-el-tabo-frontend.vercel.app"));
                config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
                config.setAllowedHeaders(List.of("*"));
                config.setAllowCredentials(true);

                UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
                source.registerCorsConfiguration("/**", config);
                return source;
        }
}