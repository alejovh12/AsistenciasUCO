package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;

import java.util.UUID;

/**
 * Fila JPQL de la consulta de estudiantes por programa (LB-008 JPA-05).
 * Tipos alineados con las entidades: {@code numeroIdentificacion} es {@code int} en SQL Server.
 */
public record EstudianteProgramaQueryRow(
        UUID id,
        UUID idUsuario,
        Integer numeroIdentificacion,
        String nombreCompleto,
        String correo,
        UUID idPrograma,
        String nombrePrograma
) {
}
