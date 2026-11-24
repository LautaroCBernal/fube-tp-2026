package com.tiendaonline.inventario.config;

import java.io.IOException;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

// No genera el traceId (eso lo hace Micrometer Tracing al entrar la request): solo lo copia al header
// X-Trace-Id de la respuesta, para que quien llama pueda reportarlo y buscarlo despues en los logs.
@Component
public class TraceIdResponseFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        // Este filtro corre despues del de Micrometer, asi que el traceId ya esta en el MDC de este thread
        String traceId = MDC.get("traceId");
        if (traceId != null) {
            response.setHeader("X-Trace-Id", traceId);
        }

        chain.doFilter(request, response);
    }
}
