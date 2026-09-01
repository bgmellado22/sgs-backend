package com.conectatech.sgs_backend.middleware;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

@Component
public class RequestLoggingFilter extends OncePerRequestFilter {

    private static final Logger logger = LoggerFactory.getLogger(RequestLoggingFilter.class);

    @Override
    protected void doFilterInternal(
            @NonNull HttpServletRequest request,
            @NonNull HttpServletResponse response,
            @NonNull FilterChain filterChain) throws ServletException, IOException {

        long startTime = System.currentTimeMillis();

        // Registramos la petición entrante
        logger.info("Petición entrante: {} {}", request.getMethod(), request.getRequestURI());

        // Continuamos con el flujo normal
        filterChain.doFilter(request, response);

        // Registramos la respuesta y el tiempo de ejecución
        long duration = System.currentTimeMillis() - startTime;
        logger.info("Respuesta generada: Estado {} | Tiempo: {} ms", response.getStatus(), duration);
    }
}