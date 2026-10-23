package com.sofia.clinica;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sofia.clinica.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import java.util.Map;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClinicaApiIntegrationTest {
    @Autowired MockMvc mvc;
    @Autowired ObjectMapper json;
    @Autowired CitaRepository citas;
    @Autowired PacienteRepository pacientes;

    @BeforeEach
    void limpiar() {
        citas.deleteAll();
        pacientes.deleteAll();
    }

    @Test
    void crudCompletoDePacientes() throws Exception {
        long id = crearPaciente("1001");
        mvc.perform(get("/api/pacientes")).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)));
        mvc.perform(get("/api/pacientes/{id}", id)).andExpect(status().isOk())
                .andExpect(jsonPath("$.documento").value("1001"));
        mvc.perform(put("/api/pacientes/{id}", id).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(paciente("1002"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.documento").value("1002"));
        mvc.perform(delete("/api/pacientes/{id}", id)).andExpect(status().isNoContent());
        mvc.perform(get("/api/pacientes/{id}", id)).andExpect(status().isNotFound());
    }

    @Test
    void crudDeCitasYRelacionUnoAMuchos() throws Exception {
        long pacienteId = crearPaciente("1001");
        long citaId = crearCita(pacienteId);
        crearCita(pacienteId);
        mvc.perform(get("/api/citas")).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(2)));
        mvc.perform(get("/api/pacientes/{id}/citas", pacienteId)).andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].pacienteId").value(pacienteId));
        mvc.perform(get("/api/citas/{id}", citaId)).andExpect(status().isOk())
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
        mvc.perform(put("/api/citas/{id}", citaId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(pacienteId, "COMPLETADA"))))
                .andExpect(status().isOk()).andExpect(jsonPath("$.estado").value("COMPLETADA"));
        mvc.perform(delete("/api/citas/{id}", citaId)).andExpect(status().isNoContent());
        mvc.perform(get("/api/citas/{id}", citaId)).andExpect(status().isNotFound());
    }

    @Test
    void pacienteConCitasNoSeEliminaHastaEliminarSusCitas() throws Exception {
        long pacienteId = crearPaciente("1001");
        long citaId = crearCita(pacienteId);
        mvc.perform(delete("/api/pacientes/{id}", pacienteId)).andExpect(status().isConflict());
        mvc.perform(get("/api/citas/{id}", citaId)).andExpect(status().isOk());
        mvc.perform(delete("/api/citas/{id}", citaId)).andExpect(status().isNoContent());
        mvc.perform(delete("/api/pacientes/{id}", pacienteId)).andExpect(status().isNoContent());
    }

    @Test
    void documentoUnicoEnCreacionYActualizacion() throws Exception {
        crearPaciente("1001");
        enviarPaciente(paciente(" 1001 ")).andExpect(status().isConflict());
        long segundo = crearPaciente("1002");
        mvc.perform(put("/api/pacientes/{id}", segundo).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(paciente("1001")))).andExpect(status().isConflict());
        mvc.perform(get("/api/pacientes/{id}", segundo))
                .andExpect(jsonPath("$.documento").value("1002"));
    }

    @Test
    void validaCamposObligatoriosYFechas() throws Exception {
        enviarPaciente(Map.of("email", "no-es-email", "fechaNacimiento", "2999-01-01"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.email").exists())
                .andExpect(jsonPath("$.errors.fechaNacimiento").exists())
                .andExpect(jsonPath("$.errors.documento").exists());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isBadRequest()).andExpect(jsonPath("$.errors.pacienteId").exists());
    }

    @Test
    void citaRequierePacienteExistenteYNoGuardaDatosInvalidos() throws Exception {
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(999999L, "PROGRAMADA")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/citas")).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void actualizarCitaPermiteCambiarPacienteYFiltraCorrectamente() throws Exception {
        long primero = crearPaciente("1001");
        long segundo = crearPaciente("1002");
        long citaId = crearCita(primero);
        mvc.perform(put("/api/citas/{id}", citaId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(segundo, "CANCELADA")))).andExpect(status().isOk());
        mvc.perform(get("/api/pacientes/{id}/citas", primero)).andExpect(jsonPath("$", hasSize(0)));
        mvc.perform(get("/api/pacientes/{id}/citas", segundo)).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void actualizarCitaConPacienteInexistenteConservaLaRelacion() throws Exception {
        long pacienteId = crearPaciente("1001");
        long citaId = crearCita(pacienteId);
        mvc.perform(put("/api/citas/{id}", citaId).contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(999999L, "CANCELADA")))).andExpect(status().isNotFound());
        mvc.perform(get("/api/citas/{id}", citaId)).andExpect(jsonPath("$.pacienteId").value(pacienteId))
                .andExpect(jsonPath("$.estado").value("PROGRAMADA"));
    }

    @Test
    void recursosInexistentesDevuelven404() throws Exception {
        mvc.perform(get("/api/pacientes/999999")).andExpect(status().isNotFound());
        mvc.perform(get("/api/pacientes/999999/citas")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/pacientes/999999")).andExpect(status().isNotFound());
        mvc.perform(delete("/api/citas/999999")).andExpect(status().isNotFound());
        mvc.perform(put("/api/pacientes/999999").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(paciente("1001")))).andExpect(status().isNotFound());
        mvc.perform(put("/api/citas/999999").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(999999L, "PROGRAMADA")))).andExpect(status().isNotFound());
    }

    @Test
    void formatosIncorrectosDevuelven400() throws Exception {
        mvc.perform(get("/api/pacientes/abc")).andExpect(status().isBadRequest());
        mvc.perform(post("/api/pacientes").contentType(MediaType.APPLICATION_JSON).content("{mal"))
                .andExpect(status().isBadRequest());
        mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(1L, "ESTADO_INVALIDO"))))
                .andExpect(status().isBadRequest());
    }

    @Test
    void saludYPortadaDisponibles() throws Exception {
        mvc.perform(get("/actuator/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("UP"));
        mvc.perform(get("/")).andExpect(status().isOk());
    }

    private Map<String, Object> paciente(String documento) {
        return Map.of("documento", documento, "nombres", "Ana", "apellidos", "Pérez",
                "fechaNacimiento", "2000-05-10", "email", "ana@example.com", "telefono", "3001234567");
    }

    private Map<String, Object> cita(long pacienteId, String estado) {
        return Map.of("pacienteId", pacienteId, "fechaHora", "2026-11-15T09:00:00", "motivo", "Control general",
                "medico", "Dra. Laura Gómez", "estado", estado);
    }

    private ResultActions enviarPaciente(Object body) throws Exception {
        return mvc.perform(post("/api/pacientes").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(body)));
    }

    private long crearPaciente(String documento) throws Exception {
        String body = enviarPaciente(paciente(documento)).andExpect(status().isCreated())
                .andExpect(header().exists("Location")).andReturn().getResponse().getContentAsString();
        return json.readTree(body).get("id").asLong();
    }

    private long crearCita(long pacienteId) throws Exception {
        String body = mvc.perform(post("/api/citas").contentType(MediaType.APPLICATION_JSON)
                .content(json.writeValueAsString(cita(pacienteId, "PROGRAMADA"))))
                .andExpect(status().isCreated()).andExpect(header().exists("Location"))
                .andReturn().getResponse().getContentAsString();
        JsonNode result = json.readTree(body);
        return result.get("id").asLong();
    }
}
