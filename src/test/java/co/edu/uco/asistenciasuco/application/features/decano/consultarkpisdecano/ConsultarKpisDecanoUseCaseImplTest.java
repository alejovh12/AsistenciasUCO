package co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.ConsultarKpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.domain.KpisDecanoDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultarkpisdecano.usecase.impl.ConsultarKpisDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisDecanoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsultarKpisDecanoUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DecanoIndicadoresQueryPort queryPort = mock(DecanoIndicadoresQueryPort.class);
    private final ConsultarKpisDecanoUseCaseImpl useCase = new ConsultarKpisDecanoUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    private static KpisDecanoProjection kpis() {
        return new KpisDecanoProjection(FACULTAD, "Ingenieria", PERIODO, "2026-2", 3, 6, 2, 1, 3, new BigDecimal("50.00"),
                35, 6, new BigDecimal("17.14"), new BigDecimal("82.86"), 3, 55, 7, new BigDecimal("12.73"),
                new BigDecimal("15.00"), new BigDecimal("20.00"));
    }

    @Test
    void T_BE_01_devuelve_las_cifras_del_puerto_sin_recalcular() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        final KpisDecanoDomain r = useCase.execute(new ConsultarKpisDecanoDomain(ACTOR, PERIODO));

        assertEquals(FACULTAD, r.idFacultad());
        assertEquals("Ingenieria", r.nombreFacultad());
        assertEquals(3, r.totalProgramas());
        assertEquals(6, r.totalEstudiantes());
        assertEquals(2, r.estudiantesRegulares());
        assertEquals(1, r.estudiantesRiesgo());
        assertEquals(3, r.estudiantesPerdida());
        assertEquals(new BigDecimal("50.00"), r.tasaRetencion());
        assertEquals(35, r.sesionesEjecutadas());
        assertEquals(6, r.faltasSinJustaCausa());
        assertEquals(new BigDecimal("17.14"), r.porcentajeAusentismo());
        assertEquals(new BigDecimal("82.86"), r.porcentajeAsistencia());
        assertEquals(3, r.gruposActivos());
        assertEquals(55, r.cupoTotal());
        assertEquals(7, r.matriculadosActivos());
        assertEquals(new BigDecimal("12.73"), r.tasaAforo());
        assertEquals(new BigDecimal("15.00"), r.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), r.umbralPerdida());
        assertEquals("2026-2", r.nombrePeriodoAcademico());
    }

    @Test
    void T_BE_03_conserva_los_nulos_en_vez_de_inventar_valores() {
        when(queryPort.consultarKpis(ACTOR, null)).thenReturn(new KpisDecanoProjection(FACULTAD, "Ingenieria", null, null, 1, 0, 0, 0, 0, null,
                0, 0, null, null, 0, 0, 0, null, new BigDecimal("15.00"), new BigDecimal("20.00")));

        final KpisDecanoDomain r = useCase.execute(new ConsultarKpisDecanoDomain(ACTOR, null));

        assertNull(r.tasaRetencion());
        assertNull(r.porcentajeAusentismo());
        assertNull(r.porcentajeAsistencia());
        assertNull(r.tasaAforo());
        assertNull(r.idPeriodoAcademico());
    }

    @Test
    void T_BE_01_sin_facultad_de_decano_responde_forbidden_sin_tocar_la_db() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarKpisDecanoDomain(ACTOR, null)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void consulta_con_el_actor_y_el_periodo_indicados() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        useCase.execute(new ConsultarKpisDecanoDomain(ACTOR, PERIODO));

        verify(queryPort).consultarKpis(ACTOR, PERIODO);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarKpisDecanoUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarKpisDecanoUseCaseImpl(scopePort, null));
    }
}
