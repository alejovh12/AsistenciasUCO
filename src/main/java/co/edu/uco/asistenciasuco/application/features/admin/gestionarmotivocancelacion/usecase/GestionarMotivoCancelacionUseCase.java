package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.usecase.domain.MotivoCancelacionDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.MotivoCancelacionAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ResultadoCatalogoAdminProjection;

import java.util.List;
import java.util.UUID;

public interface GestionarMotivoCancelacionUseCase {

    List<MotivoCancelacionAdminProjection> consultar();

    ResultadoCatalogoAdminProjection crear(MotivoCancelacionDomain domain);

    ResultadoCatalogoAdminProjection actualizar(MotivoCancelacionDomain domain);

    ResultadoCatalogoAdminProjection cambiarEstado(UUID id, Boolean estaActivo, UUID usuarioEjecutor);
}
