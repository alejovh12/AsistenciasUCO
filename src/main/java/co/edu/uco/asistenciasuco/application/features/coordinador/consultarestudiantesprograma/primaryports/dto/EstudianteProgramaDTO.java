package co.edu.uco.asistenciasuco.application.features.coordinador.consultarestudiantesprograma.primaryports.dto;

import java.util.UUID;

public record EstudianteProgramaDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto,
                                    String correo, UUID idPrograma, String nombrePrograma,
                                    Integer semestreActual, Boolean estaActivoUsuario) {
    /** Compatibilidad: sin indicador de estado activo (consulta no paginada). */
    public EstudianteProgramaDTO(UUID id, UUID idUsuario, String numeroIdentificacion, String nombreCompleto, String correo,
              UUID idPrograma, String nombrePrograma, Integer semestreActual) {
        this(id, idUsuario, numeroIdentificacion, nombreCompleto, correo, idPrograma, nombrePrograma, semestreActual, null);
    }
}
