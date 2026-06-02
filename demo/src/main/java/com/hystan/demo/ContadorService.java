package com.hystan.demo;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ContadorService {

    @Autowired
    private ContadorRepository contadorRepository;

    @Transactional
    public int proximoNumero(String firebaseUid, String tipo) {
        Contador contador = contadorRepository
            .findByFirebaseUidAndTipo(firebaseUid, tipo)
            .orElseGet(() -> {
                Contador novo = new Contador();
                novo.setFirebaseUid(firebaseUid);
                novo.setTipo(tipo);
                novo.setUltimoNumero(0);
                return novo;
            });
        contador.setUltimoNumero(contador.getUltimoNumero() + 1);
        contadorRepository.save(contador);
        return contador.getUltimoNumero();
    }
}
