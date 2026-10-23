package com.sofia.clinica.service;

import com.sofia.clinica.dto.*;
import com.sofia.clinica.entity.*;
import com.sofia.clinica.exception.ResourceNotFoundException;
import com.sofia.clinica.repository.CitaRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.util.List;

@Service
@Transactional(readOnly = true)
public class CitaService {
    private final CitaRepository citas;
    private final PacienteService pacientes;

    public CitaService(CitaRepository citas, PacienteService pacientes) {
        this.citas = citas;
        this.pacientes = pacientes;
    }

    public List<CitaResponse> listar() {
        return citas.findAll(Sort.by("fechaHora").ascending().and(Sort.by("id")))
                .stream().map(CitaResponse::from).toList();
    }

    public List<CitaResponse> listarPorPaciente(Long pacienteId) {
        pacientes.buscarEntidad(pacienteId);
        return citas.findByPacienteIdOrderByFechaHoraAsc(pacienteId).stream().map(CitaResponse::from).toList();
    }

    public CitaResponse obtener(Long id) { return CitaResponse.from(buscar(id)); }

    @Transactional
    public CitaResponse crear(CitaRequest request) {
        Cita cita = new Cita();
        copiar(request, cita);
        return CitaResponse.from(citas.save(cita));
    }

    @Transactional
    public CitaResponse actualizar(Long id, CitaRequest request) {
        Cita cita = buscar(id);
        copiar(request, cita);
        return CitaResponse.from(citas.save(cita));
    }

    @Transactional
    public void eliminar(Long id) { citas.delete(buscar(id)); }

    private Cita buscar(Long id) {
        return citas.findById(id).orElseThrow(() -> new ResourceNotFoundException("Cita no encontrada: " + id));
    }

    private void copiar(CitaRequest r, Cita c) {
        Paciente paciente = pacientes.buscarEntidad(r.pacienteId());
        c.setPaciente(paciente);
        c.setFechaHora(r.fechaHora());
        c.setMotivo(r.motivo().trim());
        c.setMedico(r.medico().trim());
        c.setEstado(r.estado());
    }
}
