package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReciboController {

    @Autowired private RateLimiter  rateLimiter;
    @Autowired private PlanoService planoService;

    @PostMapping("/gerar-recibo")
    public ResponseEntity<byte[]> gerarRecibo(
            @RequestBody ReciboRequest req,
            Authentication auth,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":recibo", 10, 60_000L))
            return ResponseEntity.status(429).body("Muitas requisicoes. Tente novamente em instantes.".getBytes());

        if (auth == null)
            return ResponseEntity.status(401).body("Não autenticado.".getBytes());
        String uid = ((Jwt) auth.getPrincipal()).getSubject();

        if (!planoService.podeGerarDocumento(uid))
            return ResponseEntity.status(403)
                .body("Limite do plano atingido. Faça upgrade para continuar.".getBytes());

        if (req.valor < 0 || !Double.isFinite(req.valor) || req.valor > 9_999_999.99)
            return ResponseEntity.status(400).body("Valor inválido.".getBytes());

        if (!planoService.podeUsarLogo(uid))
            req.logoBase64 = null;

        try {
            byte[] pdf = GeradorReciboPDF.gerar(req);

            planoService.incrementarContador(uid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("recibo.pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro interno.".getBytes());
        }
    }
}
