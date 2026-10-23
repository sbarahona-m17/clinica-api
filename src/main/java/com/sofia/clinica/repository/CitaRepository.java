package com.sofia.clinica.repository;

import com.sofia.clinica.entity.Cita;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface CitaRepository extends JpaRepository<Cita, Long> {
    List<Cita> findByPacienteIdOrderByFechaHoraAsc(Long pacienteId);
    boolean existsByPacienteId(Long pacienteId);
}
