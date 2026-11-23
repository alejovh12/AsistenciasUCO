package co.edu.uco.asistenciasuco.application.features.decano;

import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.dto.ComparativaProgramaDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.primaryports.mapper.ConsultarComparativaProgramasMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ConsultarComparativaProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.impl.ConsultarComparativaProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.dto.KpisDecanoDTO;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.primaryports.mapper.ConsultarKpisDecanoMapper;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl.ConsultarKpisDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Revision final BD-7: la DB agrega al final horasPeriodo (la base real del % de ausentismo) a los KPIs y a la comparativa del decano; el
 * backend la entrega sin recalcular para que la etiqueta diga "X h de falta sobre Y h del periodo".
 */
class HorasPeriodoDecanoTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final BigDecimal DIEZ = new BigDecimal("10.00");
    private static final BigDecimal PERIODO = new BigDecimal("1940.00");

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final DecanoIndicadoresQueryPort port = mock(DecanoIndicadoresQueryPort.class);

    @Test
    void los_kpis_propagan_horas_periodo_hasta_el_dto() {
        when(scope.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarKpis(ACTOR, null)).thenReturn(new KpisDecanoProjection(UUID.randomUUID(), "F", null, null, 1, 2, 1, 1, 0,
                DIEZ, 3, 1, DIEZ, DIEZ, 1, 40, 2, DIEZ, DIEZ, DIEZ, 7, new BigDecimal("14.00"), new BigDecimal("2.00"), 0, 0, PERIODO));

        final KpisDecanoDomain domain = new ConsultarKpisDecanoUseCaseImpl(scope, port).execute(new ConsultarKpisDecanoDomain(ACTOR, null));
        final KpisDecanoDTO dto = ConsultarKpisDecanoMapper.toDto(domain);

        assertEquals(PERIODO, dto.horasPeriodo());
        assertNull(new KpisDecanoDTO(null, null, null, null, 0, 0, 0, 0, 0, null, 0, 0, null, null, 0, 0, 0, null, null, null).horasPeriodo());
    }

    @Test
    void la_comparativa_propaga_horas_periodo_hasta_el_dto() {
        when(scope.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarComparativa(ACTOR, null)).thenReturn(List.of(new ComparativaProgramaProjection(UUID.randomUUID(), "P", true, 5, 1, 2,
                DIEZ, 33, 5, DIEZ, new BigDecimal("120.00"), new BigDecimal("18.00"), PERIODO)));

        final List<ComparativaProgramaDomain> domains = new ConsultarComparativaProgramasUseCaseImpl(scope, port)
                .execute(new ConsultarComparativaProgramasDomain(ACTOR, null));
        final ComparativaProgramaDTO dto = ConsultarComparativaProgramasMapper.toDto(domains).getFirst();

        assertEquals(PERIODO, dto.horasPeriodo());
    }
}
