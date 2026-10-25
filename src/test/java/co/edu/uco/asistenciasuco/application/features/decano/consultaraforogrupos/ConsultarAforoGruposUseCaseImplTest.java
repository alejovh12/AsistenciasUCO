package co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.AforoGruposPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.domain.ConsultarAforoGruposDomain;
import co.edu.uco.asistenciasuco.application.features.decano.consultaraforogrupos.usecase.impl.ConsultarAforoGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DecanoIndicadoresQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AforoGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaAforoGruposProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ConsultarAforoGruposUseCaseImplTest {

    private static final UUID ACTOR = UUID.fromString("11111111-1111-1111-1111-111111111111");
    private static final UUID FACULTAD = UUID.fromString("22222222-2222-2222-2222-222222222222");
    private static final UUID PERIODO = UUID.fromString("33333333-3333-3333-3333-333333333333");
    private static final UUID PROGRAMA = UUID.fromString("44444444-4444-4444-4444-444444444444");
    private static final UUID GRUPO = UUID.fromString("55555555-5555-5555-5555-555555555555");
    private static final UUID ASIGNATURA = UUID.fromString("66666666-6666-6666-6666-666666666666");

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final DecanoIndicadoresQueryPort queryPort = mock(DecanoIndicadoresQueryPort.class);
    private final ConsultarAforoGruposUseCaseImpl useCase = new ConsultarAforoGruposUseCaseImpl(scopePort, queryPort);

    @BeforeEach
    void facultadDelDecano() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.of(FACULTAD));
    }

    private static PaginaAforoGruposProjection pagina() {
        return new PaginaAforoGruposProjection(List.of(
                new AforoGrupoProjection(GRUPO, 931001, ASIGNATURA, "Asignatura Uno", PROGRAMA, "Programa 1", 10, 5, 5, new BigDecimal("50.00"), true),
                new AforoGrupoProjection(UUID.randomUUID(), 931002, ASIGNATURA, "Asignatura Dos", PROGRAMA, "Programa 1", 0, 0, 0, null, false)), 12);
    }

    @Test
    void T_BE_01_devuelve_filas_y_total_del_puerto_sin_recalcular() {
        when(queryPort.consultarAforo(ACTOR, PERIODO, PROGRAMA, true, 2, 5)).thenReturn(pagina());

        final AforoGruposPaginaDomain r = useCase.execute(new ConsultarAforoGruposDomain(ACTOR, PERIODO, PROGRAMA, true, 2, 5));

        assertEquals(12, r.totalRegistros());
        assertEquals(2, r.grupos().size());
        assertEquals(10, r.grupos().get(0).cupoMaximo());
        assertEquals(5, r.grupos().get(0).matriculadosActivos());
        assertEquals(5, r.grupos().get(0).cuposDisponibles());
        assertEquals(new BigDecimal("50.00"), r.grupos().get(0).tasaAforo());
        assertEquals("Asignatura Uno", r.grupos().get(0).nombreAsignatura());
    }

    @Test
    void T_BE_03_cupo_cero_conserva_tasa_nula() {
        when(queryPort.consultarAforo(ACTOR, null, null, true, 1, 10)).thenReturn(pagina());

        final AforoGruposPaginaDomain r = useCase.execute(new ConsultarAforoGruposDomain(ACTOR, null, null, null, null, null));

        assertNull(r.grupos().get(1).tasaAforo());
        assertEquals(false, r.grupos().get(1).grupoEstaHabilitado());
    }

    @Test
    void los_valores_por_defecto_son_pagina_1_tamano_10_y_solo_habilitados() {
        when(queryPort.consultarAforo(ACTOR, null, null, true, 1, 10)).thenReturn(pagina());

        useCase.execute(new ConsultarAforoGruposDomain(ACTOR, null, null, null, null, null));

        verify(queryPort).consultarAforo(ACTOR, null, null, true, 1, 10);
    }

    @Test
    void solo_habilitados_false_se_respeta() {
        when(queryPort.consultarAforo(ACTOR, PERIODO, null, false, 1, 10)).thenReturn(pagina());

        useCase.execute(new ConsultarAforoGruposDomain(ACTOR, PERIODO, null, false, 1, 10));

        verify(queryPort).consultarAforo(ACTOR, PERIODO, null, false, 1, 10);
    }

    @Test
    void pagina_menor_a_uno_es_error_de_validacion_sin_tocar_la_db() {
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAforoGruposDomain(ACTOR, null, null, null, 0, 10)));
        verifyNoInteractions(queryPort);
    }

    @Test
    void tamano_menor_a_uno_es_error_de_validacion_sin_tocar_la_db() {
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarAforoGruposDomain(ACTOR, null, null, null, 1, 0)));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_BE_01_sin_facultad_de_decano_responde_forbidden_sin_tocar_la_db() {
        when(scopePort.findFacultadIdByDecanoUsuario(ACTOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(new ConsultarAforoGruposDomain(ACTOR, null, null, null, 1, 10)));

        verifyNoInteractions(queryPort);
    }

    @Test
    void el_dominio_es_obligatorio() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new ConsultarAforoGruposUseCaseImpl(null, queryPort));
        assertThrows(NullPointerException.class, () -> new ConsultarAforoGruposUseCaseImpl(scopePort, null));
    }
}
