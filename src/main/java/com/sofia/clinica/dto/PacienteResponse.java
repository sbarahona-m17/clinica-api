package com.sofia.clinica.dto;

import com.sofia.clinica.entity.Paciente;
import java.time.LocalDate;

public record PacienteResponse(Long id, String documento, String nombres, String apellidos,
                               LocalDate fechaNacimiento, String email, String telefono) {
    public static PacienteResponse from(Paciente p) {
        return new PacienteResponse(p.getId(), p.getDocumento(), p.getNombres(), p.getApellidos(),
                p.getFechaNacimiento(), p.getEmail(), p.getTelefono());
    }
}
