package com.hystan.demo;

import org.apache.poi.xssf.usermodel.*;
import org.apache.poi.ss.usermodel.*;
import java.io.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class LeitorPlanilha {

    // Mês específico (chamada original)
    public static List<String[]> ler(String caminho, int mesFiltro, int anoFiltro) throws Exception {
        return ler(caminho, mesFiltro, mesFiltro, anoFiltro);
    }

    // Intervalo de meses ou ano inteiro (mes 1 a 12)
    public static List<String[]> ler(String caminho, int mesInicio, int mesFim, int anoFiltro) throws Exception {
        List<String[]> dados = new ArrayList<>();

        FileInputStream arquivo = new FileInputStream(caminho);
        XSSFWorkbook workbook   = new XSSFWorkbook(arquivo);
        XSSFSheet aba           = workbook.getSheetAt(0);

        DataFormatter formatter = new DataFormatter();
        DateTimeFormatter fmt   = DateTimeFormatter.ofPattern("dd/MM/yyyy");

        for (int i = 1; i <= aba.getLastRowNum(); i++) {
            XSSFRow linha = aba.getRow(i);
            if (linha == null) continue;

            Cell celulaNome = linha.getCell(0);
            if (celulaNome == null || formatter.formatCellValue(celulaNome).isBlank()) continue;

            String nome    = formatter.formatCellValue(linha.getCell(0));
            String servico = formatter.formatCellValue(linha.getCell(1));

            double valorNum = linha.getCell(2).getNumericCellValue();
            String valor    = String.format("%.2f", valorNum);

            Cell celulaData = linha.getCell(3);
            if (celulaData == null) continue;

            LocalDate data;
            if (celulaData.getCellType() == CellType.NUMERIC) {
                data = celulaData.getLocalDateTimeCellValue().toLocalDate();
            } else {
                String dataTexto = formatter.formatCellValue(celulaData);
                data = LocalDate.parse(dataTexto, fmt);
            }

            String dataStr = data.format(fmt);

            boolean noAno = data.getYear() == anoFiltro;
            boolean noIntervalo = data.getMonthValue() >= mesInicio
                               && data.getMonthValue() <= mesFim;

            if (noAno && noIntervalo) {
                dados.add(new String[]{nome, servico, valor, dataStr});
            }
        }

        workbook.close();
        arquivo.close();
        return dados;
    }
}