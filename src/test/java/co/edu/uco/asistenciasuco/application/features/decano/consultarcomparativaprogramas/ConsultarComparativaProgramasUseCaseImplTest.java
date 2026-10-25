package co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ComparativaProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.domain.ConsultarComparativaProgramasDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarcomparativaprogramas.usecase.impl.ConsultarComparativaProgramasUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.ComparativaProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsultarComparativaProgramasUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID P1 = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID P2 = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID P4 = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DecanoIndicadoresQueryPort queryPort = mock(DecanoIndicadoresQueryPort.class);
    private final ConsultarComparativaProgramasUseCaseImpl useCase = new ConsultarComparativaProgramasUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    @Test
    void T_BE_01_conserva_filas_orden_y_cifras_del_puerto() {
        when(queryPort.consultarComparativa(ACTOR, PERIODO)).thenReturn(List.of(
                new ComparativaProgramaProjection(P2, "Programa 2", true, 1, 0, 1, new BigDecimal("0.00"), 2, 1, new BigDecimal("50.00")),
                new ComparativaProgramaProjection(P1, "Programa 1", true, 5, 1, 2, new BigDecimal("60.00"), 33, 5, new BigDecimal("15.15")),
                new ComparativaProgramaProjection(P4, "Programa 4", false, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaProgramaDomain> r = useCase.execute(new ConsultarComparativaProgramasDomain(ACTOR, PERIODO));

        assertEquals(List.of(P2, P1, P4), r.stream().map(ComparativaProgramaDomain::idPrograma).toList());
        assertEquals(new BigDecimal("50.00"), r.get(0).porcentajeAusentismo());
        assertEquals(5, r.get(1).totalEstudiantes());
        assertEquals(33, r.get(1).sesionesEjecutadas());
        assertEquals(new BigDecimal("60.00"), r.get(1).tasaRetencion());
        assertFalse(r.get(2).estaActivoPrograma());
        assertTrue(r.get(0).estaActivoPrograma());
    }

    @Test
    void T_BE_03_un_programa_sin_datos_conserva_nulos_y_no_se_oculta() {
        when(queryPort.consultarComparativa(ACTOR, null)).thenReturn(List.of(
                new ComparativaProgramaProjection(P4, "Programa 4", false, 0, 0, 0, null, 0, 0, null)));

        final List<ComparativaProgramaDomain> r = useCase.execute(new ConsultarComparativaProgramasDomain(ACTOR, null));

        assertEquals(1, r.size());
        assertNull(r.get(0).tasaRetencion());
        assertNull(r.get(0).porcentajeAusentismo());
    }

    @Test
    void sin_programas_devuelve_lista_vacia() {
        when(queryPort.consultarComparativa(ACTOR, null)).thenReturn(List.of());

        assertTrue(useCase.execute(new ConsultarComparativaProgramasDomain(ACTOR, null)).isEmpty());
    }

    @Test
    void T_BE_01_sin_facultad_de_decano_responde_forbidden_sin_tocar_la_db() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarComparativaProgramasDomain(ACTOR, null)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarComparativaProgramasUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarComparativaProgramasUseCaseImpl(scopePort, null));
    }
}
