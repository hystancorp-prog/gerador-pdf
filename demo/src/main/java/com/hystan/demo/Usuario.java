package com.hystan.demo;

import jakarta.persistence.*;

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
    private String plano; // "basico", "pro", "gratuito"

    public Usuario() {}

    public Usuario(String email, String nome, String foto) {
        this.email = email;
        this.nome = nome;
        this.foto = foto;
        this.plano = "gratuito";
    }

    public Long getId() { return id; }
    public String getEmail() { return email; }
    public String getNome() { return nome; }
    public String getFoto() { return foto; }
    public String getPlano() { return plano; }

    public void setEmail(String email) { this.email = email; }
    public void setNome(String nome) { this.nome = nome; }
    public void setFoto(String foto) { this.foto = foto; }
    public void setPlano(String plano) { this.plano = plano; }
}