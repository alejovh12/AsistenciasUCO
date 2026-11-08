package co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.AudienciaRealtimeGrupo;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.usecase.ResolverAudienciaRealtimeUseCase;

import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort;

import java.util.Objects;

public final class ResolverAudienciaRealtimeUseCaseImpl implements ResolverAudienciaRealtimeUseCase {

    private final RealtimeAudienciaQueryPort consulta;

    public ResolverAudienciaRealtimeUseCaseImpl(final RealtimeAudienciaQueryPort consulta) {
        this.consulta = Objects.requireNonNull(consulta, "RealtimeAudienciaQueryPort es obligatorio.");
    }

    @Override
    public AudienciaRealtimeGrupo execute(final Comando comando) {
        Objects.requireNonNull(comando, "El comando es obligatorio.");
        final RealtimeAudienciaQueryPort.Audiencias a = consulta.consultar(comando.usuarioId(), comando.grupoId());
        return new AudienciaRealtimeGrupo(a.esDocenteTitular(), a.esEstudianteActivo(), a.esAmbitoLectura());
    }
}
