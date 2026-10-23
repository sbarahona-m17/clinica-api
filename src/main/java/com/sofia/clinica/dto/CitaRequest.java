package com.sofia.clinica.dto;

import com.sofia.clinica.entity.EstadoCita;
import jakarta.validation.constraints.*;
import java.time.LocalDateTime;

public record CitaRequest(
        @NotNull @Positive Long pacienteId,
        @NotNull LocalDateTime fechaHora,
        @NotBlank @Size(max = 250) String motivo,
        @NotBlank @Size(max = 120) String medico,
        @NotNull EstadoCita estado
) {}
