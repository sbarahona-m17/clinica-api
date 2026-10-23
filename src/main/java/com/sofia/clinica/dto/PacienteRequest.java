package com.sofia.clinica.dto;

import jakarta.validation.constraints.*;
import java.time.LocalDate;

public record PacienteRequest(
        @NotBlank @Size(max = 30) String documento,
        @NotBlank @Size(max = 100) String nombres,
        @NotBlank @Size(max = 100) String apellidos,
        @NotNull @PastOrPresent LocalDate fechaNacimiento,
        @NotBlank @Email @Size(max = 150) String email,
        @NotBlank @Size(max = 25) String telefono
) {}
