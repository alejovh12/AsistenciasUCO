package co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase;

import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.domain.ConfiguracionReclamosDomain;

import java.util.UUID;

public interface ConsultarConfiguracionReclamosUseCase {

    ConfiguracionReclamosDomain execute(UUID actorUsuarioId);
}
