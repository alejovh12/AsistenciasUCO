package co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase;

import co.edu.uco.asistenciasuco.application.features.docente.consultardocentes.usecase.entity.DocenteIdentidadEntity;
import co.edu.uco.asistenciasuco.application.usecase.UseCaseWithoutInputWithReturn;

import java.util.List;

public interface ConsultarDocentesUseCase extends UseCaseWithoutInputWithReturn<List<DocenteIdentidadEntity>> {
}
