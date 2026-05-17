package com.hystan.demo;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.*;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;

@RestController
public class PdfController {

    private static final long MAX_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final byte[] XLSX_MAGIC = {0x50, 0x4B, 0x03, 0x04};
    private static final int   RATE_LIMIT  = 10;           // requests per window
    private static final long  WINDOW_MS   = 60_000L;      // 1 minute

    @Autowired
    private RateLimiter rateLimiter;

    @GetMapping("/app")
    public ResponseEntity<Void> app() {
        return ResponseEntity.status(302)
            .header("Location", "/dashboard.html")
            .build();
    }

    @PostMapping("/gerar-pdf")
    public ResponseEntity<byte[]> gerarPdf(
            @RequestParam("planilha") MultipartFile file,
            @RequestParam("empresa") String nomeEmpresa,
            @RequestParam("ano") int ano,
            @RequestParam("mesInicio") int mesInicio,
            @RequestParam("mesFim") int mesFim,
            HttpServletRequest request) {

        if (!rateLimiter.isAllowed(request.getRemoteAddr() + ":gerar-pdf", RATE_LIMIT, WINDOW_MS)) {
            return ResponseEntity.status(429)
                .body("Muitas requisições. Tente novamente em instantes.".getBytes());
        }

        // 1. Size check — cheapest, before any I/O
        if (file.getSize() > MAX_SIZE) {
            return ResponseEntity.status(413)
                .body("Arquivo excede o limite de 5MB.".getBytes());
        }

        // 2. Filename sanity — prevent null/blank/path-traversal names
        String originalName = file.getOriginalFilename();
        if (originalName == null
                || Paths.get(originalName).getFileName().toString().isBlank()) {
            return ResponseEntity.status(400)
                .body("Nome de arquivo inválido.".getBytes());
        }

        File temp = null;
        File pdfTemp = null;
        try {
            temp = File.createTempFile("planilha", ".xlsx");
            file.transferTo(Objects.requireNonNull(temp));

            // 3. Magic bytes check — never trust extension or Content-Type alone
            try (FileInputStream fis = new FileInputStream(temp)) {
                byte[] header = new byte[4];
                if (fis.read(header) < 4
                        || header[0] != XLSX_MAGIC[0]
                        || header[1] != XLSX_MAGIC[1]
                        || header[2] != XLSX_MAGIC[2]
                        || header[3] != XLSX_MAGIC[3]) {
                    return ResponseEntity.status(415)
                        .body("Tipo de arquivo inválido. Envie um .xlsx válido.".getBytes());
                }
            }

            pdfTemp = File.createTempFile("relatorio", ".pdf");
            String saida = pdfTemp.getAbsolutePath();

            List<String[]> dados = LeitorPlanilha.ler(
                temp.getAbsolutePath(), mesInicio, mesFim, ano);

            if (dados.isEmpty()) {
                return ResponseEntity.status(400)
                    .body("Nenhum registro encontrado para o período selecionado.".getBytes());
            }

            GeradorPDF.gerar(dados, saida, nomeEmpresa, "");

            byte[] pdfBytes = java.nio.file.Files.readAllBytes(pdfTemp.toPath());

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment().filename("relatorio.pdf").build());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            return ResponseEntity.status(500)
                .body("Erro interno. Tente novamente.".getBytes());
        } finally {
            // Always clean up — even if an exception occurred mid-processing
            if (temp != null) temp.delete();
            if (pdfTemp != null) pdfTemp.delete();
        }
    }
}
