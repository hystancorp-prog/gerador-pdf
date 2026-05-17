package com.hystan.demo;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.Color;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class GeradorPDF {

    public static void gerar(List<String[]> dados, String saida,
                             String nomeEmpresa, String caminhoLogo) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina  = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDFont fonteBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {

                boolean temLogo = caminhoLogo != null
                               && !caminhoLogo.isEmpty()
                               && new File(caminhoLogo).exists();

                float y = 780;

                if (temLogo) {
                    PDImageXObject logo = PDImageXObject.createFromFile(caminhoLogo, doc);
                    c.drawImage(logo, 50, y - 10, 60, 60);
                }

                float xNome = temLogo ? 120 : 50;
                c.beginText();
                c.setFont(fonteBold, 20);
                c.newLineAtOffset(xNome, y);
                c.showText(sanitize(nomeEmpresa, 80));
                c.endText();

                c.beginText();
                c.setFont(fonteNormal, 11);
                c.newLineAtOffset(xNome, y - 20);
                c.showText("Relatorio de Atendimentos");
                c.endText();

                String dataHoje = LocalDate.now()
                    .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                c.beginText();
                c.setFont(fonteNormal, 10);
                c.newLineAtOffset(xNome, y - 35);
                c.showText("Gerado em: " + dataHoje);
                c.endText();

                y = 720;
                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y);
                c.lineTo(545, y);
                c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 20;

                float alturaLinha = 20;
                c.setNonStrokingColor(new Color(50, 50, 50));
                c.addRect(50, y - 5, 495, alturaLinha);
                c.fill();

                c.setNonStrokingColor(Color.WHITE);
                c.setFont(fonteBold, 11);
                escrever(c, 55,  y + 2, "Nome", 60);
                escrever(c, 255, y + 2, "Servico", 60);
                escrever(c, 420, y + 2, "Data", 20);
                escrever(c, 490, y + 2, "Valor", 20);
                c.setNonStrokingColor(Color.BLACK);

                y -= 25;

                c.setFont(fonteNormal, 11);
                double total = 0;
                Color cinzaClaro = new Color(240, 240, 240);

                for (int i = 0; i < dados.size(); i++) {
                    String[] linha = dados.get(i);

                    if (i % 2 == 0) {
                        c.setNonStrokingColor(cinzaClaro);
                        c.addRect(50, y - 5, 495, alturaLinha);
                        c.fill();
                    }

                    c.setNonStrokingColor(Color.BLACK);
                    escrever(c, 55,  y + 2, linha[0], 50);
                    escrever(c, 255, y + 2, linha[1], 50);
                    escrever(c, 420, y + 2, linha[3], 20);
                    escrever(c, 490, y + 2, "R$" + linha[2], 20);

                    total += Double.parseDouble(linha[2].replace(",", "."));
                    y -= 22;
                }

                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y);
                c.lineTo(545, y);
                c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 20;

                c.setFont(fonteBold, 12);
                escrever(c, 380, y, "Total:", 20);
                escrever(c, 490, y, String.format("R$%.2f", total), 20);
            }
            doc.save(saida);
        }
    }

    private static void escrever(PDPageContentStream c, float x, float y, String texto, int maxLen)
            throws Exception {
        c.beginText();
        c.newLineAtOffset(x, y);
        c.showText(sanitize(texto, maxLen));
        c.endText();
    }

    // Strip control characters and truncate — prevents layout breaks and encoding issues
    private static String sanitize(String input, int maxLen) {
        if (input == null) return "";
        String s = input.replaceAll("[\\x00-\\x1F\\x7F]", "").trim();
        return s.length() > maxLen ? s.substring(0, maxLen) : s;
    }
}
