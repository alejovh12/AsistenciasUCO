package co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.primaryports.interactor.ConsultarAsistenciaProgramaInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarasistenciaprograma.usecase.domain.SesionGrupoHorarioDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.Pagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaProgramaQueryPort.SesionGrupoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.CancelacionSesionProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PAG-COORD-SESIONES (RED): la procedencia temporal de la pagina de sesiones de un grupo se pide una sola vez y solo
 * para los ids de las sesiones de esa pagina ({@code consultarSesionesPorIds}); nunca se lee todo el grupo
 * ({@code consultarSesionesPorGrupo}). El contrato publico (HTTP, puertos, estadoTemporal, cancelacion, orden, total)
 * no cambia.
 */
@DisplayName("PAG-COORD-SESIONES: procedencia acotada a la pagina de sesiones del grupo")
class ConsultarAsistenciaProgramaSesionesAcotadasTest {

    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID OTRO_GRUPO = UUID.randomUUID();

    private static final LocalDateTime INICIO_V1 = LocalDateTime.of(2026, 2, 3, 7, 0);
    private static final LocalDateTime FIN_V1 = LocalDateTime.of(2026, 2, 3, 9, 0);

    private final AsistenciaProgramaQueryPort queryPort = mock(AsistenciaProgramaQueryPort.class);
    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private ConsultarAsistenciaProgramaUseCaseImpl useCase;

    @BeforeEach
    void preparar() {
        useCase = new ConsultarAsistenciaProgramaUseCaseImpl(queryPort, scopePort, procedenciaPort);
        when(scopePort.findProgramaIdByCoordinadorUsuario(COORDINADOR)).thenReturn(Optional.of(PROGRAMA));
    }

    private void paginaDe(final List<SesionGrupoProjection> sesiones, final int total) {
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 10))
                .thenReturn(new Pagina<>(sesiones, total));
    }

    private List<SesionGrupoHorarioDomain> consultar() {
        return useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null).elementos();
    }

    @SuppressWarnings("unchecked")
    private Collection<UUID> idsEnviadosAlPuerto() {
        final ArgumentCaptor<Collection<UUID>> captor = ArgumentCaptor.forClass(Collection.class);
        verify(procedenciaPort, times(1)).consultarSesionesPorIds(captor.capture());
        return captor.getValue();
    }

    @Test
    @DisplayName("pagina vacia: no se consulta ninguna procedencia (ni por ids ni por grupo)")
    void pagina_vacia_no_interactua_con_el_puerto_de_procedencia() {
        paginaDe(List.of(), 0);

        final var resultado = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null);

        assertEquals(List.of(), resultado.elementos());
        assertEquals(0, resultado.total());
        verifyNoInteractions(procedenciaPort);
    }

    @Test
    @DisplayName("pagina de 100 sesiones: una sola llamada por ids con los 100 ids, nunca por grupo completo")
    void pagina_de_cien_sesiones_hace_una_sola_consulta_por_ids() {
        final List<UUID> ids = IntStream.rangeClosed(1, 100).mapToObj(i -> UUID.randomUUID()).toList();
        when(queryPort.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 100))
                .thenReturn(new Pagina<>(ids.stream().map(ConsultarAsistenciaProgramaSesionesAcotadasTest::sesion).toList(), 340));
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of());

        final var resultado = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, 1, 100);

        assertEquals(100, resultado.elementos().size());
        assertEquals(ids, new ArrayList<>(idsEnviadosAlPuerto()));
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
        verify(procedenciaPort, never()).consultarSesion(any());
    }

    @Test
    @DisplayName("ids repetidos en la pagina se piden una sola vez, conservando el orden de primera aparicion")
    void ids_repetidos_se_piden_una_sola_vez() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        paginaDe(List.of(sesion(a), sesion(b), sesion(a)), 3);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of());

        final var resultado = consultar();

        assertEquals(List.of(a, b), new ArrayList<>(idsEnviadosAlPuerto()));
        assertEquals(List.of(a, b, a), resultado.stream().map(SesionGrupoHorarioDomain::id).toList());
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    @Test
    @DisplayName("un id nulo en la proyeccion nunca se envia al puerto de procedencia")
    void id_nulo_no_se_envia_al_puerto() {
        final UUID a = UUID.randomUUID();
        final AtomicReference<List<UUID>> recibidos = new AtomicReference<>();
        when(procedenciaPort.consultarSesionesPorIds(any())).thenAnswer(invocation -> {
            recibidos.set(new ArrayList<>(invocation.<Collection<UUID>>getArgument(0)));
            return List.of();
        });
        paginaDe(List.of(sesion(a), sesion(null)), 2);

        // SesionGrupoHorarioDomain exige id: la fila sin id falla al construirse, despues de pedir la procedencia.
        assertThrows(NullPointerException.class, this::consultar);

        assertNotNull(recibidos.get(), "se debe consultar la procedencia de los ids validos de la pagina");
        assertEquals(List.of(a), recibidos.get());
        assertFalse(recibidos.get().contains(null));
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    @Test
    @DisplayName("una procedencia de otro grupo devuelta por el puerto se ignora: sesion INDETERMINADA sin instantes ni cancelacion")
    void procedencia_de_otro_grupo_se_ignora() {
        final UUID ajena = UUID.randomUUID();
        final UUID propia = UUID.randomUUID();
        paginaDe(List.of(sesion(ajena), sesion(propia)), 2);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(ajena, OTRO_GRUPO, LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0),
                        "UTC_V2", new CancelacionSesionProjection("DOCENTE", "Ajena", null,
                                LocalDateTime.of(2026, 1, 2, 5, 0))),
                procedencia(propia, GRUPO, LocalDateTime.of(2026, 2, 4, 12, 0), LocalDateTime.of(2026, 2, 4, 14, 0),
                        "UTC_V2", null)));

        final var resultado = consultar();

        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.get(0).estadoTemporal());
        assertNull(resultado.get(0).fechaHoraInicioUtc());
        assertNull(resultado.get(0).fechaHoraFinUtc());
        assertNull(resultado.get(0).cancelacion());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(1).estadoTemporal());
        assertEquals(LocalDateTime.of(2026, 2, 4, 12, 0), resultado.get(1).fechaHoraInicioUtc());
        verify(procedenciaPort, times(1)).consultarSesionesPorIds(any());
    }

    @Test
    @DisplayName("una sesion cancelada expone estado CANCELADA y su cancelacion; la programada de la misma pagina no")
    void sesion_cancelada_expone_estado_y_cancelacion() {
        final ConsultarAsistenciaProgramaInteractor interactor = new ConsultarAsistenciaProgramaInteractor(useCase);
        final UUID cancelada = UUID.randomUUID();
        final UUID programada = UUID.randomUUID();
        paginaDe(List.of(sesion(cancelada), sesion(programada)), 2);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(cancelada, GRUPO, LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0),
                        "UTC_V2", new CancelacionSesionProjection("FESTIVO", "Dia festivo: San Jose", "San Jose",
                                LocalDateTime.of(2026, 1, 2, 5, 0))),
                procedencia(programada, GRUPO, LocalDateTime.of(2026, 2, 4, 12, 0), LocalDateTime.of(2026, 2, 4, 14, 0),
                        "UTC_V2", null)));

        final var dto = interactor.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null).elementos();

        assertEquals("CANCELADA", dto.get(0).estado());
        assertEquals("FESTIVO", dto.get(0).cancelacion().tipo());
        assertEquals("San Jose", dto.get(0).cancelacion().nombreFestivo());
        assertEquals(Instant.parse("2026-01-02T05:00:00Z"), dto.get(0).cancelacion().fechaCancelacionUtc());
        assertEquals("CONFIRMADA", dto.get(0).estadoTemporal());
        assertEquals("PROGRAMADA", dto.get(1).estado());
        assertNull(dto.get(1).cancelacion());
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    @Test
    @DisplayName("el orden de la pagina se conserva y cada sesion recibe su propia procedencia aunque el puerto responda en otro orden")
    void orden_de_la_pagina_se_conserva() {
        final UUID tercera = UUID.randomUUID();
        final UUID primera = UUID.randomUUID();
        final UUID segunda = UUID.randomUUID();
        paginaDe(List.of(sesion(tercera), sesion(primera), sesion(segunda)), 3);
        // El puerto responde en orden inverso al de la pagina.
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(segunda, GRUPO, LocalDateTime.of(2026, 2, 12, 12, 0), LocalDateTime.of(2026, 2, 12, 14, 0), "UTC_V2", null),
                procedencia(primera, GRUPO, LocalDateTime.of(2026, 2, 11, 12, 0), LocalDateTime.of(2026, 2, 11, 14, 0), "UTC_V2", null),
                procedencia(tercera, GRUPO, LocalDateTime.of(2026, 2, 10, 12, 0), LocalDateTime.of(2026, 2, 10, 14, 0), "UTC_V2", null)));

        final var resultado = consultar();

        assertEquals(List.of(tercera, primera, segunda), resultado.stream().map(SesionGrupoHorarioDomain::id).toList());
        assertEquals(List.of(LocalDateTime.of(2026, 2, 10, 12, 0), LocalDateTime.of(2026, 2, 11, 12, 0),
                        LocalDateTime.of(2026, 2, 12, 12, 0)),
                resultado.stream().map(SesionGrupoHorarioDomain::fechaHoraInicioUtc).toList());
        assertTrue(resultado.stream().allMatch(s -> s.estadoTemporal() == EstadoHorarioSesion.CONFIRMADA));
        verify(procedenciaPort, times(1)).consultarSesionesPorIds(any());
    }

    @Test
    @DisplayName("el total de la consulta academica se conserva (no se recalcula con el tamano de la pagina ni de la procedencia)")
    void total_se_conserva() {
        final UUID a = UUID.randomUUID();
        final UUID b = UUID.randomUUID();
        paginaDe(List.of(sesion(a), sesion(b)), 250);
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(a, GRUPO, LocalDateTime.of(2026, 2, 3, 12, 0), LocalDateTime.of(2026, 2, 3, 14, 0), "UTC_V2", null),
                procedencia(b, OTRO_GRUPO, LocalDateTime.of(2026, 2, 4, 12, 0), LocalDateTime.of(2026, 2, 4, 14, 0), "UTC_V2", null),
                procedencia(UUID.randomUUID(), GRUPO, LocalDateTime.of(2026, 2, 5, 12, 0), LocalDateTime.of(2026, 2, 5, 14, 0), "UTC_V2", null)));

        final var resultado = useCase.consultarSesionesGrupo(COORDINADOR, GRUPO, null, null, null, null);

        assertEquals(250, resultado.total());
        assertEquals(2, resultado.elementos().size());
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    @Test
    @DisplayName("ambito: un usuario que no es coordinador recibe 403 y no se consulta ni la pagina ni la procedencia")
    void usuario_sin_programa_es_403_sin_consultas() {
        final UUID otro = UUID.randomUUID();
        when(scopePort.findProgramaIdByCoordinadorUsuario(otro)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class,
                () -> useCase.consultarSesionesGrupo(otro, GRUPO, null, null, 1, 10));
        assertThrows(ForbiddenException.class,
                () -> useCase.consultarSesionesGrupo(null, GRUPO, null, null, 1, 10));

        verifyNoInteractions(queryPort, procedenciaPort);
    }

    @Test
    @DisplayName("ambito: grupo nulo es ValidationException y no se consulta ni la pagina ni la procedencia")
    void grupo_nulo_es_validacion_sin_consultas() {
        assertThrows(ValidationException.class,
                () -> useCase.consultarSesionesGrupo(COORDINADOR, null, null, null, 1, 10));

        verify(queryPort, never()).consultarSesionesGrupo(any(), any(), any(), any(), anyInt(), anyInt());
        verifyNoInteractions(procedenciaPort);
    }

    private static SesionGrupoProjection sesion(final UUID id) {
        return new SesionGrupoProjection(id, GRUPO, 1, "Grupo 1", "Sesion 1", 1, "S-01", 2, INICIO_V1, FIN_V1);
    }

    private static SesionProcedenciaRepositoryProjection procedencia(final UUID sesion, final UUID grupo,
                                                                      final LocalDateTime inicio,
                                                                      final LocalDateTime fin,
                                                                      final String procedenciaTemporal,
                                                                      final CancelacionSesionProjection cancelacion) {
        return new SesionProcedenciaRepositoryProjection(sesion, grupo, "Sesion 1", 1, "S-01", 2, "1", "Grupo 1",
                inicio, fin, procedenciaTemporal, cancelacion);
    }
}
