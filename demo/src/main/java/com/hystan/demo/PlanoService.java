package com.hystan.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.Optional;

@Service
public class PlanoService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    public boolean podeGerarDocumento(String firebaseUid) {
        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(firebaseUid);
        if (opt.isEmpty()) return false;

        Usuario u = opt.get();
        String plano  = u.getPlano()  != null ? u.getPlano()      : "gratuito";
        String status = u.getPlanoStatus() != null ? u.getPlanoStatus() : "inativo";

        if (!status.equals("ativo") && !status.equals("trial")) return false;

        if (status.equals("trial")) {
            int usado = u.getDocsTotalTrial() != null ? u.getDocsTotalTrial() : 0;
            return usado < 5;
        }

        if (plano.equals("basico") || plano.equals("essencial")) {
            int mesAtual = LocalDate.now().getMonthValue();
            if (u.getMesContagem() == null || u.getMesContagem() != mesAtual) {
                u.setDocsMesAtual(0);
                u.setMesContagem(mesAtual);
                usuarioRepository.save(u);
            }
            int usado = u.getDocsMesAtual() != null ? u.getDocsMesAtual() : 0;
            return usado < 20;
        }

        // pro / business — ilimitado
        return true;
    }

    public boolean podeUsarLogo(String firebaseUid) {
        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(firebaseUid);
        if (opt.isEmpty()) return false;
        Usuario u = opt.get();
        String plano = u.getPlano() != null ? u.getPlano() : "gratuito";
        return plano.equals("pro") || plano.equals("business");
    }

    public void incrementarContador(String firebaseUid) {
        Optional<Usuario> opt = usuarioRepository.findByFirebaseUid(firebaseUid);
        if (opt.isEmpty()) return;

        Usuario u = opt.get();

        if ("trial".equals(u.getPlanoStatus())) {
            int atual = u.getDocsTotalTrial() != null ? u.getDocsTotalTrial() : 0;
            u.setDocsTotalTrial(atual + 1);
        }

        int mesAtual = LocalDate.now().getMonthValue();
        if (u.getMesContagem() == null || u.getMesContagem() != mesAtual) {
            u.setDocsMesAtual(0);
            u.setMesContagem(mesAtual);
        }
        int mensal = u.getDocsMesAtual() != null ? u.getDocsMesAtual() : 0;
        u.setDocsMesAtual(mensal + 1);

        usuarioRepository.save(u);
    }
}
