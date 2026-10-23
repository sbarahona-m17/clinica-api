package com.sofia.clinica.service;

import com.sofia.clinica.dto.*;
import com.sofia.clinica.entity.Paciente;
import com.sofia.clinica.exception.*;
import com.sofia.clinica.repository.*;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class PacienteService {
    private final PacienteRepository pacientes;
    private final CitaRepository citas;

    public PacienteService(PacienteRepository pacientes, CitaRepository citas) {
        this.pacientes = pacientes;
        this.citas = citas;
    }

    public List<PacienteResponse> listar() {
        return pacientes.findAll(Sort.by("id")).stream().map(PacienteResponse::from).toList();
    }

    public PacienteResponse obtener(Long id) { return PacienteResponse.from(buscarEntidad(id)); }

    public Paciente buscarEntidad(Long id) {
        return pacientes.findById(id).orElseThrow(() -> new ResourceNotFoundException("Paciente no encontrado: " + id));
    }

    @Transactional
    public PacienteResponse crear(PacienteRequest request) {
        if (pacientes.existsByDocumento(request.documento().trim())) {
            throw new ConflictException("Ya existe un paciente con ese documento.");
        }
        Paciente paciente = new Paciente();
        copiar(request, paciente);
        return PacienteResponse.from(pacientes.save(paciente));
    }

    @Transactional
    public PacienteResponse actualizar(Long id, PacienteRequest request) {
        Paciente paciente = buscarEntidad(id);
        if (pacientes.existsByDocumentoAndIdNot(request.documento().trim(), id)) {
            throw new ConflictException("Ya existe otro paciente con ese documento.");
        }
        copiar(request, paciente);
        return PacienteResponse.from(pacientes.save(paciente));
    }

    @Transactional
    public void eliminar(Long id) {
        Paciente paciente = buscarEntidad(id);
        if (citas.existsByPacienteId(id)) {
            throw new ConflictException("El paciente tiene citas asociadas. Elimina sus citas antes de eliminarlo.");
        }
        pacientes.delete(paciente);
    }

    private void copiar(PacienteRequest r, Paciente p) {
        p.setDocumento(r.documento().trim());
        p.setNombres(r.nombres().trim());
        p.setApellidos(r.apellidos().trim());
        p.setFechaNacimiento(r.fechaNacimiento());
        p.setEmail(r.email().trim());
        p.setTelefono(r.telefono().trim());
    }
}
