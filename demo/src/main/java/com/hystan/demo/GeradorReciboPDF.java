package com.hystan.demo;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import java.awt.Color;
import java.io.*;
import java.util.Random;

public class GeradorReciboPDF {

    private static final String[] UNIDADES = {
        "", "um", "dois", "tres", "quatro", "cinco", "seis", "sete", "oito", "nove",
        "dez", "onze", "doze", "treze", "quatorze", "quinze", "dezesseis",
        "dezessete", "dezoito", "dezenove"
    };
    private static final String[] DEZENAS = {
        "", "", "vinte", "trinta", "quarenta", "cinquenta",
        "sessenta", "setenta", "oitenta", "noventa"
    };
    private static final String[] CENTENAS = {
        "", "cem", "duzentos", "trezentos", "quatrocentos", "quinhentos",
        "seiscentos", "setecentos", "oitocentos", "novecentos"
    };

    public static byte[] gerar(ReciboRequest req) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDFont fonteBold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont fonteNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream c = new PDPageContentStream(doc, pagina)) {

                float y = 780;
                String numRecibo = sanitize(
                    req.numeroRecibo != null && !req.numeroRecibo.isBlank()
                        ? req.numeroRecibo
                        : String.valueOf(new Random().nextInt(9000) + 1000),
                    20);

                // ── CABEÇALHO
                c.setNonStrokingColor(new Color(50, 50, 50));
                c.addRect(50, y - 10, 495, 36);
                c.fill();

                c.setNonStrokingColor(Color.WHITE);
                c.setFont(fonteBold, 18);
                escrever(c, 55, y + 6, "RECIBO", 20);

                c.setFont(fonteNormal, 11);
                escrever(c, 200, y + 6, "No " + numRecibo, 25);

                c.setFont(fonteNormal, 10);
                String dataStr = req.data != null && !req.data.isBlank()
                    ? sanitize(req.data, 20) : "";
                escrever(c, 400, y + 6, dataStr, 20);

                c.setNonStrokingColor(Color.BLACK);
                y -= 50;

                // ── VALOR EM DESTAQUE
                double valor = req.valor;
                String valorFormatado = String.format("R$ %.2f", valor).replace('.', ',');
                c.setFont(fonteBold, 22);
                escrever(c, 50, y, valorFormatado, 30);
                y -= 30;

                // ── CORPO PRINCIPAL
                String clienteNome    = sanitize(req.clienteNome, 60);
                String descricao      = sanitize(req.descricaoServico, 200);
                String extenso        = valorPorExtenso(valor);
                String prestadorNome  = sanitize(req.prestadorNome, 60);
                String prestadorDoc   = sanitize(req.prestadorCpfCnpj, 30);
                String cidade         = sanitize(req.cidade, 50);
                String data           = sanitize(req.data != null ? req.data : "", 20);

                c.setFont(fonteNormal, 11);
                String linha1 = "Recebi de " + clienteNome + " a quantia de " + valorFormatado;
                y = drawWrappedText(c, fonteNormal, 11, 50, y, 495, linha1);

                String linha2 = "(" + extenso + ")";
                y = drawWrappedText(c, fonteNormal, 11, 50, y, 495, linha2);
                y -= 4;

                String linha3 = "referente a: " + descricao;
                y = drawWrappedText(c, fonteNormal, 11, 50, y, 495, linha3);
                y -= 20;

                // ── SEPARADOR
                c.setStrokingColor(Color.LIGHT_GRAY);
                c.moveTo(50, y); c.lineTo(545, y); c.stroke();
                c.setStrokingColor(Color.BLACK);
                y -= 20;

                // ── DADOS DO PRESTADOR
                c.setNonStrokingColor(new Color(120, 120, 120));
                c.setFont(fonteBold, 8);
                escrever(c, 50, y, "PRESTADOR", 20);
                c.setNonStrokingColor(Color.BLACK);
                y -= 15;

                c.setFont(fonteBold, 12);
                escrever(c, 50, y, prestadorNome, 55);
                y -= 16;

                if (!prestadorDoc.isBlank()) {
                    c.setFont(fonteNormal, 10);
                    escrever(c, 50, y, "CPF/CNPJ: " + prestadorDoc, 50);
                    y -= 14;
                }
                y -= 16;

                // ── LOCAL E DATA
                c.setFont(fonteNormal, 10);
                String localData = cidade + (!cidade.isBlank() && !data.isBlank() ? ", " : "") + data;
                if (!localData.isBlank()) {
                    escrever(c, 50, y, sanitize(localData, 60), 60);
                    y -= 16;
                }

                y -= 40;

                // ── SEPARADOR DE ASSINATURA
                c.setStrokingColor(Color.BLACK);
                c.moveTo(50, y); c.lineTo(260, y); c.stroke();
                y -= 12;

                c.setFont(fonteNormal, 10);
                c.setNonStrokingColor(new Color(100, 100, 100));
                escrever(c, 50, y, prestadorNome, 40);
                y -= 12;
                escrever(c, 50, y, "Assinatura do Prestador", 30);
                c.setNonStrokingColor(Color.BLACK);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static float drawWrappedText(PDPageContentStream c, PDFont font, float size,
                                          float x, float y, float maxWidth,
                                          String text) throws Exception {
        if (text == null || text.isBlank()) return y;
        String clean = sanitize(text, 500);
        String[] words = clean.split("\\s+");
        StringBuilder line = new StringBuilder();

        for (String word : words) {
            if (word.isEmpty()) continue;
            String candidate = line.length() == 0 ? word : line + " " + word;
            float w;
            try {
                w = font.getStringWidth(candidate) / 1000f * size;
            } catch (Exception e) {
                w = maxWidth + 1;
            }
            if (w > maxWidth && line.length() > 0) {
                c.setFont(font, size);
                c.beginText();
                c.newLineAtOffset(x, y);
                c.showText(line.toString());
                c.endText();
                y -= size * 1.5f;
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) {
            c.setFont(font, size);
            c.beginText();
            c.newLineAtOffset(x, y);
            c.showText(line.toString());
            c.endText();
            y -= size * 1.5f;
        }
        return y;
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

    // ── VALOR POR EXTENSO (PT-BR)

    private static String valorPorExtenso(double valor) {
        long intPart = (long) valor;
        long cents = Math.round((valor - intPart) * 100);
        if (cents >= 100) { intPart++; cents = 0; }

        if (intPart == 0 && cents == 0) return "zero reais";

        StringBuilder sb = new StringBuilder();
        if (intPart > 0) {
            sb.append(porExtenso(intPart));
            sb.append(intPart == 1 ? " real" : " reais");
        }
        if (cents > 0) {
            if (intPart > 0) sb.append(" e ");
            sb.append(porExtenso(cents));
            sb.append(cents == 1 ? " centavo" : " centavos");
        }
        return sb.toString();
    }

    private static String porExtenso(long n) {
        if (n == 0) return "zero";
        StringBuilder sb = new StringBuilder();

        if (n >= 1_000_000) {
            long m = n / 1_000_000;
            sb.append(porExtenso(m)).append(m == 1 ? " milhao" : " milhoes");
            n %= 1_000_000;
            if (n > 0) sb.append(n < 100 ? " e " : ", ");
        }
        if (n >= 1_000) {
            long m = n / 1_000;
            if (m == 1) sb.append("mil");
            else sb.append(porExtenso(m)).append(" mil");
            n %= 1_000;
            if (n > 0) sb.append(n < 100 ? " e " : ", ");
        }
        if (n >= 100) {
            long cent = n / 100;
            if (n % 100 == 0) {
                sb.append(CENTENAS[(int) cent]);
            } else {
                sb.append(cent == 1 ? "cento" : CENTENAS[(int) cent]);
                sb.append(" e ");
            }
            n %= 100;
        }
        if (n >= 20) {
            sb.append(DEZENAS[(int) (n / 10)]);
            if (n % 10 > 0) sb.append(" e ").append(UNIDADES[(int) (n % 10)]);
        } else if (n > 0) {
            sb.append(UNIDADES[(int) n]);
        }
        return sb.toString();
    }
}
