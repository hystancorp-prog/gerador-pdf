package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

@RestController
public class ReciboController {

    @Autowired
    private RateLimiter rateLimiter;

    @PostMapping("/gerar-recibo")
    public ResponseEntity<byte[]> gerarRecibo(
            @RequestBody ReciboRequest req,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(
                RateLimiter.getClientIp(request) + ":recibo", 10, 60_000L))
            return ResponseEntity.status(429)
                .body("Muitas requisicoes. Tente novamente em instantes.".getBytes());

        try {
            byte[] pdf = GeradorReciboPDF.gerar(req);
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename("recibo.pdf").build());
            return ResponseEntity.ok().headers(headers).body(pdf);
        } catch (Exception e) {
            return ResponseEntity.status(500).body("Erro interno.".getBytes());
        }
    }
}
