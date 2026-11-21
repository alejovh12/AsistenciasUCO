package co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.CategoriaJustificacionAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarcategoriajustificacion.primaryports.dto.GuardarCategoriaJustificacionDTO;
import co.edu.uco.asistenciasuco.application.features.admin.common.dto.ResultadoCatalogoAdminDTO;

import java.util.List;
import java.util.UUID;

/** Administracion del catalogo de categorias de justificacion (solo ADMINISTRADOR). */
public interface GestionarCategoriaJustificacionInputPort {

    List<CategoriaJustificacionAdminDTO> consultar();

    ResultadoCatalogoAdminDTO crear(GuardarCategoriaJustificacionDTO dto);

    ResultadoCatalogoAdminDTO actualizar(GuardarCategoriaJustificacionDTO dto);

    ResultadoCatalogoAdminDTO cambiarEstado(UUID id, Boolean estaActiva, UUID usuarioEjecutor);
}
