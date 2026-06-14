package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class UserSyncController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Autowired
    private RateLimiter rateLimiter;

    @PostMapping("/sync-user")
    public ResponseEntity<?> syncUser(
            org.springframework.security.core.Authentication auth,
            @RequestBody Map<String, String> body,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":sync-user", 10, 60_000L))
            return ResponseEntity.status(429).body(Map.of("error", "Muitas requisições. Tente novamente em instantes."));

        if (auth == null) return ResponseEntity.status(401).build();

        String uid = auth.getName();
        Jwt jwt = (Jwt) auth.getPrincipal();
        String email = jwt.getClaimAsString("email");
        String nome = body.get("nome");
        String foto = body.get("foto");

        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(uid);
        boolean verificado = isEmailVerificado(jwt);

        if (opt.isEmpty()) {
            if (isEmailInvalido(email)) {
                return ResponseEntity.status(400).body(Map.of("error", "Email inválido"));
            }
            // Primeiro login — usuário começa no plano gratuito e escolhe um
            // plano em /planos.html. O trial só é concedido pelo Stripe (com cartão).
            Usuario novo = new Usuario(email, nome, foto);
            novo.setFirebaseUid(uid);
            novo.setPlano("gratuito");
            novo.setPlanoStatus("inativo");
            novo.setDocsTotalTrial(0);
            novo.setDocsMesAtual(0);
            usuarioRepository.save(novo);
            emailService.enviarBoasVindas(email, nome);
        } else {
            // Atualiza dados básicos se mudaram
            Usuario u = opt.get();
            if (nome != null) u.setNome(nome);
            if (foto != null) u.setFoto(foto);
            if (u.getFirebaseUid() == null) u.setFirebaseUid(uid);

            // Se estava pendente e agora o email está verificado, ativa o trial
            if ("pendente_verificacao".equals(u.getPlanoStatus()) && verificado) {
                u.setPlanoStatus("trial");
                u.setTrialFim(java.time.LocalDateTime.now().plusDays(7));
                u.setDocsTotalTrial(0);
            }

            usuarioRepository.save(u);
        }

        return ResponseEntity.ok().build();
    }

    /**
     * Camada extra de validação no backend. O Firebase Auth já garante que o e-mail
     * existe de fato, mas aqui bloqueamos domínios e padrões claramente de teste/descartáveis
     * antes de criar uma conta nova.
     */
    private boolean isEmailInvalido(String email) {
        if (email == null || !email.contains("@")) return true;

        String dominio = email.substring(email.indexOf('@') + 1).toLowerCase();
        if (!dominio.contains(".")) return true;

        if (email.toLowerCase().matches(".*\\b(teste|test|fake|temp|dummy)\\b.*")) return true;

        return dominio.equals("teste.com")
            || dominio.equals("test.com")
            || dominio.equals("fake.com")
            || dominio.equals("tempmail.com")
            || dominio.equals("mailinator.com")
            || dominio.equals("guerrillamail.com");
    }

    /**
     * Email verificado pelo Firebase, ou login via provider Google
     * (que já garante o email verificado).
     */
    private boolean isEmailVerificado(Jwt jwt) {
        Boolean emailVerified = jwt.getClaimAsBoolean("email_verified");

        String provider = "";
        Object firebaseClaim = jwt.getClaim("firebase");
        if (firebaseClaim instanceof Map) {
            Object p = ((Map<?, ?>) firebaseClaim).get("sign_in_provider");
            if (p != null) provider = p.toString();
        }

        return Boolean.TRUE.equals(emailVerified) || "google.com".equals(provider);
    }
}
