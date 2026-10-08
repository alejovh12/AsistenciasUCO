package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.persistence.sqlserver.jpa.projection;
import java.util.UUID;
public record EstudianteResumenQueryRow(UUID id, UUID idUsuario, UUID tipoIdentificacionId,
        Integer numeroIdentificacion, String primerApellido, String segundoApellido, String primerNombre,
        String segundoNombre, String nombreCompleto, String correo, Boolean estaActivoUsuario) { }
