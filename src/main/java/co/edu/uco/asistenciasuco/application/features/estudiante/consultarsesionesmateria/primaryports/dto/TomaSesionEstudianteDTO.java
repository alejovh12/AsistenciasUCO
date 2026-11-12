package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.primaryports.dto;

import java.time.Instant;
import java.util.UUID;

/** PC (puntos de control): toma de la sesion del estudiante; mismos valores que {@code TomaSesionEstudianteDomain}. */
public record TomaSesionEstudianteDTO(UUID idToma, Integer numero, Instant fechaPuntoControlUtc, String estado,
                                      boolean reclamable, UUID idSolicitudRevision, String estadoSolicitudRevision,
                                      String resultadoAprobacion) {
}
