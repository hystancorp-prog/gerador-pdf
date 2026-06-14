package com.hystan.demo;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.Set;

@Component
public class ContentLengthFilter extends OncePerRequestFilter {

    private static final long MAX = 1_000_000; // 1MB
    private static final Set<String> PROTEGIDOS = Set.of(
        "/gerar-orcamento", "/gerar-recibo", "/sync-user", "/empresas"
    );

    @Override
    protected void doFilterInternal(HttpServletRequest req, HttpServletResponse res, FilterChain chain)
            throws ServletException, IOException {
        String uri = req.getRequestURI();
        if (PROTEGIDOS.contains(uri) && req.getContentLengthLong() > MAX) {
            res.setStatus(413); // Payload Too Large
            res.getWriter().write("Requisição muito grande.");
            return;
        }
        chain.doFilter(req, res);
    }
}
