package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.common.PaginaResultado;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.EstadoAsistenciaSesion;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarsesionesmateria.usecase.domain.SesionMateriaEstudianteDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.common.EstadoHorarioSesion;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.AsistenciaSesionEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SesionMateriaEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.AsistenciaSesionEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.SesionMateriaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionProcedenciaRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * PAG-EST-SESIONES (RED): las lecturas complementarias (asistencia y procedencia) se acotan a los ids de las sesiones de
 * la pagina devuelta; no se lee toda la asistencia de la materia ni todas las sesiones de cada grupo. El contrato
 * publico (estados, reclamable, estadoTemporal) no cambia.
 */
class ConsultarSesionesMateriaEstudianteAcotadoTest {

    private final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
    private final SesionMateriaEstudianteQueryPort queryPort = mock(SesionMateriaEstudianteQueryPort.class);
    private final AsistenciaSesionEstudianteQueryPort asistenciaPort = mock(AsistenciaSesionEstudianteQueryPort.class);
    private final SesionProcedenciaQueryPort procedenciaPort = mock(SesionProcedenciaQueryPort.class);
    private final ParameterCatalogPort parametros = mock(ParameterCatalogPort.class);
    private final ConsultarSesionesMateriaEstudianteUseCaseImpl useCase =
            new ConsultarSesionesMateriaEstudianteUseCaseImpl(scopePort, queryPort, asistenciaPort, procedenciaPort, parametros);

    private final UUID actor = UUID.randomUUID();
    private final UUID estudiante = UUID.randomUUID();
    private final UUID asignatura = UUID.randomUUID();
    private final UUID grupoA = UUID.randomUUID();
    private final UUID grupoB = UUID.randomUUID();
    private final UUID sesion1 = UUID.randomUUID();
    private final UUID sesion2 = UUID.randomUUID();
    private final UUID sesionFueraDePagina = UUID.randomUUID();

    private SesionMateriaEstudianteProjection sesion(final UUID id, final int numero, final UUID grupo) {
        return new SesionMateriaEstudianteProjection(id, "Sesion " + numero, numero, "SES-0" + numero, numero, grupo,
                "1", "Grupo 001", LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 15, 0));
    }

    private static AsistenciaSesionEstudianteProjection hecho(final UUID sesion, final boolean asistio, final String razon) {
        return new AsistenciaSesionEstudianteProjection(sesion, 1, asistio, razon, null, null, null);
    }

    private static SesionProcedenciaRepositoryProjection procedencia(final UUID sesion, final UUID grupo) {
        return new SesionProcedenciaRepositoryProjection(sesion, grupo, "Sesion", 1, "SES", 1, "1", "Grupo 001",
                LocalDateTime.of(2026, 8, 17, 13, 0), LocalDateTime.of(2026, 8, 17, 15, 0), "UTC_V2");
    }

    private void paginaConSesiones(final List<SesionMateriaEstudianteProjection> sesiones) {
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(estudiante));
        when(queryPort.consultarSesionesMateria(actor, asignatura, 1, 100))
                .thenReturn(new PaginaResultado<>(sesiones, 1, 100, sesiones.size()));
    }

    @Test
    @DisplayName("la asistencia y la procedencia se piden solo para los ids de la pagina, sin leer grupos ni materia completos")
    void las_lecturas_complementarias_se_acotan_a_los_ids_de_la_pagina() {
        paginaConSesiones(List.of(sesion(sesion1, 1, grupoA), sesion(sesion2, 2, grupoB)));
        when(asistenciaPort.consultarAsistenciaSesiones(eq(estudiante), eq(asignatura), any())).thenReturn(List.of());
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of());

        useCase.execute(actor, asignatura);

        verify(asistenciaPort).consultarAsistenciaSesiones(eq(estudiante), eq(asignatura),
                argThat(ids -> ids.size() == 2 && Set.copyOf(ids).equals(Set.of(sesion1, sesion2))));
        verify(procedenciaPort).consultarSesionesPorIds(
                argThat(ids -> ids.size() == 2 && Set.copyOf(ids).equals(Set.of(sesion1, sesion2))));
        verify(asistenciaPort, never()).consultarAsistenciaSesiones(any(), any());
        verify(procedenciaPort, never()).consultarSesionesPorGrupo(any());
    }

    @Test
    @DisplayName("una pagina vacia no ejecuta ninguna consulta complementaria")
    void pagina_vacia_no_consulta_asistencia_ni_procedencia() {
        paginaConSesiones(List.of());

        final List<SesionMateriaEstudianteDomain> resultado = useCase.execute(actor, asignatura);

        assertEquals(List.of(), resultado);
        verifyNoInteractions(asistenciaPort, procedenciaPort);
    }

    @Test
    @DisplayName("ids de sesion repetidos en la pagina se piden una sola vez")
    void ids_repetidos_se_deduplican_antes_de_consultar() {
        paginaConSesiones(List.of(sesion(sesion1, 1, grupoA), sesion(sesion1, 1, grupoA)));
        when(asistenciaPort.consultarAsistenciaSesiones(eq(estudiante), eq(asignatura), any())).thenReturn(List.of());
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of());

        useCase.execute(actor, asignatura);

        verify(asistenciaPort, times(1)).consultarAsistenciaSesiones(eq(estudiante), eq(asignatura),
                argThat(ids -> ids.size() == 1 && ids.contains(sesion1)));
    }

    @Test
    @DisplayName("el estado de cada sesion sale de sus propios hechos; filas de otra sesion o de otro grupo no contaminan")
    void hechos_y_procedencias_se_aplican_por_sesion_sin_mezclar_grupos() {
        paginaConSesiones(List.of(sesion(sesion1, 1, grupoA), sesion(sesion2, 2, grupoB)));
        when(asistenciaPort.consultarAsistenciaSesiones(eq(estudiante), eq(asignatura), any())).thenReturn(List.of(
                hecho(sesion1, false, "SJC"), hecho(sesion2, true, "AN"), hecho(sesionFueraDePagina, false, "SJC")));
        // sesion1 trae procedencia de grupoB (no coincide con su grupo) y debe ignorarse; sesion2 si coincide.
        when(procedenciaPort.consultarSesionesPorIds(any())).thenReturn(List.of(
                procedencia(sesion1, grupoB), procedencia(sesion2, grupoB), procedencia(sesionFueraDePagina, grupoA)));

        final List<SesionMateriaEstudianteDomain> resultado = useCase.execute(actor, asignatura);

        assertEquals(List.of(sesion1, sesion2), resultado.stream().map(SesionMateriaEstudianteDomain::id).toList());
        assertEquals(EstadoAsistenciaSesion.AUSENTE, resultado.get(0).estadoAsistencia());
        assertEquals(EstadoHorarioSesion.INDETERMINADA, resultado.get(0).estadoTemporal());
        assertEquals(EstadoAsistenciaSesion.PRESENTE, resultado.get(1).estadoAsistencia());
        assertEquals(EstadoHorarioSesion.CONFIRMADA, resultado.get(1).estadoTemporal());
    }
}
