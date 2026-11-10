package co.edu.uco.asistenciasuco.application.secondaryports.academic;

import java.util.UUID;

public interface DecanoCommandPort {

    void crearDecano(CrearDecanoCommand command);

    /**
     * DEC-065 (5): registra, en una sola transaccion de BD, a la persona decano y la facultad que titulara
     * ({@code usp_crear_decano_con_facultad}). Ante cualquier rechazo no queda persona ni facultad.
     */
    ResultadoDecanoConFacultad crearDecanoConFacultad(CrearDecanoConFacultadCommand command);

    /** Identificadores y nombre confirmados por la base de datos para el alta compuesta. */
    record ResultadoDecanoConFacultad(UUID idDecano, UUID idFacultad, String nombreFacultad) {
    }

    record CrearDecanoConFacultadCommand(
            UUID idDecano,
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            String password,
            String nombreFacultad,
            String codigoFacultad,
            UUID idInstitucion,
            UUID usuarioEjecutor
    ) {
    }

    /**
     * CULM-001 A.2-8: cambia el estado del decano ({@code estaActivo} nulo invierte el actual). La BD valida RBAC
     * ADMINISTRADOR, el ambito de la institucion (DEC_001, 404) y que la facultad no tenga programas activos (DEC_002, 409).
     */
    ResultadoCambioEstadoDecano cambiarEstadoDecano(UUID idDecano, Boolean estaActivo, UUID usuarioEjecutor);

    /** Usuario del decano, si la persona quedo sin roles activos y el estado resultante del decano. */
    record ResultadoCambioEstadoDecano(UUID idUsuario, boolean personaSinRolesActivos, boolean estaActivoDecano) {
    }

    record CrearDecanoCommand(
            UUID idDecano,
            UUID tipoIdentificacionId,
            Integer numeroIdentificacion,
            String primerNombre,
            String segundoNombre,
            String primerApellido,
            String segundoApellido,
            String correo,
            UUID idFacultad,
            String nombreFacultad,
            String password,
            UUID usuarioEjecutor
    ) {
    }
}
