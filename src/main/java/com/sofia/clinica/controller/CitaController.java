package com.sofia.clinica.controller;

import com.sofia.clinica.dto.*;
import com.sofia.clinica.service.CitaService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.net.URI;
import java.util.List;

@RestController
@RequestMapping("/api/citas")
public class CitaController {
    private final CitaService citas;

    public CitaController(CitaService citas) { this.citas = citas; }

    @GetMapping
    public List<CitaResponse> listar() { return citas.listar(); }

    @GetMapping("/{id}")
    public CitaResponse obtener(@PathVariable Long id) { return citas.obtener(id); }

    @PostMapping
    public ResponseEntity<CitaResponse> crear(@Valid @RequestBody CitaRequest request) {
        CitaResponse cita = citas.crear(request);
        return ResponseEntity.created(URI.create("/api/citas/" + cita.id())).body(cita);
    }

    @PutMapping("/{id}")
    public CitaResponse actualizar(@PathVariable Long id, @Valid @RequestBody CitaRequest request) {
        return citas.actualizar(id, request);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable Long id) {
        citas.eliminar(id);
        return ResponseEntity.noContent().build();
    }
}
