package com.hystan.demo;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.awt.Color;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GeradorOrcamentoPDF {

    public static byte[] gerar(OrcamentoRequest req) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDFont fonteBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {

                float y = 780;
                int numOrc = new Random().nextInt(9000) + 1000;
                String dataHoje = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                // ── CABEÇALHO
                c.setNonStrokingColor(new Color(50, 50, 50));
                c.addRect(50, y - 10, 495, 36);
                c.fill();

                c.setNonStrokingColor(Color.WHITE);
                c.setFont(fonteBold, 15);
                escrever(c, 55, y + 8, "ORCAMENTO No " + numOrc, 50);

                c.setFont(fonteNormal, 10);
                escrever(c, 400, y + 8, "Emissao: " + dataHoje, 30);

                c.setNonStrokingColor(Color.BLACK);
                y -= 44;

                // ── PRESTADOR
                c.setNonStrokingColor(new Color(120, 120, 120));
                c.setFont(fonteBold, 8);
                escrever(c, 50, y, "PRESTADOR", 20);
                c.setNonStrokingColor(Color.BLACK);
                y -= 15;

                c.setFont(fonteBold, 12);
                escrever(c, 50, y, sanitize(req.prestadorNome, 55), 55);
                y -= 17;

                c.setFont(fonteNormal, 10);
                if (req.prestadorCnpj != null && !req.prestadorCnpj.isBlank()) {
                    escrever(c, 50, y, "CNPJ/CPF: " + sanitize(req.prestadorCnpj, 30), 50);
                    y -= 14;
                }
                if (req.prestadorTelefone != null && !req.prestadorTelefone.isBlank()) {
                    escrever(c, 50, y, "Tel: " + sanitize(req.prestadorTelefone, 30), 40);
                    y -= 14;
                }
                y -= 14;

                // ── CLIENTE
                c.setNonStrokingColor(new Color(120, 120, 120));
                c.setFont(fonteBold, 8);
                escrever(c, 50, y, "CLIENTE", 20);
                c.setNonStrokingColor(Color.BLACK);
                y -= 15;

                c.setFont(fonteBold, 12);
                escrever(c, 50, y, sanitize(req.clienteNome, 55), 55);
                y -= 17;

                if (req.clienteCnpj != null && !req.clienteCnpj.isBlank()) {
                    c.setFont(fonteNormal, 10);
                    escrever(c, 50, y, "CNPJ/CPF: " + sanitize(req.clienteCnpj, 30), 50);
                    y -= 14;
                }
                y -= 18;

                // ── SEPARADOR
                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y); c.lineTo(545, y); c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 18;

                // ── CABEÇALHO DA TABELA
                float rowH = 20f;
                c.setNonStrokingColor(new Color(50, 50, 50));
                c.addRect(50, y - 5, 495, rowH);
                c.fill();

                c.setNonStrokingColor(Color.WHITE);
                c.setFont(fonteBold, 10);
                escrever(c, 55,  y + 3, "DESCRICAO", 30);
                escrever(c, 275, y + 3, "QTD", 8);
                escrever(c, 325, y + 3, "VALOR UNIT.", 18);
                escrever(c, 460, y + 3, "TOTAL", 12);
                c.setNonStrokingColor(Color.BLACK);
                y -= 25;

                // ── LINHAS DOS ITENS
                c.setFont(fonteNormal, 10);
                double subtotal = 0;
                Color zebraColor = new Color(240, 240, 240);

                List<OrcamentoRequest.ItemOrcamento> itens =
                    req.itens != null ? req.itens : Collections.emptyList();

                for (int i = 0; i < itens.size(); i++) {
                    OrcamentoRequest.ItemOrcamento item = itens.get(i);
                    double total = item.quantidade * item.valorUnitario;
                    subtotal += total;

                    if (i % 2 == 0) {
                        c.setNonStrokingColor(zebraColor);
                        c.addRect(50, y - 5, 495, rowH);
                        c.fill();
                    }
                    c.setNonStrokingColor(Color.BLACK);
                    escrever(c, 55,  y + 2, sanitize(item.descricao, 38), 38);
                    escrever(c, 275, y + 2, String.valueOf(item.quantidade), 8);
                    escrever(c, 325, y + 2,
                        String.format("R$ %.2f", item.valorUnitario).replace('.', ','), 18);
                    escrever(c, 460, y + 2,
                        String.format("R$ %.2f", total).replace('.', ','), 15);
                    y -= 22;
                }

                // ── SEPARADOR PÓS-TABELA
                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y); c.lineTo(545, y); c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 18;

                // ── TOTAL
                c.setFont(fonteBold, 12);
                escrever(c, 370, y, "TOTAL:", 15);
                escrever(c, 460, y,
                    String.format("R$ %.2f", subtotal).replace('.', ','), 18);
                y -= 30;

                // ── SEPARADOR
                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y); c.lineTo(545, y); c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 18;

                // ── RODAPÉ: VALIDADE E OBSERVAÇÕES
                c.setFont(fonteNormal, 10);
                if (req.validadeDias != null && !req.validadeDias.isBlank()) {
                    escrever(c, 50, y,
                        "Validade da proposta: " + sanitize(req.validadeDias, 10) + " dias", 55);
                    y -= 16;
                }

                if (req.observacoes != null && !req.observacoes.isBlank()) {
                    c.setNonStrokingColor(new Color(120, 120, 120));
                    c.setFont(fonteBold, 8);
                    escrever(c, 50, y, "OBSERVACOES", 20);
                    c.setNonStrokingColor(Color.BLACK);
                    y -= 14;
                    c.setFont(fonteNormal, 10);
                    escrever(c, 50, y, sanitize(req.observacoes, 120), 120);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static void escrever(PDPageContentStream c, float x, float y,
                                   String texto, int maxLen) throws Exception {
        c.beginText();
        c.newLineAtOffset(x, y);
        c.showText(sanitize(texto, maxLen));
        c.endText();
    }

    private static String sanitize(String input, int maxLen) {
        if (input == null) return "";
        String s = input.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
