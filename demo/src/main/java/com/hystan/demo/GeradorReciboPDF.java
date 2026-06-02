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

public class GeradorReciboPDF {

    private static final String FOOTER_TEXT = "Gerado com Hystan · www.hystan.com.br";

    private static final float PAGE_H    = PDRectangle.A4.getHeight();
    private static final float MAR_L     = 40f;
    private static final float MAR_R     = 555f;
    private static final float CONTENT_W = 515f;

    // Table columns: DESCRICAO DO SERVICO | VALOR
    private static final float C_SVC_X = 40f,  C_SVC_W = 395f;
    private static final float C_VAL_R = 555f;

    private static final float ROW_H = 20f;
    private static final float TBL_H = 22f;
    private static final float TOT_H = 26f;
    private static final float PAD   =  4f;

    // Palette (same as GeradorOrcamentoPDF)
    private static final Color CBLACK = new Color( 20,  20,  20);
    private static final Color CGM    = new Color(100, 100, 100);
    private static final Color CGL    = new Color(160, 160, 160);
    private static final Color CGS    = new Color(140, 140, 140);
    private static final Color CGHD   = new Color( 45,  45,  45);
    private static final Color CGSP   = new Color(210, 210, 210);
    private static final Color CGFT   = new Color(190, 190, 190);
    private static final Color COBS   = new Color( 80,  80,  80);
    private static final Color CWHITE = Color.WHITE;

    // Valor por extenso tables (ASCII-safe)
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

    // ─────────────────────────────────────────────────────────────
    // PUBLIC ENTRY POINT
    // ─────────────────────────────────────────────────────────────

    public static byte[] gerar(ReciboRequest req) throws Exception {
        try (PDDocument doc = new PDDocument()) {
            PDFont bold   = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDFont normal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            String numRec = ok(req.numeroRecibo) ? san(req.numeroRecibo, 20) : "0001";

            String dataFmt = "";
            if (ok(req.data)) {
                try {
                    dataFmt = LocalDate.parse(req.data)
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy"));
                } catch (Exception e) {
                    dataFmt = san(req.data, 20);
                }
            }

            PDImageXObject logo = loadLogo(doc, req.logoBase64);

            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);
            PDPageContentStream cs = new PDPageContentStream(doc, page);

            drawHeader(cs, bold, normal, numRec, dataFmt, logo);
            drawInfoBlocks(cs, bold, normal, req);

            float y = 625f;
            y = drawTableHeader(cs, bold, y);
            y = drawServiceRow(cs, normal, req, y);
            y = drawTotalRow(cs, bold, y, req.valor);

            // Valor por extenso
            y -= 20f;
            cs.setNonStrokingColor(CGL);
            txt(cs, bold, 7f, MAR_L, y, "VALOR POR EXTENSO");
            y -= 13f;
            cs.setNonStrokingColor(COBS);
            String extenso = "(" + valorPorExtenso(req.valor) + ")";
            for (String line : wrapText(normal, 9f, extenso, CONTENT_W)) {
                txt(cs, normal, 9f, MAR_L, y, line);
                y -= 13f;
            }
            cs.setNonStrokingColor(CBLACK);

            // Local e data
            String localData = buildLocalData(req, dataFmt);
            if (!localData.isEmpty()) {
                y -= 6f;
                cs.setNonStrokingColor(CGL);
                txt(cs, bold, 7f, MAR_L, y, "LOCAL E DATA");
                y -= 13f;
                cs.setNonStrokingColor(CBLACK);
                txt(cs, normal, 10f, MAR_L, y, localData);
            }

            // Signature (fixed position on page, prestador signs the recibo)
            drawSignature(cs, bold, normal, req.prestadorNome);
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
                                   String numRec, String dataFmt,
                                   PDImageXObject logo) throws Exception {
        float titleY = PAGE_H - 32f; // baseline ~810
        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 20f, MAR_L, titleY, "RECIBO - No " + numRec);

        if (!dataFmt.isEmpty()) {
            cs.setNonStrokingColor(CGS);
            txt(cs, normal, 9f, MAR_L, titleY - 18f, "Data: " + dataFmt);
        }

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
                                       ReciboRequest req) throws Exception {
        // ── PRESTADOR (left) ──
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, MAR_L, 740f, "PRESTADOR");

        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 12f, MAR_L, 727f, san(req.prestadorNome, 50));

        cs.setNonStrokingColor(CGM);
        if (ok(req.prestadorCpfCnpj)) txt(cs, normal, 9f, MAR_L, 714f, san(req.prestadorCpfCnpj, 30));

        // Vertical separator
        vline(cs, CGSP, 0.5f, 297f, 640f, 745f);

        // ── CLIENTE (right) ──
        float cx = 307f;
        cs.setNonStrokingColor(CGL);
        txt(cs, bold, 7f, cx, 740f, "CLIENTE");

        cs.setNonStrokingColor(CBLACK);
        txt(cs, bold, 12f, cx, 727f, san(req.clienteNome, 50));

        cs.setNonStrokingColor(CBLACK);
        hline(cs, CGSP, 0.5f, MAR_L, MAR_R, 635f);
    }

    private static float drawTableHeader(PDPageContentStream cs, PDFont bold, float y) throws Exception {
        cs.setNonStrokingColor(CGHD);
        cs.addRect(C_SVC_X, y - TBL_H, CONTENT_W, TBL_H);
        cs.fill();

        cs.setNonStrokingColor(CWHITE);
        float tY = y - TBL_H + (TBL_H - 8f) / 2f + 2f;
        txt(cs,  bold, 8f, C_SVC_X + PAD,  tY, "DESCRICAO DO SERVICO");
        txtR(cs, bold, 8f, C_VAL_R - PAD,  tY, "VALOR");

        cs.setNonStrokingColor(CBLACK);
        return y - TBL_H;
    }

    private static float drawServiceRow(PDPageContentStream cs, PDFont normal,
                                        ReciboRequest req, float y) throws Exception {
        hline(cs, new Color(225, 225, 225), 0.3f, C_SVC_X, C_VAL_R, y - ROW_H);

        float rY = y - ROW_H + (ROW_H - 9f) / 2f + 2f;
        cs.setNonStrokingColor(new Color(40, 40, 40));
        String desc = trunc(normal, 9f, san(req.descricaoServico, 150), C_SVC_W - PAD * 2);
        txt(cs,  normal, 9f, C_SVC_X + PAD, rY, desc);
        txtR(cs, normal, 9f, C_VAL_R - PAD, rY, money(req.valor));

        return y - ROW_H;
    }

    private static float drawTotalRow(PDPageContentStream cs, PDFont bold,
                                      float y, double total) throws Exception {
        cs.setNonStrokingColor(CGHD);
        cs.addRect(C_SVC_X, y - TOT_H, CONTENT_W, TOT_H);
        cs.fill();

        float tY = y - TOT_H + (TOT_H - 10f) / 2f + 2f;
        cs.setNonStrokingColor(CWHITE);
        txt(cs,  bold, 10f, MAR_L + PAD,  tY, "TOTAL");
        txtR(cs, bold, 12f, C_VAL_R - PAD, tY, money(total));

        cs.setNonStrokingColor(CBLACK);
        return y - TOT_H;
    }

    private static void drawSignature(PDPageContentStream cs, PDFont bold, PDFont normal,
                                      String prestadorNome) throws Exception {
        float sigY = 110f;
        float x1   = 370f, x2 = 555f;
        float cx   = x1 + (x2 - x1) / 2f;

        hline(cs, CGL, 0.5f, x1, x2, sigY);

        String label = "Assinatura do Prestador";
        cs.setNonStrokingColor(CGL);
        txt(cs, normal, 8f, cx - tw(normal, 8f, label) / 2f, sigY - 10f, label);

        if (ok(prestadorNome)) {
            String name = san(prestadorNome, 40);
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

    private static String buildLocalData(ReciboRequest req, String dataFmt) {
        String cidade = ok(req.cidade) ? san(req.cidade, 50) : "";
        if (cidade.isEmpty() && dataFmt.isEmpty()) return "";
        if (cidade.isEmpty()) return dataFmt;
        if (dataFmt.isEmpty()) return cidade;
        return cidade + ", " + dataFmt;
    }

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

    // ── VALOR POR EXTENSO ──

    private static String valorPorExtenso(double valor) {
        long intPart = (long) valor;
        long cents   = Math.round((valor - intPart) * 100);
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
            sb.append(DEZENAS[(int)(n / 10)]);
            if (n % 10 > 0) sb.append(" e ").append(UNIDADES[(int)(n % 10)]);
        } else if (n > 0) {
            sb.append(UNIDADES[(int) n]);
        }
        return sb.toString();
    }
}
