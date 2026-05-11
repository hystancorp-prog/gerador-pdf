package com.hystan.demo;

import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.*;
import java.util.List;

@Controller
public class PdfController {

    @GetMapping("/")
    public String index() {
        return "index";
    }

    @PostMapping("/gerar-pdf")
    public ResponseEntity<byte[]> gerarPdf(
            @RequestParam("planilha") MultipartFile file,
            @RequestParam("empresa") String nomeEmpresa,
            @RequestParam("ano") int ano,
            @RequestParam("mesInicio") int mesInicio,
            @RequestParam("mesFim") int mesFim) {
        try {
            File temp = File.createTempFile("planilha", ".xlsx");
            file.transferTo(temp);

            File pdfTemp = File.createTempFile("relatorio", ".pdf");
            String saida = pdfTemp.getAbsolutePath();

            List<String[]> dados = LeitorPlanilha.ler(
                temp.getAbsolutePath(), mesInicio, mesFim, ano);

            if (dados.isEmpty()) {
                temp.delete();
                pdfTemp.delete();
                return ResponseEntity
                    .status(400)
                    .body("Nenhum registro encontrado para o período selecionado.".getBytes());
            }

            GeradorPDF.gerar(dados, saida, nomeEmpresa, "");

            byte[] pdfBytes = java.nio.file.Files.readAllBytes(pdfTemp.toPath());

            temp.delete();
            pdfTemp.delete();

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_PDF);
            headers.setContentDisposition(
                ContentDisposition.attachment()
                    .filename("relatorio.pdf").build());

            return ResponseEntity.ok().headers(headers).body(pdfBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).build();
        }
    }
}