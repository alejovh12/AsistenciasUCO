package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.dto.ConfiguracionReclamosDTO;

import java.util.UUID;

/** CULM-001 D.2 (CONTRACT D 3.3): {@code GET /api/v1/configuracion/reclamos} para ESTUDIANTE y DOCENTE. */
public interface ConsultarConfiguracionReclamosInputPort {

    ConfiguracionReclamosDTO execute(UUID actorUsuarioId);
}
