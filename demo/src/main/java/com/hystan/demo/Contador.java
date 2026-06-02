package com.hystan.demo;

import jakarta.persistence.*;

@Entity
@Table(name = "contadores")
public class Contador {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firebase_uid")
    private String firebaseUid;

    @Column(name = "tipo")
    private String tipo;

    @Column(name = "ultimo_numero")
    private int ultimoNumero = 0;

    public Long getId()                      { return id; }
    public void setId(Long id)               { this.id = id; }

    public String getFirebaseUid()                       { return firebaseUid; }
    public void setFirebaseUid(String firebaseUid)       { this.firebaseUid = firebaseUid; }

    public String getTipo()                  { return tipo; }
    public void setTipo(String tipo)         { this.tipo = tipo; }

    public int getUltimoNumero()             { return ultimoNumero; }
    public void setUltimoNumero(int n)       { this.ultimoNumero = n; }
}
