package com.hystan.demo;

import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LeitorPlanilha {

    private static final int MAX_ROWS = 5000;

    public static List<String[]> ler(String caminho, int mesFiltro, int anoFiltro) throws Exception {
        return ler(caminho, mesFiltro, mesFiltro, anoFiltro);
    }

    public static List<String[]> ler(String caminho, int mesInicio, int mesFim, int anoFiltro) throws Exception {
        List<String[]> dados = new ArrayList<>();

        // Prevent ZIP bomb / billion-laughs attacks
        ZipSecureFile.setMinInflateRatio(0.001);

        try (FileInputStream arquivo = new FileInputStream(caminho);
             XSSFWorkbook workbook = new XSSFWorkbook(arquivo)) {

            XSSFSheet aba = workbook.getSheetAt(0);

            if (aba.getLastRowNum() > MAX_ROWS) {
                throw new IllegalArgumentException(
                    "Planilha excede o limite de " + MAX_ROWS + " linhas.");
            }

            DataFormatter formatter = new DataFormatter();
            DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

            for (int i = 1; i <= aba.getLastRowNum(); i++) {
                XSSFRow linha = aba.getRow(i);
                if (linha == null) continue;

                Cell celulaNome = linha.getCell(0);
                if (celulaNome == null || formatter.formatCellValue(celulaNome).isBlank()) continue;

                // Skip rows with missing value (col 2) instead of throwing NPE
                Cell celulaValor = linha.getCell(2);
                if (celulaValor == null) continue;

                Cell celulaData = linha.getCell(3);
                if (celulaData == null) continue;

                String nome    = formatter.formatCellValue(linha.getCell(0));
                String servico = formatter.formatCellValue(linha.getCell(1));

                double valorNum = celulaValor.getNumericCellValue();
                String valor    = String.format("%.2f", valorNum);

                LocalDate data;
                if (celulaData.getCellType() == CellType.NUMERIC) {
                    data = celulaData.getLocalDateTimeCellValue().toLocalDate();
                } else {
                    String dataTexto = formatter.formatCellValue(celulaData);
                    data = LocalDate.parse(dataTexto, fmt);
                }

                String dataStr = data.format(fmt);

                boolean noAno      = data.getYear() == anoFiltro;
                boolean noIntervalo = data.getMonthValue() >= mesInicio
                                   && data.getMonthValue() <= mesFim;

                if (noAno && noIntervalo) {
                    dados.add(new String[]{nome, servico, valor, dataStr});
                }
            }
        }

        return dados;
    }
}
