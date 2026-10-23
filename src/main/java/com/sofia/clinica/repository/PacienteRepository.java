package com.sofia.clinica.repository;

import com.sofia.clinica.entity.Paciente;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PacienteRepository extends JpaRepository<Paciente, Long> {
    boolean existsByDocumento(String documento);
    boolean existsByDocumentoAndIdNot(String documento, Long id);
}
