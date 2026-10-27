package co.edu.uco.asistenciasuco.application.secondaryports.academic.projection;

import java.util.UUID;

/** Cabecera de la ficha del estudiante (HU069); solo campos existentes en la DB. */
public record FichaEstudianteProjection(
        UUID idEstudiante,
        String tipoIdentificacion,
        Integer numeroIdentificacion,
        String primerNombre,
        String segundoNombre,
        String primerApellido,
        String segundoApellido,
        String nombreCompleto,
        String correo,
        boolean estaActivoUsuario,
        UUID idPrograma,
        String nombrePrograma
) {
}
