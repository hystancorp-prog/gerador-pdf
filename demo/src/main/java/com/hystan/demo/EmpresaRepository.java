package com.hystan.demo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmpresaRepository extends JpaRepository<Empresa, Long> {
    List<Empresa> findByFirebaseUid(String firebaseUid);
    Optional<Empresa> findByIdAndFirebaseUid(Long id, String firebaseUid);
}
