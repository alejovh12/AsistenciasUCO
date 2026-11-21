package co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.MotivoCancelacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarmotivocancelacion.primaryports.dto.GuardarMotivoCancelacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;

import java.util.List;
import java.util.UUID;

/** Administracion del catalogo de motivos de cancelacion de sesion (solo ADMINISTRADOR). */
public interface GestionarMotivoCancelacionInputPort {

    List<MotivoCancelacionAdminDTO> consultar();

    ResultadoCatalogoAdminDTO crear(GuardarMotivoCancelacionDTO dto);

    ResultadoCatalogoAdminDTO actualizar(GuardarMotivoCancelacionDTO dto);

    ResultadoCatalogoAdminDTO cambiarEstado(UUID id, Boolean estaActivo, UUID usuarioEjecutor);
}
