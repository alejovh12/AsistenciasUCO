package co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.usecase.ConsultarMotivosCancelacionUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;

import java.util.List;
import java.util.Objects;

/** CULM-001 / DEC-064: la DB valida el perfil COORDINADOR y devuelve los motivos activos en su orden. */
public final class ConsultarMotivosCancelacionUseCaseImpl implements ConsultarMotivosCancelacionUseCase {

    private final SesionManualRepositoryPort repositorio;

    public ConsultarMotivosCancelacionUseCaseImpl(final SesionManualRepositoryPort repositorio) {
        this.repositorio = Objects.requireNonNull(repositorio, "SesionManualRepositoryPort es obligatorio.");
    }

    @Override
    public List<Motivo> execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando para consultar los motivos es obligatorio.");
        return repositorio.consultarMotivosCancelacion(comando.usuarioEjecutor()).stream()
                .map(motivo -> new Motivo(motivo.codigo(), motivo.nombre()))
                .toList();
    }
}
