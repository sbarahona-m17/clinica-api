package com.sofia.clinica.entity;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pacientes", uniqueConstraints = @UniqueConstraint(name = "uk_paciente_documento", columnNames = "documento"))
public class Paciente {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 30)
    private String documento;
    @Column(nullable = false, length = 100)
    private String nombres;
    @Column(nullable = false, length = 100)
    private String apellidos;
    @Column(nullable = false)
    private LocalDate fechaNacimiento;
    @Column(nullable = false, length = 150)
    private String email;
    @Column(nullable = false, length = 25)
    private String telefono;

    @OneToMany(mappedBy = "paciente", fetch = FetchType.LAZY)
    private List<Cita> citas = new ArrayList<>();

    public Long getId() { return id; }
    public String getDocumento() { return documento; }
    public void setDocumento(String documento) { this.documento = documento; }
    public String getNombres() { return nombres; }
    public void setNombres(String nombres) { this.nombres = nombres; }
    public String getApellidos() { return apellidos; }
    public void setApellidos(String apellidos) { this.apellidos = apellidos; }
    public LocalDate getFechaNacimiento() { return fechaNacimiento; }
    public void setFechaNacimiento(LocalDate fechaNacimiento) { this.fechaNacimiento = fechaNacimiento; }
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    public String getTelefono() { return telefono; }
    public void setTelefono(String telefono) { this.telefono = telefono; }
    public List<Cita> getCitas() { return citas; }
}
