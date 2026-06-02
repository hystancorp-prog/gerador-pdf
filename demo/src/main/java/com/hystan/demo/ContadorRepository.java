package com.hystan.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ContadorRepository extends JpaRepository<Contador, Long> {
    Optional<Contador> findByFirebaseUidAndTipo(String firebaseUid, String tipo);
}
