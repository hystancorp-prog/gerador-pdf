package com.hystan.demo;

import org.apache.pdfbox.pdmodel.*;
import org.apache.pdfbox.pdmodel.font.*;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;
import java.awt.Color;
import java.awt.image.BufferedImage;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Random;
import javax.imageio.ImageIO;

public class GeradorReciboPDF {

    private static final String FOOTER = "Gerado com Hystan · hystancorp.up.railway.app";

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

            PDFont bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);
            PDFont italic = new PDType1Font(Standard14Fonts.FontName.HELVETICA_OBLIQUE);

            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                float y = 790f;

                String numRecibo = san(
                    ok(req.numeroRecibo) ? req.numeroRecibo
                                        : String.valueOf(new Random().nextInt(9000) + 1000), 20);

                /* data fornecida pelo frontend ou hoje */
                String dataFormatada = "";
                if (ok(req.data)) {
                    try {
                        /* aceita ISO (yyyy-MM-dd) e converte para dd/MM/yyyy */
                        java.time.LocalDate ld = java.time.LocalDate.parse(req.data);
                        dataFormatada = ld.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                    } catch (Exception ex) {
                        dataFormatada = san(req.data, 20);
                    }
                }

                /* ── HEADER ── */
                float headerH   = 56f;
                float headerBot = y - headerH;
                cs.setNonStrokingColor(new Color(26, 26, 26));
                cs.addRect(50, headerBot, 495, headerH);
                cs.fill();

                PDImageXObject logo = loadLogo(doc, req.logoBase64);
                float textX = 60f;
                if (logo != null) {
                    float lH = Math.min(36f, logo.getHeight());
                    float lW = lH * ((float) logo.getWidth() / logo.getHeight());
                    cs.drawImage(logo, 60, headerBot + (headerH - lH) / 2f, lW, lH);
                    textX = 60 + lW + 10;
                }

                float textY = headerBot + headerH / 2f - 4f;
                cs.setNonStrokingColor(Color.WHITE);
                txt(cs, bold, 13, textX, textY, "RECIBO");
                float reciboW = tw(bold, 13, "RECIBO");
                txt(cs, normal, 11, textX + reciboW + 5, textY, "No " + numRecibo);
                if (!dataFormatada.isEmpty())
                    txtR(cs, normal, 9, 545 - 5, textY, dataFormatada);
                cs.setNonStrokingColor(Color.BLACK);
                y = headerBot - 24;

                /* ── VALOR EM DESTAQUE ── */
                double valor = req.valor;
                String valorFmt = money(valor);
                txt(cs, bold, 24, 50, y, valorFmt);
                y -= 32;
                hline(cs, new Color(224, 224, 221), 0.5f, 50, 545, y);
                y -= 16;

                /* ── CORPO ── */
                String cliente = san(req.clienteNome, 80);
                y = wrap(cs, normal, 10, 50, y, 495, "Recebi de " + cliente + " a quantia de " + valorFmt);

                String extenso = "(" + valorPorExtenso(valor) + ")";
                cs.setNonStrokingColor(new Color(85, 85, 85));
                y = wrap(cs, italic, 10, 50, y, 495, extenso);
                cs.setNonStrokingColor(Color.BLACK);
                y -= 4;

                if (ok(req.descricaoServico))
                    y = wrap(cs, normal, 10, 50, y, 495, "referente a: " + san(req.descricaoServico, 500));

                y -= 24;
                hline(cs, new Color(224, 224, 221), 0.5f, 50, 545, y);
                y -= 16;

                /* ── PRESTADOR ── */
                cs.setNonStrokingColor(new Color(136, 136, 136));
                txt(cs, bold, 8, 50, y, "PRESTADOR");
                cs.setNonStrokingColor(Color.BLACK);
                y -= 12;

                txt(cs, bold, 11, 50, y, san(req.prestadorNome, 70));
                y -= 14;

                if (ok(req.prestadorCpfCnpj)) {
                    txt(cs, normal, 9, 50, y, san(req.prestadorCpfCnpj, 40));
                    y -= 12;
                }

                String localData = "";
                if (ok(req.cidade))         localData = san(req.cidade, 50);
                if (!dataFormatada.isEmpty()) localData += (localData.isEmpty() ? "" : ", ") + dataFormatada;
                if (!localData.isEmpty()) {
                    txt(cs, normal, 9, 50, y, localData);
                    y -= 12;
                }

                y -= 48;

                /* ── ASSINATURA ── */
                hline(cs, Color.BLACK, 0.8f, 50, 250, y);
                y -= 12;
                txt(cs, normal, 8, 50, y, san(req.prestadorNome, 40));
                y -= 11;
                cs.setNonStrokingColor(new Color(136, 136, 136));
                txt(cs, normal, 8, 50, y, "Assinatura do Prestador");
                cs.setNonStrokingColor(Color.BLACK);

                /* ── PAGE FOOTER ── */
                footer(cs, normal);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    /* ────────────────────────── HELPERS ────────────────────────── */

    private static void footer(PDPageContentStream cs, PDFont normal) throws Exception {
        hline(cs, new Color(224, 224, 221), 0.5f, 50, 545, 52);
        cs.setNonStrokingColor(new Color(170, 170, 170));
        float w = tw(normal, 7, FOOTER);
        txt(cs, normal, 7, 50 + (495 - w) / 2f, 40, FOOTER);
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static float wrap(PDPageContentStream cs, PDFont f, float sz,
                               float x, float y, float maxW, String text) throws Exception {
        if (!ok(text)) return y;
        String clean = san(text, 1000);
        String[] words = clean.split("\\s+");
        StringBuilder line = new StringBuilder();
        for (String word : words) {
            if (word.isEmpty()) continue;
            String cand = line.length() == 0 ? word : line + " " + word;
            float w;
            try { w = f.getStringWidth(cand) / 1000f * sz; } catch (Exception e) { w = maxW + 1; }
            if (w > maxW && line.length() > 0) {
                txt(cs, f, sz, x, y, line.toString());
                y -= sz * 1.5f;
                line = new StringBuilder(word);
            } else {
                if (line.length() > 0) line.append(' ');
                line.append(word);
            }
        }
        if (line.length() > 0) { txt(cs, f, sz, x, y, line.toString()); y -= sz * 1.5f; }
        return y;
    }

    private static void hline(PDPageContentStream cs, Color col, float w,
                                float x1, float x2, float lineY) throws Exception {
        cs.setStrokingColor(col); cs.setLineWidth(w);
        cs.moveTo(x1, lineY); cs.lineTo(x2, lineY); cs.stroke();
        cs.setStrokingColor(Color.BLACK); cs.setLineWidth(1f);
    }

    private static void txt(PDPageContentStream cs, PDFont f, float sz,
                              float x, float y, String s) throws Exception {
        cs.setFont(f, sz);
        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText(s); cs.endText();
    }

    private static void txtR(PDPageContentStream cs, PDFont f, float sz,
                               float rightX, float y, String s) throws Exception {
        txt(cs, f, sz, rightX - tw(f, sz, s), y, s);
    }

    private static float tw(PDFont f, float sz, String s) {
        try { return f.getStringWidth(s) / 1000f * sz; }
        catch (Exception e) { return s.length() * sz * 0.5f; }
    }

    private static String money(double v) {
        String s = String.format(java.util.Locale.US, "%,.2f", v);
        return "R$ " + s.replace(".", "X").replace(",", ".").replace("X", ",");
    }

    private static String san(String input, int max) {
        if (input == null) return "";
        StringBuilder sb = new StringBuilder();
        for (char ch : input.toCharArray())
            if ((ch >= 0x20 && ch <= 0x7E) || (ch >= 0xA0 && ch <= 0xFF)) sb.append(ch);
        String s = sb.toString().trim();
        return s.length() > max ? s.substring(0, max) : s;
    }

    private static boolean ok(String s) { return s != null && !s.isBlank(); }

    private static PDImageXObject loadLogo(PDDocument doc, String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try {
            if (b64.contains(",")) b64 = b64.substring(b64.indexOf(",") + 1);
            byte[] bytes = java.util.Base64.getDecoder().decode(b64.trim());
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            return img == null ? null : LosslessFactory.createFromImage(doc, img);
        } catch (Exception e) { return null; }
    }

    /* ── VALOR POR EXTENSO (ASCII-safe) ── */

    private static String valorPorExtenso(double valor) {
        long intPart = (long) valor;
        long cents = Math.round((valor - intPart) * 100);
        if (cents >= 100) { intPart++; cents = 0; }
        if (intPart == 0 && cents == 0) return "zero reais";
        StringBuilder sb = new StringBuilder();
        if (intPart > 0) { sb.append(porExtenso(intPart)); sb.append(intPart == 1 ? " real" : " reais"); }
        if (cents > 0) {
            if (intPart > 0) sb.append(" e ");
            sb.append(porExtenso(cents)); sb.append(cents == 1 ? " centavo" : " centavos");
        }
        return sb.toString();
    }

    private static String porExtenso(long n) {
        if (n == 0) return "zero";
        StringBuilder sb = new StringBuilder();
        if (n >= 1_000_000) {
            long m = n / 1_000_000;
            sb.append(porExtenso(m)).append(m == 1 ? " milhao" : " milhoes");
            n %= 1_000_000; if (n > 0) sb.append(n < 100 ? " e " : ", ");
        }
        if (n >= 1_000) {
            long m = n / 1_000;
            if (m == 1) sb.append("mil"); else sb.append(porExtenso(m)).append(" mil");
            n %= 1_000; if (n > 0) sb.append(n < 100 ? " e " : ", ");
        }
        if (n >= 100) {
            long cent = n / 100;
            if (n % 100 == 0) sb.append(CENTENAS[(int) cent]);
            else { sb.append(cent == 1 ? "cento" : CENTENAS[(int) cent]); sb.append(" e "); }
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
