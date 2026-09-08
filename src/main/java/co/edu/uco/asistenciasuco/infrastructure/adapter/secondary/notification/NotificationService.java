package co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.notification;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.UUID;

/**
 * Servicio para emisión de notificaciones institucionales y alertas de ausentismo (HU013).
 */
@Service
public class NotificationService {

    private static final Logger LOGGER = LoggerFactory.getLogger(NotificationService.class);

    private final JdbcTemplate jdbcTemplate;

    public NotificationService(final JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = Objects.requireNonNull(jdbcTemplate, "JdbcTemplate es obligatorio.");
    }

    /**
     * Evalúa y notifica al estudiante en caso de acumulación riesgosa o crítica de inasistencias.
     */
    @Async
    public void evaluarAlertaAusentismo(final UUID estudianteId, final UUID grupoId, final int totalInasistencias) {
        if (totalInasistencias < 2) {
            return;
        }

        final String nivelAlerta = totalInasistencias >= 3 ? "CRÍTICO (Riesgo de Cancelación)" : "ADVERTENCIA (2 Fallas)";
        LOGGER.warn("ALERTA AUSENTISMO [HU013]: Estudiante ID={}, Grupo ID={}, Nivel={}, Inasistencias={}",
                estudianteId, grupoId, nivelAlerta, totalInasistencias);

        try {
            // Registrar evento en AuditoriaEvento
            jdbcTemplate.update(
                    "INSERT INTO dbo.AuditoriaEvento (" +
                    "    id, occurredAt, actorId, actorType, action, resourceType, resourceId, result, correlationId, httpMethod, path, httpStatus, metadata" +
                    ") VALUES (NEWID(), SYSDATETIMEOFFSET(), ?, 'SISTEMA', 'ALERTA_AUSENTISMO', 'EstudianteGrupo', ?, 'EMITIDA', NEWID(), 'INTERNAL', '/evaluarAlertaAusentismo', 200, ?)",
                    estudianteId.toString(),
                    grupoId.toString(),
                    "Notificación emitida: " + nivelAlerta + " por " + totalInasistencias + " inasistencias acumuladas."
            );
        } catch (Exception ex) {
            LOGGER.error("Error registrando auditoria de alerta ausentismo", ex);
        }
    }

    /**
     * Notifica al estudiante la resolución de su solicitud de revisión.
     */
    @Async
    public void notificarResolucionReclamo(final UUID solicitudId, final String estadoResolucion, final String justificacionDocente) {
        LOGGER.info("NOTIFICACIÓN RESOLUCIÓN RECLAMO [HU013]: Solicitud ID={}, Estado={}, Justificación={}",
                solicitudId, estadoResolucion, justificacionDocente);

        try {
            jdbcTemplate.update(
                    "INSERT INTO dbo.AuditoriaEvento (" +
                    "    id, occurredAt, actorId, actorType, action, resourceType, resourceId, result, correlationId, httpMethod, path, httpStatus, metadata" +
                    ") VALUES (NEWID(), SYSDATETIMEOFFSET(), ?, 'DOCENTE', 'RESOLUCION_RECLAMO', 'SolicitudRevisionAsistencia', ?, 'NOTIFICADA', NEWID(), 'INTERNAL', '/notificarResolucionReclamo', 200, ?)",
                    "DOCENTE_SISTEMA",
                    solicitudId.toString(),
                    "Reclamo " + estadoResolucion + ": " + justificacionDocente
            );
        } catch (Exception ex) {
            LOGGER.error("Error registrando auditoria de notificación reclamo", ex);
        }
    }
}
