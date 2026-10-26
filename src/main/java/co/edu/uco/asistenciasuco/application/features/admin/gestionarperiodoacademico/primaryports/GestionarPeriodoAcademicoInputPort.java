package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.GuardarPeriodoAcademicoDTO;

import java.util.UUID;

/** HU096/HU097/HU098: gestion de periodos academicos (solo ADMINISTRADOR: el periodo es unico por institucion). */
public interface GestionarPeriodoAcademicoInputPort {

    void crear(GuardarPeriodoAcademicoDTO dto);

    void actualizar(GuardarPeriodoAcademicoDTO dto);

    void eliminar(UUID idPeriodo, UUID usuario);
}
