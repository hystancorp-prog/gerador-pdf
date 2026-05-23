package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDateTime;
import java.util.*;

@RestController
public class EmpresaController {

    @Autowired
    private EmpresaRepository empresaRepo;

    @Autowired
    private RateLimiter rateLimiter;

    @GetMapping("/empresas")
    public ResponseEntity<?> listar(Authentication auth, HttpServletRequest request) {
        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":empresas-get", 30, 60_000L))
            return ResponseEntity.status(429).body("Muitas requisições.");
        return ResponseEntity.ok(empresaRepo.findByFirebaseUid(auth.getName()));
    }

    @PostMapping("/empresas")
    public ResponseEntity<?> criar(@RequestBody Empresa req,
                                    Authentication auth,
                                    HttpServletRequest request) {
        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":empresas-post", 10, 60_000L))
            return ResponseEntity.status(429).body("Muitas requisições.");
        // Limit base64 logo to ~3MB decoded (4MB base64) to prevent DB abuse / OOM
        if (req.getLogoBase64() != null && req.getLogoBase64().length() > 4 * 1024 * 1024)
            return ResponseEntity.status(413).body("Logo muito grande. Máximo 2MB.");
        req.setId(null);
        req.setFirebaseUid(auth.getName());
        req.setCriadoEm(LocalDateTime.now());
        return ResponseEntity.status(201).body(empresaRepo.save(req));
    }

    @DeleteMapping("/empresas/{id}")
    public ResponseEntity<?> deletar(@PathVariable Long id,
                                      Authentication auth,
                                      HttpServletRequest request) {
        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":empresas-del", 10, 60_000L))
            return ResponseEntity.status(429).body("Muitas requisições.");
        if (!empresaRepo.findByIdAndFirebaseUid(id, auth.getName()).isPresent())
            return ResponseEntity.notFound().build();
        empresaRepo.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
