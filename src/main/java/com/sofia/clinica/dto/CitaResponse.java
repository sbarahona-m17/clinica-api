package com.sofia.clinica.dto;

import com.sofia.clinica.entity.Cita;
import com.sofia.clinica.entity.EstadoCita;
import java.time.LocalDateTime;

public record CitaResponse(Long id, Long pacienteId, LocalDateTime fechaHora,
                           String motivo, String medico, EstadoCita estado) {
    public static CitaResponse from(Cita c) {
        return new CitaResponse(c.getId(), c.getPaciente().getId(), c.getFechaHora(),
                c.getMotivo(), c.getMedico(), c.getEstado());
    }
}
