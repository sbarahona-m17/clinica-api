package com.sofia.clinica.controller;

import com.sofia.clinica.dto.*;
import com.sofia.clinica.service.*;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/pacientes")
public class PacienteController {
    private final PacienteService pacientes;
    private final CitaService citas;

    public PacienteController(PacienteService pacientes, CitaService citas) {
        this.pacientes = pacientes;
        this.citas = citas;
    }

    @GetMapping
    public List<PacienteResponse> listar() { return pacientes.listar(); }

    @GetMapping("/{id}")
    public PacienteResponse obtener(@PathVariable Long id) { return pacientes.obtener(id); }

    @PostMapping
    public ResponseEntity<PacienteResponse> crear(@Valid @RequestBody PacienteRequest request) {
        PacienteResponse paciente = pacientes.crear(request);
        return ResponseEntity.created(URI.create("/api/pacientes/" + paciente.id())).body(paciente);
    }

    @PutMapping("/{id}")
    public PacienteResponse actualizar(@PathVariable Long id, @Valid @RequestBody PacienteRequest request) {
        return pacientes.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        pacientes.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/citas")
    public List<CitaResponse> citas(@PathVariable Long id) { return citas.listarPorPaciente(id); }
}
