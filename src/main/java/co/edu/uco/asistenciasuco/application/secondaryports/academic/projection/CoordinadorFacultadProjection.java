package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/**
 * Fila de {@code usp_consultar_coordinadores_facultad_paginado} (CIERRE-W1B): un coordinador de la facultad del decano con
 * su primer programa, los datos editables, el estado y los conteos de docentes con vinculacion activa y grupos habilitados.
 */
public record CoordinadorFacultadProjection(
        UUID id,
        UUID idUsuario,
        String numeroIdentificacion,
        String nombreCompleto,
        UUID idPrograma,
        String nombrePrograma,
        boolean estaActivoCoordinador,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String correo,
        boolean estaActivoRolCoordinador,
        String nombreFacultad,
        int totalDocentes,
        int totalGrupos
) {
}
