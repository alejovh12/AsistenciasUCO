package co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarresumensesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.ResumenTomasSesionFila;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.SesionTitularAcceso;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.consultarresumensesion.usecase.ConsultarResumenTomasSesionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;

import java.util.List;
import java.util.Objects;

/** PC: resumen por estudiante de una sesion (titularidad en Application y en el SP {@code usp_consultar_resumen_tomas_sesion}). */
public final class ConsultarResumenTomasSesionUseCaseImpl implements ConsultarResumenTomasSesionUseCase {

    private final SesionTitularAcceso sesiones;
    private final TomaAsistenciaRepositoryPort repositorio;

    public ConsultarResumenTomasSesionUseCaseImpl(final SesionTitularAcceso sesiones, final TomaAsistenciaRepositoryPort repositorio) {
        this.sesiones = Objects.requireNonNull(sesiones, "SesionTitularAcceso es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
    }

    @Override
    public List<ResumenTomasSesionFila> execute(final Consulta consulta) {
        Objects.requireNonNull(consulta, "La consulta del resumen de tomas de la sesion es obligatoria.");
        sesiones.exigirTitular(consulta.sesionId(), consulta.usuarioEjecutor());
        return repositorio.consultarResumenTomasSesion(consulta.sesionId(), consulta.usuarioEjecutor());
    }
}
