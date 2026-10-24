package co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.BitacoraPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.domain.ConsultarBitacoraEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarbitacoraestudiante.usecase.impl.ConsultarBitacoraEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.CoordinadorIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaBitacoraProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static co.edu.uco.asistenciasuco.application.features.coordinador.kpis.KpisTestData.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ConsultarBitacoraEstudianteUseCaseImplTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final CoordinadorIndicadoresQueryPort queryPort = mock(CoordinadorIndicadoresQueryPort.class);
    private final ConsultarBitacoraEstudianteUseCaseImpl useCase = new ConsultarBitacoraEstudianteUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void programaDelCoordinador() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(queryPort.consultarBitacora(any(), any(), any(), anyInt(), anyInt())).thenReturn(new PaginaBitacoraProjection(List.of(bitacora()), 25));
    }

    @Test
    void T_BIT_01_devuelve_la_pagina_y_conserva_asistio_nulo() {
        final BitacoraPaginaDomain result = useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, ESTUDIANTE, PERIODO, 3, 10));

        verify(queryPort).consultarBitacora(ACTOR, ESTUDIANTE, PERIODO, 3, 10);
        assertEquals(25, result.totalRegistros());
        assertEquals(7, result.sesiones().getFirst().numeroSesion());
        assertEquals(GRUPO, result.sesiones().getFirst().idGrupo());
        assertEquals("Estructuras de Datos", result.sesiones().getFirst().nombreAsignatura());
        assertNull(result.sesiones().getFirst().asistio());
    }

    @Test
    void aplica_pagina_1_y_tamano_10_por_defecto() {
        useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, ESTUDIANTE, null, null, null));

        verify(queryPort).consultarBitacora(ACTOR, ESTUDIANTE, null, 1, 10);
    }

    @Test
    void T_BE_02_valida_la_forma_antes_de_llamar_al_puerto() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, null, null, 1, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, ESTUDIANTE, null, 0, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, ESTUDIANTE, null, 1, 0)));
        verify(queryPort, never()).consultarBitacora(any(), any(), any(), anyInt(), anyInt());
    }

    @Test
    void T_BE_01_sin_programa_de_coordinador_responde_forbidden() {
        when(scopePort.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarBitacoraEstudianteDomain(ACTOR, ESTUDIANTE, null, 1, 10)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarBitacoraEstudianteUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarBitacoraEstudianteUseCaseImpl(scopePort, null));
    }
}
