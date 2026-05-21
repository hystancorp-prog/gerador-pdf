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
import java.util.*;
import javax.imageio.ImageIO;

public class GeradorOrcamentoPDF {

    private static final java.util.Random RNG = new java.util.Random();

    /* column boundaries — content width 495 (x 50→545) */
    private static final float C0 = 50,  C0R = 297; // DESCRICAO  50%
    private static final float C1 = 297, C1R = 347; // QTD        10%
    private static final float C2 = 347, C2R = 446; // VALOR UNIT 20%
    private static final float C3 = 446, C3R = 545; // TOTAL      20%
    private static final float PAD = 5f;
    private static final String FOOTER = "Gerado com Hystan · hystancorp.up.railway.app";

    private static final float PAGE_H   = PDRectangle.A4.getHeight();
    private static final float ROW_H    = 22f;
    private static final float TBLHDR_H = 20f;
    private static final float MIN_Y    = 80f; // footer + margin

    public static byte[] gerar(OrcamentoRequest req) throws Exception {
        try (PDDocument doc = new PDDocument()) {

            PDFont bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            int numOrc = RNG.nextInt(9000) + 1000;
            String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            PDImageXObject logo = loadLogo(doc, req.logoBase64);

            PDPage firstPage = new PDPage(PDRectangle.A4);
            doc.addPage(firstPage);
            PDPageContentStream cs = new PDPageContentStream(doc, firstPage);
            float y = PAGE_H - 10f;

            /* ── HEADER ── */
            float headerH   = 56f;
            float headerBot = y - headerH;
            cs.setNonStrokingColor(new Color(26, 26, 26));
            cs.addRect(C0, headerBot, 495, headerH);
            cs.fill();

            float textX = 60f;
            if (logo != null) {
                float lH = Math.min(36f, logo.getHeight());
                float lW = lH * ((float) logo.getWidth() / logo.getHeight());
                cs.drawImage(logo, 60, headerBot + (headerH - lH) / 2f, lW, lH);
                textX = 60 + lW + 10;
            }

            float textY = headerBot + headerH / 2f - 4f;
            cs.setNonStrokingColor(Color.WHITE);
            txt(cs, bold,   13, textX,       textY, "ORCAMENTO No " + numOrc);
            txtR(cs, normal, 9, C3R - PAD,   textY, "EMISSAO: " + hoje);
            cs.setNonStrokingColor(Color.BLACK);
            y = headerBot - 20;

            /* ── PRESTADOR ── */
            lbl(cs, bold, normal, 50, y, "PRESTADOR");
            y -= 12;
            txt(cs, bold, 11, 50, y, san(req.prestadorNome, 70));
            y -= 14;
            if (ok(req.prestadorCnpj))     { txt(cs, normal, 9, 50, y, san(req.prestadorCnpj, 40));     y -= 12; }
            if (ok(req.prestadorTelefone)) { txt(cs, normal, 9, 50, y, san(req.prestadorTelefone, 40)); y -= 12; }
            y -= 16;

            /* ── CLIENTE ── */
            lbl(cs, bold, normal, 50, y, "CLIENTE");
            y -= 12;
            txt(cs, bold, 11, 50, y, san(req.clienteNome, 70));
            y -= 14;
            if (ok(req.clienteCnpj)) { txt(cs, normal, 9, 50, y, san(req.clienteCnpj, 40)); y -= 12; }
            y -= 20;

            /* ── TABLE HEADER ── */
            y = drawTableHeader(cs, bold, y);

            /* ── TABLE ROWS ── */
            double subtotal = 0;
            List<OrcamentoRequest.ItemOrcamento> itens =
                req.itens != null ? req.itens : Collections.emptyList();

            for (int i = 0; i < itens.size(); i++) {
                /* need space for row + total block (34 + 16) */
                if (y - ROW_H < MIN_Y + 50f) {
                    footer(cs, normal);
                    cs.close();
                    PDPage next = new PDPage(PDRectangle.A4);
                    doc.addPage(next);
                    cs = new PDPageContentStream(doc, next);
                    y  = PAGE_H - 20f;
                    y  = drawTableHeader(cs, bold, y);
                }

                OrcamentoRequest.ItemOrcamento item = itens.get(i);
                double total = item.quantidade * item.valorUnitario;
                subtotal += total;

                if (i % 2 == 0) {
                    cs.setNonStrokingColor(new Color(245, 245, 245));
                    cs.addRect(C0, y - ROW_H, 495, ROW_H);
                    cs.fill();
                    cs.setNonStrokingColor(Color.BLACK);
                }
                hline(cs, new Color(224, 224, 224), 0.3f, C0, C3R, y - ROW_H);

                float rY = y - ROW_H + (ROW_H - 9f) / 2f + 2f;
                txt(cs, normal,  9, C0 + PAD, rY, trunc(normal, 9, san(item.descricao, 100), C0R - C0 - PAD * 2));
                txtC(cs, normal, 9, C1, C1R - C1, rY, String.valueOf(item.quantidade));
                txtR(cs, normal, 9, C2R - PAD, rY, money(item.valorUnitario));
                txtR(cs, normal, 9, C3R - PAD, rY, money(total));
                y -= ROW_H;
            }

            /* ── TOTAL ── */
            y -= 10;
            hline(cs, new Color(26, 26, 26), 1f, C0, C3R, y);
            float totH = 24f;
            cs.setNonStrokingColor(new Color(249, 249, 249));
            cs.addRect(C0, y - totH, 495, totH);
            cs.fill();
            cs.setNonStrokingColor(Color.BLACK);
            float tY = y - totH + (totH - 11f) / 2f + 3f;
            txtR(cs, bold, 11, C2R - PAD, tY, "TOTAL:");
            txtR(cs, bold, 11, C3R - PAD, tY, money(subtotal));
            y -= totH + 16;

            /* ── VALIDADE / OBS. ── */
            if (ok(req.validadeDias)) {
                txt(cs, normal, 9, 50, y, "Validade da proposta: " + san(req.validadeDias, 20) + " dias");
                y -= 13;
            }
            if (ok(req.observacoes)) {
                cs.setNonStrokingColor(new Color(136, 136, 136));
                txt(cs, bold, 8, 50, y, "OBS.:");
                cs.setNonStrokingColor(Color.BLACK);
                y -= 11;
                txt(cs, normal, 9, 50, y, san(req.observacoes, 200));
            }

            footer(cs, normal);
            cs.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont bold, float y) throws Exception {
        float thH = TBLHDR_H;
        cs.setNonStrokingColor(new Color(26, 26, 26));
        cs.addRect(C0, y - thH, 495, thH);
        cs.fill();
        cs.setNonStrokingColor(Color.WHITE);
        float thY = y - thH + (thH - 9f) / 2f + 2f;
        txt(cs, bold,  9, C0 + PAD,    thY, "DESCRICAO");
        txtC(cs, bold, 9, C1, C1R - C1, thY, "QTD");
        txtR(cs, bold, 9, C2R - PAD,   thY, "VALOR UNIT.");
        txtR(cs, bold, 9, C3R - PAD,   thY, "TOTAL");
        cs.setNonStrokingColor(Color.BLACK);
        return y - thH;
    }

    /* ────────────────────────── HELPERS ────────────────────────── */

    private static void footer(PDPageContentStream cs, PDFont normal) throws Exception {
        hline(cs, new Color(224, 224, 221), 0.5f, 50, 545, 52);
        cs.setNonStrokingColor(new Color(170, 170, 170));
        float w = tw(normal, 7, FOOTER);
        txt(cs, normal, 7, 50 + (495 - w) / 2f, 40, FOOTER);
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static void lbl(PDPageContentStream cs, PDFont bold, PDFont normal,
                              float x, float y, String label) throws Exception {
        cs.setNonStrokingColor(new Color(136, 136, 136));
        txt(cs, bold, 8, x, y, label);
        cs.setNonStrokingColor(Color.BLACK);
    }

    private static void hline(PDPageContentStream cs, Color col, float w,
                                float x1, float x2, float lineY) throws Exception {
        cs.setStrokingColor(col);
        cs.setLineWidth(w);
        cs.moveTo(x1, lineY);
        cs.lineTo(x2, lineY);
        cs.stroke();
        cs.setStrokingColor(Color.BLACK);
        cs.setLineWidth(1f);
    }

    private static void txt(PDPageContentStream cs, PDFont f, float sz,
                              float x, float y, String s) throws Exception {
        cs.setFont(f, sz);
        cs.beginText(); cs.newLineAtOffset(x, y); cs.showText(s); cs.endText();
    }

    private static void txtR(PDPageContentStream cs, PDFont f, float sz,
                               float rightX, float y, String s) throws Exception {
        float w = tw(f, sz, s);
        txt(cs, f, sz, rightX - w, y, s);
    }

    private static void txtC(PDPageContentStream cs, PDFont f, float sz,
                               float colX, float colW, float y, String s) throws Exception {
        float w = tw(f, sz, s);
        txt(cs, f, sz, colX + (colW - w) / 2f, y, s);
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
}
