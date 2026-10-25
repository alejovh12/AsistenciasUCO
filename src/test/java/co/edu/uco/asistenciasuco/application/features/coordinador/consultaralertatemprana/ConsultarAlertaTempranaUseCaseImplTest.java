package co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.AlertasPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.domain.ConsultarAlertaTempranaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultaralertatemprana.usecase.impl.ConsultarAlertaTempranaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAlertasProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.coordinador.kpis.KpisTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarAlertaTempranaUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorIndicadoresQueryPort queryPort = mock(CoordinadorIndicadoresQueryPort.class);
    private final ConsultarAlertaTempranaUseCaseImpl useCase = new ConsultarAlertaTempranaUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void programaDelCoordinador() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarAlertas(any(), any(), any(), any(), anyInt(), anyInt())).thenReturn(new PaginaAlertasProjection(List.of(alerta()), 5));
    }

    @Test
    void T_ALT_01_devuelve_la_pagina_con_el_total_y_las_cifras_del_puerto() {
        final AlertasPaginaDomain result = useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, PERIODO, "ROJO", " Ana ", 2, 5));

        verify(queryPort).consultarAlertas(ACTOR, PERIODO, "ROJO", "Ana", 2, 5);
        assertEquals(5, result.totalRegistros());
        assertEquals(1, result.alertas().size());
        assertEquals(ESTUDIANTE, result.alertas().getFirst().idEstudiante());
        assertEquals(new BigDecimal("66.67"), result.alertas().getFirst().porcentajeInasistencia());
        assertEquals("ROJO", result.alertas().getFirst().semaforo());
        assertEquals("PERDIDA", result.alertas().getFirst().codigoPermanencia());
        assertEquals(2, result.alertas().getFirst().totalMatriculasActivas());
    }

    @Test
    void aplica_pagina_1_y_tamano_10_por_defecto_y_normaliza_filtros_vacios() {
        useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, null, "   ", null, null));

        verify(queryPort).consultarAlertas(ACTOR, null, null, null, 1, 10);
    }

    @ParameterizedTest
    @ValueSource(strings = {"VERDE", "AMBAR", "ROJO"})
    void acepta_los_tres_semaforos(final String semaforo) {
        useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, semaforo, null, 1, 10));

        verify(queryPort).consultarAlertas(ACTOR, null, semaforo, null, 1, 10);
    }

    @Test
    void T_BE_02_valida_la_forma_antes_de_llamar_al_puerto() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, "AZUL", null, 1, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, null, null, 0, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, null, null, 1, 0)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, null, null, -3, -1)));
        verify(queryPort, never()).consultarAlertas(any(), any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void T_BE_01_sin_programa_de_coordinador_responde_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarAlertaTempranaDomain(ACTOR, null, null, null, 1, 10)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarAlertaTempranaUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarAlertaTempranaUseCaseImpl(scopePort, null));
    }
}
