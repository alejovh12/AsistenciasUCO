package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.PaginaPeriodosAdminDTO;

import java.util.UUID;

/** HU096/HU097/HU098: gestion de periodos academicos (solo ADMINISTRADOR: el periodo es unico por institucion). */
public interface GestionarPeriodoAcademicoInputPort {

    /** Devuelve el identificador del periodo creado (CULM-001 D.9-a, F-ADM-030). */
    UUID crear(GuardarPeriodoAcademicoDTO dto);

    PaginaPeriodosAdminDTO consultar(UUID usuario, Integer pagina, Integer tamano);

    void actualizar(GuardarPeriodoAcademicoDTO dto);

    void eliminar(UUID idPeriodo, UUID usuario);
}
