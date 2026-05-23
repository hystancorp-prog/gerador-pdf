package com.hystan.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;
import java.util.Optional;

@RestController
public class UserSyncController {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @PostMapping("/sync-user")
    public ResponseEntity<Void> syncUser(
            org.springframework.security.core.Authentication auth,
            @RequestBody Map<String, String> body) {

        if (auth == null) return ResponseEntity.status(401).build();

        String uid = auth.getName();
        String email = ((org.springframework.security.oauth2.jwt.Jwt) auth.getPrincipal()).getClaimAsString("email");
        String nome = body.get("nome");
        String foto = body.get("foto");

        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(uid);

        if (opt.isEmpty()) {
            // Primeiro login — inicia com trial
            Usuario novo = new Usuario(email, nome, foto);
            novo.setFirebaseUid(uid);
            novo.setPlano("trial");
            novo.setPlanoStatus("trial");
            novo.setDocsTotalTrial(0);
            novo.setDocsMesAtual(0);
            usuarioRepository.save(novo);
        } else {
            // Atualiza dados básicos se mudaram
            Usuario u = opt.get();
            if (nome != null) u.setNome(nome);
            if (foto != null) u.setFoto(foto);
            if (u.getFirebaseUid() == null) u.setFirebaseUid(uid);
            usuarioRepository.save(u);
        }

        return ResponseEntity.ok().build();
    }
}
