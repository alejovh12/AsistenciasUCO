package co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AdminIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.ConsultarTendenciaAusentismoUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.ConsultarTendenciaAusentismoDomain;
import co.edu.uco.asistenciasuco.application.features.admin.consultartendenciaausentismo.usecase.domain.PuntoTendenciaDomain;
import java.util.List;
import java.util.Set;
import java.util.Objects;

/** El ambito (institucion del administrador) lo fuerza la DB: aqui solo se valida la forma de la consulta. */
public final class ConsultarTendenciaAusentismoUseCaseImpl implements ConsultarTendenciaAusentismoUseCase {

    static final String GRANULARIDAD_POR_DEFECTO = "SEMANA";
    static final Set<String> GRANULARIDADES = Set.of("SEMANA", "MES");

    private final AdminIndicadoresQueryPort queryPort;

    public ConsultarTendenciaAusentismoUseCaseImpl(final AdminIndicadoresQueryPort queryPort) {
        this.queryPort = Objects.requireNonNull(queryPort, "AdminIndicadoresQueryPort es obligatorio.");
    }

    @Override
    public List<PuntoTendenciaDomain> execute(final ConsultarTendenciaAusentismoDomain domain) {
        if (domain == null) {
            throw new ValidationException("Los datos de la consulta son obligatorios.");
        }
        final String granularidad = domain.granularidad() == null || domain.granularidad().isBlank()
                ? GRANULARIDAD_POR_DEFECTO : domain.granularidad().trim().toUpperCase();
        if (!GRANULARIDADES.contains(granularidad)) {
            throw new ValidationException("La granularidad debe ser SEMANA o MES.");
        }
        return queryPort.consultarTendencia(domain.actorUsuarioId(), domain.idPeriodoAcademico(), granularidad).stream()
                .map(p -> new PuntoTendenciaDomain(p.inicioIntervalo(), p.finIntervalo(), p.sesionesEjecutadas(), p.faltasSinJustaCausa(), p.porcentajeAusentismo()))
                .toList();
    }
}
