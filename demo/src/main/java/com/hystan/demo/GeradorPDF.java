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
import java.util.List;
import javax.imageio.ImageIO;

public class GeradorPDF {

    /* column boundaries — content width 495 (x 50→545) */
    private static final float CN  = 50,  CNR = 223; // NOME     35%
    private static final float CS  = 223, CSR = 371; // SERVICO  30%
    private static final float CD  = 371, CDR = 455; // DATA     17%
    private static final float CV  = 455, CVR = 545; // VALOR    18%
    private static final float PAD = 5f;
    private static final String FOOTER = "Gerado com Hystan · hystancorp.up.railway.app";

    public static void gerar(List<String[]> dados, String saida,
                             String nomeEmpresa, String logoBase64) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDPage pagina = new PDPage(PDRectangle.A4);
            doc.addPage(pagina);

            PDFont bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, pagina)) {
                float y = 790f;
                String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));

                /* ── HEADER ── */
                float headerH   = 56f;
                float headerBot = y - headerH;
                cs.setNonStrokingColor(new Color(26, 26, 26));
                cs.addRect(50, headerBot, 495, headerH);
                cs.fill();

                PDImageXObject logo = loadLogo(doc, logoBase64);
                float textX = 60f;
                if (logo != null) {
                    float lH = Math.min(36f, logo.getHeight());
                    float lW = lH * ((float) logo.getWidth() / logo.getHeight());
                    cs.drawImage(logo, 60, headerBot + (headerH - lH) / 2f, lW, lH);
                    textX = 60 + lW + 10;
                }

                /* two text lines in header */
                float line1Y = headerBot + headerH - 20f;
                float line2Y = headerBot + 12f;
                cs.setNonStrokingColor(Color.WHITE);
                txt(cs, bold,   13, textX, line1Y, san(nomeEmpresa, 60));
                txt(cs, normal,  9, textX, line2Y, "Relatorio de Atendimentos");
                txtR(cs, normal, 9, 545 - PAD, line2Y, "Gerado em: " + hoje);
                cs.setNonStrokingColor(Color.BLACK);
                y = headerBot - 20;

                /* ── TABLE HEADER ── */
                float thH = 20f;
                cs.setNonStrokingColor(new Color(26, 26, 26));
                cs.addRect(CN, y - thH, 495, thH);
                cs.fill();
                cs.setNonStrokingColor(Color.WHITE);
                float thY = y - thH + (thH - 9f) / 2f + 2f;
                txt(cs, bold, 9, CN + PAD, thY, "NOME");
                txt(cs, bold, 9, CS + PAD, thY, "SERVICO");
                txtC(cs, bold, 9, CD, CDR - CD, thY, "DATA");
                txtR(cs, bold, 9, CVR - PAD,    thY, "VALOR");
                cs.setNonStrokingColor(Color.BLACK);
                y -= thH;

                /* ── TABLE ROWS ── */
                float rowH = 20f;
                double total = 0;

                for (int i = 0; i < dados.size(); i++) {
                    String[] row = dados.get(i);
                    /* row[0]=nome row[1]=servico row[2]=valor row[3]=data */
                    double val = 0;
                    try { val = Double.parseDouble(row[2].replace(",", ".")); }
                    catch (Exception ignored) {}
                    total += val;

                    if (i % 2 == 0) {
                        cs.setNonStrokingColor(new Color(245, 245, 245));
                        cs.addRect(CN, y - rowH, 495, rowH);
                        cs.fill();
                        cs.setNonStrokingColor(Color.BLACK);
                    }
                    hline(cs, new Color(224, 224, 224), 0.3f, CN, CVR, y - rowH);

                    float rY = y - rowH + (rowH - 9f) / 2f + 2f;

                    String nome    = trunc(normal, 9, san(row[0], 80), CNR - CN - PAD * 2);
                    String servico = trunc(normal, 9, san(row[1], 80), CSR - CS - PAD * 2);
                    String data    = san(row[3], 20);
                    String valor   = money(val);

                    txt(cs, normal,  9, CN + PAD, rY, nome);
                    txt(cs, normal,  9, CS + PAD, rY, servico);
                    txtC(cs, normal, 9, CD, CDR - CD, rY, data);
                    txtR(cs, normal, 9, CVR - PAD,    rY, valor);

                    y -= rowH;
                }

                /* ── TOTAL ── */
                y -= 8;
                hline(cs, new Color(26, 26, 26), 1f, CN, CVR, y);
                float totH = 24f;
                cs.setNonStrokingColor(new Color(249, 249, 249));
                cs.addRect(CN, y - totH, 495, totH);
                cs.fill();
                cs.setNonStrokingColor(Color.BLACK);
                float tY = y - totH + (totH - 11f) / 2f + 3f;
                txtR(cs, bold, 11, CDR - PAD, tY, "Total:");
                txtR(cs, bold, 11, CVR - PAD, tY, money(total));

                /* ── PAGE FOOTER ── */
                footer(cs, normal);
            }
            doc.save(saida);
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

    private static void txtC(PDPageContentStream cs, PDFont f, float sz,
                               float colX, float colW, float y, String s) throws Exception {
        txt(cs, f, sz, colX + (colW - tw(f, sz, s)) / 2f, y, s);
    }

    private static float tw(PDFont f, float sz, String s) {
        try { return f.getStringWidth(s) / 1000f * sz; }
        catch (Exception e) { return s.length() * sz * 0.5f; }
    }

    private static String trunc(PDFont f, float sz, String s, float maxW) {
        try {
            if (tw(f, sz, s) <= maxW) return s;
            float dotW = tw(f, sz, "...");
            while (s.length() > 0) {
                s = s.substring(0, s.length() - 1);
                if (tw(f, sz, s) + dotW <= maxW) return s + "...";
            }
        } catch (Exception ignored) {}
        int est = (int)(maxW / (sz * 0.5f));
        return s.length() <= est ? s : s.substring(0, Math.max(0, est - 3)) + "...";
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

    private static PDImageXObject loadLogo(PDDocument doc, String b64) {
        if (b64 == null || b64.isBlank()) return null;
        try {
            if (b64.contains(",")) b64 = b64.substring(b64.indexOf(",") + 1);
            byte[] bytes = java.util.Base64.getDecoder().decode(b64.trim());
            BufferedImage img = ImageIO.read(new ByteArrayInputStream(bytes));
            return img == null ? null : LosslessFactory.createFromImage(doc, img);
        } catch (Exception e) { return null; }
    }
}
