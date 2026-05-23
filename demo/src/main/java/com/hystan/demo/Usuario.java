package com.hystan.demo;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "usuarios")
public class Usuario {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String email;

    private String nome;
    private String foto;

    @Column(nullable = false)
    private String plano = "gratuito"; // "gratuito", "basico", "pro"

    @Column(name = "stripe_customer_id")
    private String stripeCustomerId;

    @Column(name = "stripe_subscription_id")
    private String stripeSubscriptionId;

    @Column(name = "plano_status")
    private String planoStatus = "inativo"; // "ativo", "trial", "inativo", "cancelado"

    @Column(name = "trial_fim")
    private LocalDateTime trialFim;

    @Column(name = "assinatura_fim")
    private LocalDateTime assinaturaFim;

    @Column(name = "firebase_uid", unique = true)
    private String firebaseUid;

    @Column(name = "docs_mes_atual")
    private Integer docsMesAtual = 0;

    @Column(name = "mes_contagem")
    private Integer mesContagem;

    @Column(name = "ano_contagem")
    private Integer anoContagem;

    @Column(name = "docs_total_trial")
    private Integer docsTotalTrial = 0;

    @Column(name = "criado_em")
    private LocalDateTime criadoEm = LocalDateTime.now();

    @Column(name = "atualizado_em")
    private LocalDateTime atualizadoEm = LocalDateTime.now();

    public Usuario() {}

    public Usuario(String email, String nome, String foto) {
        this.email = email;
        this.nome = nome;
        this.foto = foto;
        this.plano = "gratuito";
        this.planoStatus = "inativo";
        this.criadoEm = LocalDateTime.now();
        this.atualizadoEm = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.atualizadoEm = LocalDateTime.now();
    }

    // Getters
    public Long getId()                         { return id; }
    public Integer getDocsMesAtual()             { return docsMesAtual; }
    public Integer getMesContagem()              { return mesContagem; }
    public Integer getAnoContagem()              { return anoContagem; }
    public Integer getDocsTotalTrial()           { return docsTotalTrial; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getFoto() { return foto; }
    public String getPlano() { return plano; }
    public String getStripeCustomerId() { return stripeCustomerId; }
    public String getStripeSubscriptionId() { return stripeSubscriptionId; }
    public String getPlanoStatus() { return planoStatus; }
    public LocalDateTime getTrialFim() { return trialFim; }
    public LocalDateTime getAssinaturaFim() { return assinaturaFim; }
    public String getFirebaseUid() { return firebaseUid; }
    public LocalDateTime getCriadoEm() { return criadoEm; }
    public LocalDateTime getAtualizadoEm() { return atualizadoEm; }

    // Setters
    public void setDocsMesAtual(Integer v)   { this.docsMesAtual = v; }
    public void setMesContagem(Integer v)    { this.mesContagem = v; }
    public void setAnoContagem(Integer v)    { this.anoContagem = v; }
    public void setDocsTotalTrial(Integer v) { this.docsTotalTrial = v; }

    public void setEmail(String email) { this.email = email; }
    public void setNome(String nome) { this.nome = nome; }
    public void setFoto(String foto) { this.foto = foto; }
    public void setPlano(String plano) { this.plano = plano; }
    public void setStripeCustomerId(String stripeCustomerId) { this.stripeCustomerId = stripeCustomerId; }
    public void setStripeSubscriptionId(String stripeSubscriptionId) { this.stripeSubscriptionId = stripeSubscriptionId; }
    public void setPlanoStatus(String planoStatus) { this.planoStatus = planoStatus; }
    public void setTrialFim(LocalDateTime trialFim) { this.trialFim = trialFim; }
    public void setAssinaturaFim(LocalDateTime assinaturaFim) { this.assinaturaFim = assinaturaFim; }
    public void setFirebaseUid(String firebaseUid) { this.firebaseUid = firebaseUid; }
    public void setCriadoEm(LocalDateTime criadoEm) { this.criadoEm = criadoEm; }
    public void setAtualizadoEm(LocalDateTime atualizadoEm) { this.atualizadoEm = atualizadoEm; }
}