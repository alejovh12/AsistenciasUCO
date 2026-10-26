package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor.ConsultarAsistenciaProgramaInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.AsistenciaSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.GrupoProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.SesionGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU064/HU060/HU061: reglas de la consulta de solo lectura del coordinador. */
class ConsultarAsistenciaProgramaUseCaseImplTest {

    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID SESION = UUID.randomUUID();

    private final AsistenciaProgramaQueryPort queryPort = mock(AsistenciaProgramaQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private ConsultarAsistenciaProgramaUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        useCase = new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, scopePort);
        when(scopePort.findProgramaIdByCoordinadorUsuario(COORDINADOR)).thenReturn(Optional.of(PROGRAMA));
    }

    @Test
    void constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciaProgramaUseCaseImpl(null, scopePort));
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, null));
    }

    @Test
    void T_064_01_grupos_aplica_valores_por_defecto_y_normaliza_busqueda() {
        final Pagina<GrupoProgramaProjection> pagina = new Pagina<>(List.of(grupo()), 1);
        when(queryPort.consultarGrupos(COORDINADOR, PERIODO, "sis", 1, 10)).thenReturn(pagina);

        assertEquals(pagina, useCase.consultarGrupos(COORDINADOR, PERIODO, "  sis ", null, null));
    }

    @Test
    void T_064_02_grupos_busqueda_en_blanco_se_envia_nula() {
        when(queryPort.consultarGrupos(COORDINADOR, null, null, 2, 5)).thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarGrupos(COORDINADOR, null, "   ", 2, 5);

        verify(queryPort).consultarGrupos(COORDINADOR, null, null, 2, 5);
    }

    @Test
    void T_064_03_usuario_sin_programa_de_coordinador_es_403() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.consultarGrupos(otro, null, null, 1, 10));
        assertThrows(ForbiddenException.class, () -> useCase.consultarGrupos(null, null, null, 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_064_04_paginacion_fuera_de_rango_es_400() {
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 0, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 1, 0));
        assertThrows(ValidationException.class, () -> useCase.consultarGrupos(COORDINADOR, null, null, 1, 101));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_060_01_sesiones_envia_rango_de_fechas() {
        final LocalDate desde = LocalDate.of(2026, 2, 1);
        final LocalDate hasta = LocalDate.of(2026, 3, 1);
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, desde, hasta, 1, 100))
                .thenReturn(new Pagina<>(List.of(sesion()), 1));

        assertEquals(1, useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, desde, hasta, 1, 100).total());
    }

    @Test
    void T_060_02_sesiones_exige_grupo_y_rango_coherente() {
        assertThrows(ValidationException.class,
                () -> useCase.consultarSesionesGrupo(COORDINADOR, null, null, null, 1, 10));
        assertThrows(ValidationException.class, () -> useCase.consultarSesionesGrupo(COORDINADOR, GRUPO,
                LocalDate.of(2026, 3, 2), LocalDate.of(2026, 3, 1), 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void T_060_03_sesiones_con_una_sola_fecha_es_valido() {
        when(queryPort.consultarSesionesGrupo(any(), any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(), 0));

        useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, LocalDate.of(2026, 3, 2), null, null, null);
        useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, LocalDate.of(2026, 3, 2), null, null);

        verify(queryPort).consultarSesionesGrupo(COORDINADOR, GRUPO, LocalDate.of(2026, 3, 2), null, 1, 10);
    }

    @Test
    void T_061_01_asistencias_envia_filtro_asistio() {
        when(queryPort.consultarAsistenciasSesion(COORDINADOR, SESION, false, 1, 10))
                .thenReturn(new Pagina<>(List.of(asistencia()), 1));

        assertFalse(useCase.consultarAsistenciasSesion(COORDINADOR, SESION, false, null, null).elementos().getFirst()
                .asistio());
    }

    @Test
    void T_061_02_asistencias_exige_sesion() {
        assertThrows(ValidationException.class,
                () -> useCase.consultarAsistenciasSesion(COORDINADOR, null, null, 1, 10));
        verifyNoInteractions(queryPort);
    }

    @Test
    void interactor_mapea_proyecciones_a_dto_con_pagina_y_tamano() {
        final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);
        when(queryPort.consultarGrupos(COORDINADOR, null, null, 1, 10)).thenReturn(new Pagina<>(List.of(grupo()), 7));
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 2, 5))
                .thenReturn(new Pagina<>(List.of(sesion()), 6));
        when(queryPort.consultarAsistenciasSesion(any(), any(), any(), anyInt(), anyInt()))
                .thenReturn(new Pagina<>(List.of(asistencia()), 1));

        final var grupos = interactor.consultarGrupos(COORDINADOR, null, null, null, null);
        assertEquals(7, grupos.total());
        assertEquals(1, grupos.pagina());
        assertEquals(10, grupos.tamano());
        assertEquals("Grupo 1", grupos.elementos().getFirst().nombre());
        assertTrue(grupos.elementos().getFirst().habilitado());

        final var sesiones = interactor.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 2, 5);
        assertEquals(2, sesiones.pagina());
        assertEquals("S-01", sesiones.elementos().getFirst().codigo());

        final var asistencias = interactor.consultarAsistenciasSesion(COORDINADOR, SESION, null, null, null);
        assertEquals("Ana Ruiz", asistencias.elementos().getFirst().nombreCompleto());
        assertEquals("INJ", asistencias.elementos().getFirst().codigoRazonCausa());
    }

    @Test
    void interactor_rechaza_use_case_nulo() {
        assertThrows(NullPointerException.class, () -> new ConsultarAsistenciaProgramaInteractor(null));
    }

    private static GrupoProgramaProjection grupo() {
        return new GrupoProgramaProjection(GRUPO, 1, "Grupo 1", UUID.randomUUID(), "Calculo", UUID.randomUUID(), 30, 20,
                10, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 5));
    }

    private static SesionGrupoProjection sesion() {
        return new SesionGrupoProjection(SESION, GRUPO, 1, "Grupo 1", "Sesion 1", 1, "S-01", 2,
                LocalDateTime.of(2026, 2, 3, 7, 0), LocalDateTime.of(2026, 2, 3, 9, 0));
    }

    private static AsistenciaSesionProjection asistencia() {
        return new AsistenciaSesionProjection(UUID.randomUUID(), SESION, "Sesion 1", UUID.randomUUID(), "1017000111",
                "Ana Ruiz", false, "INJ", "Inasistencia injustificada", LocalDateTime.of(2026, 2, 3, 7, 0),
                LocalDateTime.of(2026, 2, 3, 9, 0));
    }
}
