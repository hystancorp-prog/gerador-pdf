package com.hystan.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TrialExpirationService {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private EmailService emailService;

    @Scheduled(cron = "0 0 8 * * *")
    public void verificarTrialsExpirados() {
        List<Usuario> expirados = usuarioRepository
            .findByPlanoStatusAndTrialFimBefore("trial", LocalDateTime.now());

        for (Usuario u : expirados) {
            u.setPlanoStatus("expirado");
            usuarioRepository.save(u);
            int docs = u.getDocsTotalTrial() != null ? u.getDocsTotalTrial() : 0;
            emailService.enviarTrialExpirado(u.getEmail(), u.getNome(), docs);
        }
    }
}
