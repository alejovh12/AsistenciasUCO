package co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.ConsultarKpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.domain.KpisCoordinadorDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarkpis.usecase.impl.ConsultarKpisCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.coordinador.kpis.KpisTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarKpisCoordinadorUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorIndicadoresQueryPort queryPort = mock(CoordinadorIndicadoresQueryPort.class);
    private final ConsultarKpisCoordinadorUseCaseImpl useCase = new ConsultarKpisCoordinadorUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void programaDelCoordinador() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
    }

    @Test
    void T_KPI_01_devuelve_las_cifras_del_puerto_sin_recalcular() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        final KpisCoordinadorDomain result = useCase.execute(new ConsultarKpisCoordinadorDomain(ACTOR, PERIODO));

        assertEquals(5, result.totalEstudiantes());
        assertEquals(2, result.estudiantesRegulares());
        assertEquals(1, result.estudiantesRiesgo());
        assertEquals(2, result.estudiantesPerdida());
        assertEquals(new BigDecimal("60.00"), result.tasaPermanencia());
        assertEquals(15, result.sesionesProgramadas());
        assertEquals(10, result.sesionesVencidas());
        assertEquals(9, result.sesionesEjecutadas());
        assertEquals(1, result.sesionesVencidasSinRegistro());
        assertEquals(1, result.sesionesSinProcedenciaTemporal());
        assertEquals(new BigDecimal("90.00"), result.porcentajeCumplimiento());
        assertEquals(new BigDecimal("66.67"), result.porcentajeProgresoCalendario());
        assertEquals(new BigDecimal("15.00"), result.umbralAdvertencia());
        assertEquals(new BigDecimal("20.00"), result.umbralPerdida());
        assertEquals("2026-2", result.nombrePeriodoAcademico());
    }

    @Test
    void T_BE_04_conserva_los_nulos_en_vez_de_inventar_valores() {
        final var sinDatos = new co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.KpisCoordinadorProjection(
                PROGRAMA, "Programa", null, null, 0, 0, 0, 0, null, 0, 0, 0, 0, 0, null, null, new BigDecimal("15.00"), new BigDecimal("20.00"));
        when(queryPort.consultarKpis(ACTOR, null)).thenReturn(sinDatos);

        final KpisCoordinadorDomain result = useCase.execute(new ConsultarKpisCoordinadorDomain(ACTOR, null));

        assertNull(result.tasaPermanencia());
        assertNull(result.porcentajeCumplimiento());
        assertNull(result.porcentajeProgresoCalendario());
        assertNull(result.idPeriodoAcademico());
    }

    @Test
    void T_BE_01_sin_programa_de_coordinador_responde_forbidden_sin_tocar_la_db() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarKpisCoordinadorDomain(ACTOR, null)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void consulta_con_el_actor_y_el_periodo_indicados() {
        when(queryPort.consultarKpis(ACTOR, PERIODO)).thenReturn(kpis());

        useCase.execute(new ConsultarKpisCoordinadorDomain(ACTOR, PERIODO));

        verify(queryPort).consultarKpis(ACTOR, PERIODO);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarKpisCoordinadorUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarKpisCoordinadorUseCaseImpl(scopePort, null));
    }
}
