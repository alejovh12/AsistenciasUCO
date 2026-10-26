package co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase;

import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.domain.PeriodoAcademicoDomain;

import java.util.UUID;

public interface GestionarPeriodoAcademicoUseCase {

    void crear(PeriodoAcademicoDomain domain);

    void actualizar(PeriodoAcademicoDomain domain);

    void eliminar(UUID idPeriodo, UUID usuario);
}
