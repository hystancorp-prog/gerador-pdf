package com.hystan.demo;

import java.util.List;

public class OrcamentoRequest {
    public String prestadorNome;
    public String prestadorCnpj;
    public String prestadorTelefone;
    public String clienteNome;
    public String clienteCnpj;
    public String validadeDias;
    public String observacoes;
    public List<ItemOrcamento> itens;

    public static class ItemOrcamento {
        public String descricao;
        public int quantidade;
        public double valorUnitario;
    }
}
