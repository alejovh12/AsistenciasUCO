package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import org.hibernate.annotations.Immutable;

import java.util.UUID;

/**
 * PC (puntos de control): proyeccion de SOLO LECTURA de {@code dbo.uv_asistencia_sesion_horas} (una fila por cabecera de
 * asistencia). Solo se mapean los minutos y el estado de la sesion para el estudiante.
 */
@Entity
@Immutable
@Table(name = "uv_asistencia_sesion_horas", schema = "dbo")
public class UvAsistenciaSesionHorasEntity {

    @Id @Column(name = "idAsistencia") private UUID idAsistencia;
    @Column(name = "idEstudianteGrupo") private UUID idEstudianteGrupo;
    @Column(name = "idSesion") private UUID idSesion;
    @Column(name = "minutosSesion") private Integer minutosSesion;
    @Column(name = "minutosAsistidos") private Integer minutosAsistidos;
    @Column(name = "minutosFalta") private Integer minutosFalta;
    @Column(name = "minutosExcusa") private Integer minutosExcusa;
    @Column(name = "minutosSinRegistro") private Integer minutosSinRegistro;
    @Column(name = "codigoEstadoSesionEstudiante") private String codigoEstadoSesionEstudiante;

    protected UvAsistenciaSesionHorasEntity() {
        // requerido por JPA
    }
}
