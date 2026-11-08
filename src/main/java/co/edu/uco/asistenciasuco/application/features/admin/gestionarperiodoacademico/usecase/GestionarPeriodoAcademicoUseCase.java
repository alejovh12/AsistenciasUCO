package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain.PeriodoAcademicoDomain;

import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPeriodosAdminProjection;

import java.util.UUID;

public interface GestionarPeriodoAcademicoUseCase {

    UUID crear(PeriodoAcademicoDomain domain);

    PaginaPeriodosAdminProjection consultar(UUID usuario, Integer pagina, Integer tamano);

    void actualizar(PeriodoAcademicoDomain domain);

    void eliminar(UUID idPeriodo, UUID usuario);
}
