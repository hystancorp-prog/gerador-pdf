package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.*;

@RestController
public class OrcamentoController {

    @Autowired private RateLimiter     rateLimiter;
    @Autowired private PlanoService    planoService;
    @Autowired private ContadorService contadorService;

    @PostMapping("/gerar-orcamento")
    public ResponseEntity<byte[]> gerarOrcamento(
            @RequestBody OrcamentoRequest req,
            Authentication auth,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(RateLimiter.getClientIp(request) + ":orcamento", 10, 60_000L))
            return ResponseEntity.status(429).body("Muitas requisicoes. Tente novamente em instantes.".getBytes());

        if (auth == null)
            return ResponseEntity.status(401).body("Não autenticado.".getBytes());
        String uid = ((Jwt) auth.getPrincipal()).getSubject();

        if (!planoService.podeGerarDocumento(uid))
            return ResponseEntity.status(403)
                .body("Limite do plano atingido. Faça upgrade para continuar.".getBytes());

        if (!planoService.podeUsarLogo(uid))
            req.logoBase64 = null;

        if (req.itens != null) {
            for (OrcamentoRequest.ItemOrcamento item : req.itens) {
                if (item.quantidade <= 0) item.quantidade = 1;
                if (item.valorUnitario < 0) item.valorUnitario = 0;
            }
        }

        try {
            int numDoc = contadorService.proximoNumero(uid, "orcamento");
            byte[] pdf = GeradorOrcamentoPDF.gerar(req, numDoc);

            planoService.incrementarContador(uid);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(ContentDisposition.attachment().filename("orcamento.pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro interno.".getBytes());
        }
    }
}
