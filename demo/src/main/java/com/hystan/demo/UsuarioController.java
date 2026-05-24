package com.hystan.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
public class UsuarioController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @GetMapping("/meu-plano")
    public ResponseEntity<Map<String, String>> meuPlano(
            org.springframework.security.core.Authentication auth) {

        if (auth == null) return ResponseEntity.status(401).build();

        String uid = auth.getName(); // Firebase UID vem do JWT subject

        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(uid);

        Map<String, String> resp = new HashMap<>();

        if (opt.isEmpty()) {
            resp.put("plano", "gratuito");
            resp.put("status", "inativo");
            return ResponseEntity.ok(resp);
        }

        Usuario u = opt.get();
        resp.put("plano",         u.getPlano()       != null ? u.getPlano()       : "gratuito");
        resp.put("status",        u.getPlanoStatus() != null ? u.getPlanoStatus() : "inativo");
        resp.put("docsMesAtual",  String.valueOf(u.getDocsMesAtual()   != null ? u.getDocsMesAtual()   : 0));
        resp.put("docsTotalTrial",String.valueOf(u.getDocsTotalTrial() != null ? u.getDocsTotalTrial() : 0));
        if (u.getTrialFim() != null) {
            resp.put("trialFim", u.getTrialFim().toString());
        } else {
            if (u.getCriadoEm() != null) {
                resp.put("trialFim", u.getCriadoEm().plusDays(7).toString());
            } else {
                resp.put("trialFim", java.time.LocalDateTime.now().plusDays(7).toString());
            }
        }
        return ResponseEntity.ok(resp);
    }
}
