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
    private static final String FOOTER_TEXT = "Gerado com Hystan · www.hystan.com.br";

    private static final float PAGE_H    = PDRectangle.A4.getHeight(); // 841.89
    private static final float MAR_L     = 40f;
    private static final float MAR_R     = 555f;
    private static final float CONTENT_W = 515f;

    // Table columns: x 40 -> 272 -> 323 -> 436 -> 555
    private static final float C_DESC_X = 40f,  C_DESC_W = 232f;
    private static final float C_QTD_X  = 272f, C_QTD_W  =  51f;
    private static final float C_UNIT_X = 323f, C_UNIT_W = 113f;
    private static final float C_TOT_X  = 436f;
    private static final float C_TOT_R  = 555f;

    private static final float ROW_H = 20f;
    private static final float TBL_H = 22f;
    private static final float TOT_H = 26f;
    private static final float MIN_Y = 130f; // clears signature + footer
    private static final float PAD   =  4f;

    // Palette
    private static final Color CBLACK = new Color( 20,  20,  20);
    private static final Color CTEXT  = new Color( 40,  40,  40);
    private static final Color CGM    = new Color(100, 100, 100);
    private static final Color CGL    = new Color(160, 160, 160);
    private static final Color CGS    = new Color(140, 140, 140);
    private static final Color CGHD   = new Color( 45,  45,  45);
    private static final Color CGRW   = new Color(250, 250, 250);
    private static final Color CGSP   = new Color(210, 210, 210);
    private static final Color CGLN   = new Color(225, 225, 225);
    private static final Color CGST   = new Color(242, 242, 242);
    private static final Color CGFT   = new Color(190, 190, 190);
    private static final Color COBS   = new Color( 80,  80,  80);
    private static final Color CWHITE = Color.WHITE;

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public static byte[] gerar(OrcamentoRequest req) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDFont bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            int numOrc = 1000 + RNG.nextInt(9000);
            String hoje = LocalDate.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
            PDImageXObject logo = loadLogo(doc, req.logoBase64);
            List<OrcamentoRequest.ItemOrcamento> itens =
                req.itens != null ? req.itens : Collections.emptyList();

            // First page
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            drawHeader(cs, bold, normal, numOrc, hoje, logo);
            drawInfoBlocks(cs, bold, normal, req);

            float y = 625f;
            y = drawTableHeader(cs, bold, y);

            // Item rows
            double subtotal = 0;
            for (int i = 0; i < itens.size(); i++) {
                if (y - ROW_H < MIN_Y) {
                    drawFooter(cs, normal);
                    cs.close();
                    page = new PDPage(PDRectangle.A4);
                    doc.addPage(page);
                    cs = new PDPageContentStream(doc, page);
                    y = PAGE_H - 40f;
                    y = drawTableHeader(cs, bold, y);
                }
                OrcamentoRequest.ItemOrcamento item = itens.get(i);
                double rowTotal = item.quantidade * item.valorUnitario;
                subtotal += rowTotal;
                y = drawItemRow(cs, normal, i, item, rowTotal, y);
            }

            // Summary rows — if not enough room, push to new page
            if (y - ROW_H - TOT_H < MIN_Y) {
                drawFooter(cs, normal);
                cs.close();
                page = new PDPage(PDRectangle.A4);
                doc.addPage(page);
                cs = new PDPageContentStream(doc, page);
                y = PAGE_H - 40f;
            }
            y = drawSubtotalRow(cs, bold, normal, y, subtotal);
            y = drawTotalRow(cs, bold, y, subtotal);

            // Post-table sections
            y -= 20f;
            y = drawValidade(cs, bold, normal, y, req);
            y = drawObservacoes(cs, bold, normal, y, req);

            // Signature and footer always at fixed positions on last page
            drawSignature(cs, bold, normal, req.clienteNome);
            drawFooter(cs, normal);
            cs.close();

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            doc.save(baos);
            return baos.toByteArray();
        }
    }

    // ─────────────────────────────────────────────────────────────
    // SECTION DRAWERS
    // ─────────────────────────────────────────────────────────────

    private static void drawHeader(PDPageContentStream cs, PDFont bold, PDFont normal,
                                   int numOrc, String hoje, PDImageXObject logo) throws Exception {
        // Title — left side
        float titleY = PAGE_H - 32f; // baseline ~810
        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 20f, MAR_L, titleY, "ORCAMENTO - No " + numOrc);

        cs.setNonStrokingColor(CGS);
        txt(cs, normal, 9f, MAR_L, titleY - 18f, "Data de emissao: " + hoje);

        // Logo — right side, scaled to fit 100x55 box, top-right aligned
        if (logo != null) {
            float maxW = 100f, maxH = 55f;
            float iW   = logo.getWidth(), iH = logo.getHeight();
            float scale = Math.min(maxW / iW, maxH / iH);
            float dW = iW * scale, dH = iH * scale;
            cs.drawImage(logo, MAR_R - dW, PAGE_H - dH, dW, dH);
        }

        cs.setNonStrokingColor(CBLACK);
        hline(cs, CGSP, 0.5f, MAR_L, MAR_R, 755f);
    }

    private static void drawInfoBlocks(PDPageContentStream cs, PDFont bold, PDFont normal,
                                       OrcamentoRequest req) throws Exception {
        // ── PRESTADOR (left) ──
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, MAR_L, 740f, "PRESTADOR");

        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 12f, MAR_L, 727f, san(req.prestadorNome, 50));

        cs.setNonStrokingColor(CGM);
        float infoY = 714f;
        if (ok(req.prestadorCnpj))     { txt(cs, normal, 9f, MAR_L, infoY, san(req.prestadorCnpj, 30));     infoY -= 12f; }
        if (ok(req.prestadorTelefone)) { txt(cs, normal, 9f, MAR_L, infoY, san(req.prestadorTelefone, 25)); }

        // Vertical separator between blocks
        vline(cs, CGSP, 0.5f, 297f, 640f, 745f);

        // ── CLIENTE (right) ──
        float cx = 307f;
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, cx, 740f, "CLIENTE");

        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 12f, cx, 727f, san(req.clienteNome, 50));

        cs.setNonStrokingColor(CGM);
        if (ok(req.clienteCnpj)) txt(cs, normal, 9f, cx, 714f, san(req.clienteCnpj, 30));

        cs.setNonStrokingColor(CBLACK);
        hline(cs, CGSP, 0.5f, MAR_L, MAR_R, 635f);
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont bold, float y) throws Exception {
        cs.setNonStrokingColor(CGHD);
        cs.addRect(C_DESC_X, y - TBL_H, CONTENT_W, TBL_H);
        cs.fill();

        cs.setNonStrokingColor(CWHITE);
        float tY = y - TBL_H + (TBL_H - 8f) / 2f + 2f;
        txt(cs,  bold, 8f, C_DESC_X + PAD,             tY, "DESCRICAO");
        txtR(cs, bold, 8f, C_QTD_X  + C_QTD_W  - PAD, tY, "QTD");
        txtR(cs, bold, 8f, C_UNIT_X + C_UNIT_W - PAD, tY, "VALOR UNIT.");
        txtR(cs, bold, 8f, C_TOT_R  - PAD,             tY, "TOTAL");

        cs.setNonStrokingColor(CBLACK);
        return y - TBL_H;
    }

    private static float drawItemRow(PDPageContentStream cs, PDFont normal,
                                     int idx, OrcamentoRequest.ItemOrcamento item,
                                     double rowTotal, float y) throws Exception {
        // Even rows (1-indexed) = gray stripe; pares = idx%2==1 in 0-indexed
        if (idx % 2 == 1) {
            cs.setNonStrokingColor(CGRW);
            cs.addRect(C_DESC_X, y - ROW_H, CONTENT_W, ROW_H);
            cs.fill();
        }
        hline(cs, CGLN, 0.3f, C_DESC_X, C_TOT_R, y - ROW_H);

        float rY = y - ROW_H + (ROW_H - 9f) / 2f + 2f;
        cs.setNonStrokingColor(CTEXT);
        String desc = trunc(normal, 9f, san(item.descricao, 150), C_DESC_W - PAD * 2);
        txt(cs,  normal, 9f, C_DESC_X + PAD,             rY, desc);
        txtR(cs, normal, 9f, C_QTD_X  + C_QTD_W  - PAD, rY, String.valueOf(item.quantidade));
        txtR(cs, normal, 9f, C_UNIT_X + C_UNIT_W - PAD, rY, money(item.valorUnitario));
        txtR(cs, normal, 9f, C_TOT_R  - PAD,             rY, money(rowTotal));

        return y - ROW_H;
    }

    private static float drawSubtotalRow(PDPageContentStream cs, PDFont bold, PDFont normal,
                                         float y, double subtotal) throws Exception {
        cs.setNonStrokingColor(CGST);
        cs.addRect(C_DESC_X, y - ROW_H, CONTENT_W, ROW_H);
        cs.fill();

        float rY = y - ROW_H + (ROW_H - 9f) / 2f + 2f;
        cs.setNonStrokingColor(CGM);
        txtR(cs, normal, 9f, C_TOT_X - PAD, rY, "Subtotal");
        cs.setNonStrokingColor(CBLACK);
        txtR(cs, bold,   9f, C_TOT_R - PAD, rY, money(subtotal));

        return y - ROW_H;
    }

    private static float drawTotalRow(PDPageContentStream cs, PDFont bold,
                                      float y, double total) throws Exception {
        cs.setNonStrokingColor(CGHD);
        cs.addRect(C_DESC_X, y - TOT_H, CONTENT_W, TOT_H);
        cs.fill();

        float tY = y - TOT_H + (TOT_H - 10f) / 2f + 2f;
        cs.setNonStrokingColor(CWHITE);
        txt(cs,  bold, 10f, MAR_L + PAD,  tY, "TOTAL");
        txtR(cs, bold, 12f, C_TOT_R - PAD, tY, money(total));

        cs.setNonStrokingColor(CBLACK);
        return y - TOT_H;
    }

    private static float drawValidade(PDPageContentStream cs, PDFont bold, PDFont normal,
                                      float y, OrcamentoRequest req) throws Exception {
        if (!ok(req.validadeDias)) return y;
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, MAR_L, y, "VALIDADE DO ORCAMENTO");
        y -= 13f;
        cs.setNonStrokingColor(CBLACK);
        txt(cs, normal, 10f, MAR_L, y, san(req.validadeDias, 10) + " dias");
        return y - 16f;
    }

    private static float drawObservacoes(PDPageContentStream cs, PDFont bold, PDFont normal,
                                         float y, OrcamentoRequest req) throws Exception {
        if (!ok(req.observacoes)) return y;
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, MAR_L, y, "OBSERVACOES");
        y -= 13f;
        cs.setNonStrokingColor(COBS);
        for (String line : wrapText(normal, 9f, san(req.observacoes, 600), CONTENT_W)) {
            if (y < MIN_Y + 20f) break;
            txt(cs, normal, 9f, MAR_L, y, line);
            y -= 13f;
        }
        cs.setNonStrokingColor(CBLACK);
        return y;
    }

    private static void drawSignature(PDPageContentStream cs, PDFont bold, PDFont normal,
                                      String clienteNome) throws Exception {
        float sigY = 110f;
        float x1   = 370f, x2 = 555f;
        float cx   = x1 + (x2 - x1) / 2f;

        hline(cs, CGL, 0.5f, x1, x2, sigY);

        String label = "Assinatura do cliente";
        cs.setNonStrokingColor(CGL);
        txt(cs, normal, 8f, cx - tw(normal, 8f, label) / 2f, sigY - 10f, label);

        if (ok(clienteNome)) {
            String name = san(clienteNome, 40);
            cs.setNonStrokingColor(CBLACK);
            txt(cs, bold, 9f, cx - tw(bold, 9f, name) / 2f, sigY - 22f, name);
        }
        cs.setNonStrokingColor(CBLACK);
    }

    private static void drawFooter(PDPageContentStream cs, PDFont normal) throws Exception {
        hline(cs, CGSP, 0.3f, MAR_L, MAR_R, 42f);
        float w = tw(normal, 7f, FOOTER_TEXT);
        cs.setNonStrokingColor(CGFT);
        txt(cs, normal, 7f, MAR_L + (CONTENT_W - w) / 2f, 28f, FOOTER_TEXT);
        cs.setNonStrokingColor(CBLACK);
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private static void hline(PDPageContentStream cs, Color col, float lw,
                               float x1, float x2, float lineY) throws Exception {
        cs.setStrokingColor(col);
        cs.setLineWidth(lw);
        cs.moveTo(x1, lineY); cs.lineTo(x2, lineY); cs.stroke();
        cs.setStrokingColor(Color.BLACK); cs.setLineWidth(1f);
    }

    private static void vline(PDPageContentStream cs, Color col, float lw,
                               float x, float y1, float y2) throws Exception {
        cs.setStrokingColor(col);
        cs.setLineWidth(lw);
        cs.moveTo(x, y1); cs.lineTo(x, y2); cs.stroke();
        cs.setStrokingColor(Color.BLACK); cs.setLineWidth(1f);
    }

    private static void txt(PDPageContentStream cs, PDFont f, float sz,
                             float x, float y, String s) throws Exception {
        if (s == null || s.isEmpty()) return;
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

    private static String trunc(PDFont f, float sz, String s, float maxW) {
        if (s == null || s.isEmpty()) return "";
        try {
            if (tw(f, sz, s) <= maxW) return s;
            float dotW = tw(f, sz, "...");
            while (!s.isEmpty()) {
                s = s.substring(0, s.length() - 1);
                if (tw(f, sz, s) + dotW <= maxW) return s + "...";
            }
        } catch (Exception ignored) {}
        int est = (int)(maxW / (sz * 0.5f));
        return s.length() <= est ? s : s.substring(0, Math.max(0, est - 3)) + "...";
    }

    private static List<String> wrapText(PDFont f, float sz, String text, float maxW) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) return result;
        String[] words = text.split("\\s+");
        StringBuilder current = new StringBuilder();
        for (String word : words) {
            String test = current.length() == 0 ? word : current + " " + word;
            try {
                if (f.getStringWidth(test) / 1000f * sz <= maxW) {
                    current = new StringBuilder(test);
                } else {
                    if (current.length() > 0) result.add(current.toString());
                    // If a single word is wider than maxW, truncate it
                    current = new StringBuilder(trunc(f, sz, word, maxW));
                }
            } catch (Exception e) {
                if (current.length() > 0) current.append(" ");
                current.append(word);
            }
        }
        if (current.length() > 0) result.add(current.toString());
        return result;
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
