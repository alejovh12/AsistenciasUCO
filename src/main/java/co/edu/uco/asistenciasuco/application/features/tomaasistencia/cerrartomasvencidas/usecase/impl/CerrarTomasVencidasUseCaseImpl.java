package co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.cerrartomasvencidas.usecase.CerrarTomasVencidasUseCase;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaEventos;
import co.edu.uco.asistenciasuco.application.features.tomaasistencia.common.TomaAsistenciaResumen;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.TomaAsistenciaRepositoryPort;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * CULM-001 B.2: cierre automatico. La base de datos decide que tomas cerrar (multi-instancia segura con {@code READPAST})
 * y marca ausentes segun el modo; por cada toma cerrada se publica {@code TOMA_CERRADA} con {@code automatico = true}.
 * {@code totalFaltantes} no viaja en el cierre automatico: el SP del job no lo calcula.
 */
public final class CerrarTomasVencidasUseCaseImpl implements CerrarTomasVencidasUseCase {

    private final TomaAsistenciaRepositoryPort repositorio;
    private final TomaAsistenciaEventos eventos;
    private final EvaluadorAlertasInasistencia evaluador;

    public CerrarTomasVencidasUseCaseImpl(final TomaAsistenciaRepositoryPort repositorio, final TomaAsistenciaEventos eventos) {
        this(repositorio, eventos, EvaluadorAlertasInasistencia.NINGUNO);
    }

    /** CULM-001 C.1: con evaluador, cada grupo con tomas cerradas se evalua una vez (SUPUESTO-C-04). */
    public CerrarTomasVencidasUseCaseImpl(final TomaAsistenciaRepositoryPort repositorio, final TomaAsistenciaEventos eventos,
                                          final EvaluadorAlertasInasistencia evaluador) {
        this.evaluador = Objects.requireNonNull(evaluador, "EvaluadorAlertasInasistencia es obligatorio.");
        this.repositorio = Objects.requireNonNull(repositorio, "TomaAsistenciaRepositoryPort es obligatorio.");
        this.eventos = Objects.requireNonNull(eventos, "TomaAsistenciaEventos es obligatorio.");
    }

    @Override
    public int execute() {
        final List<TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente> cerradas = repositorio.cerrarVencidas();
        for (final TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente cerrada : cerradas) {
            final Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("grupo", cerrada.idGrupo().toString());
            payload.put("sesion", cerrada.idSesion().toString());
            payload.put("toma", cerrada.idToma().toString());
            numero(cerrada).ifPresent(numero -> payload.put("numero", numero));
            payload.put("totalAusentesMarcados", cerrada.totalAusentesMarcados());
            payload.put("automatico", true);
            eventos.publicar(TomaAsistenciaEventos.TOMA_CERRADA, payload);
        }
        cerradas.stream().map(TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente::idGrupo).distinct()
                .forEach(grupo -> evaluador.evaluarYAvisar(grupo, null));
        return cerradas.size();
    }

    private Optional<Integer> numero(final TomaAsistenciaRepositoryPort.TomaCerradaAutomaticamente cerrada) {
        try {
            return repositorio.consultarToma(cerrada.idToma()).map(TomaAsistenciaResumen::numero);
        } catch (final RuntimeException _) {
            return Optional.empty();
        }
    }
}
