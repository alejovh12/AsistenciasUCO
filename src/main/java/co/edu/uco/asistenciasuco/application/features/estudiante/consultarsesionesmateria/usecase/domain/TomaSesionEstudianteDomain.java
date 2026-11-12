package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain;

import java.time.Instant;
import java.util.UUID;

/**
 * PC (puntos de control): una toma de la sesion vista por el estudiante. {@code estado}: PRESENTE | AUSENTE | JUSTIFICADA |
 * SIN_REGISTRO. {@code reclamable} y la solicitud los calcula la base de datos; {@code estadoSolicitudRevision}: PENDIENTE |
 * APROBADA | RECHAZADA (nulo sin solicitud); {@code resultadoAprobacion}: PRESENTE | JUSTIFICADA (nulo si no esta aprobada).
 */
public record TomaSesionEstudianteDomain(UUID idToma, Integer numero, Instant fechaPuntoControlUtc, String estado,
                                         boolean reclamable, UUID idSolicitudRevision, String estadoSolicitudRevision,
                                         String resultadoAprobacion) {
}
