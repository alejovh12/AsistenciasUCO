package co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.domain.GenerarReporteAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.entity.ReporteAsistenciaEntity;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaRow;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GenerarReporteAsistenciaUseCaseImplTest {

    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID DOCENTE_TITULAR = UUID.randomUUID();
    private static final UUID DOCENTE_AJENO = UUID.randomUUID();
    private static final UUID DOCENTE_SIN_IDENTIDAD = UUID.randomUUID();
    private static final UUID COORDINADOR = UUID.randomUUID();
    private static final UUID COORDINADOR_AJENO = UUID.randomUUID();
    private static final UUID ADMINISTRADOR = UUID.randomUUID();
    private static final UUID ADMINISTRADOR_INACTIVO = UUID.randomUUID();
    private static final UUID DOCENTE_COORDINADOR = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();

    private final ReporteAsistenciaQueryPort queryPort = mock(ReporteAsistenciaQueryPort.class);
    private final InstitutionalScopePort institutionalScopePort = mock(InstitutionalScopePort.class);
    private final GenerarReporteAsistenciaUseCaseImpl useCase =
            new GenerarReporteAsistenciaUseCaseImpl(queryPort, institutionalScopePort);

    @Test
    void docente_titular_activo_obtiene_el_reporte() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_TITULAR)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_TITULAR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_TITULAR, GRUPO)).thenReturn(true);
        when(queryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(row("Ana Perez")));

        final List<ReporteAsistenciaEntity> result = useCase.execute(domain(DOCENTE_TITULAR));

        assertEquals("Ana Perez", result.getFirst().nombreEstudiante());
        verify(queryPort).consultarReporteAsistenciaGrupo(GRUPO);
    }

    @Test
    void docente_ajeno_es_rechazado_antes_de_consultar_el_reporte() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_AJENO)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_AJENO)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(DOCENTE_AJENO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(DOCENTE_AJENO)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void docente_sin_identidad_institucional_falla_cerrado() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_SIN_IDENTIDAD)).thenReturn(Set.of(InstitutionalRole.DOCENTE));
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_SIN_IDENTIDAD)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(DOCENTE_SIN_IDENTIDAD)));

        verify(institutionalScopePort, never()).canDocenteAccessGrupo(DOCENTE_SIN_IDENTIDAD, GRUPO);
        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void docente_con_roles_activos_vacios_es_rechazado_aunque_tenga_identidad_y_titularidad_residuales() {
        final UUID actor = UUID.randomUUID();
        when(institutionalScopePort.findRolesActivosByUsuario(actor)).thenReturn(Set.of());
        when(institutionalScopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(institutionalScopePort.canDocenteAccessGrupo(actor, GRUPO)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(actor)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void coordinador_activo_con_scope_del_grupo_obtiene_el_reporte() {
        when(institutionalScopePort.findRolesActivosByUsuario(COORDINADOR)).thenReturn(Set.of(InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.canCoordinadorAccessGrupo(COORDINADOR, GRUPO)).thenReturn(true);
        when(queryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(row("Ana Perez")));

        assertEquals(1, useCase.execute(domain(COORDINADOR)).size());

        verify(institutionalScopePort, never()).findDocenteIdByUsuario(COORDINADOR);
        verify(queryPort).consultarReporteAsistenciaGrupo(GRUPO);
    }

    @Test
    void coordinador_fuera_de_scope_es_rechazado_sin_consultar() {
        when(institutionalScopePort.findRolesActivosByUsuario(COORDINADOR_AJENO)).thenReturn(Set.of(InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.canCoordinadorAccessGrupo(COORDINADOR_AJENO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(COORDINADOR_AJENO)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void coordinador_con_roles_activos_vacios_es_rechazado_aunque_el_scope_residual_sea_valido() {
        final UUID actor = UUID.randomUUID();
        when(institutionalScopePort.findRolesActivosByUsuario(actor)).thenReturn(Set.of());
        when(institutionalScopePort.canCoordinadorAccessGrupo(actor, GRUPO)).thenReturn(true);

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(actor)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void administrador_activo_conserva_acceso_institucional() {
        when(institutionalScopePort.findRolesActivosByUsuario(ADMINISTRADOR)).thenReturn(Set.of(InstitutionalRole.ADMINISTRADOR));
        when(institutionalScopePort.isAdministradorActivo(ADMINISTRADOR)).thenReturn(true);
        when(queryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(row("Ana Perez")));

        assertEquals(1, useCase.execute(domain(ADMINISTRADOR)).size());

        verify(queryPort).consultarReporteAsistenciaGrupo(GRUPO);
    }

    @Test
    void administrador_no_activo_no_accede_aun_si_el_jwt_presenta_el_rol() {
        when(institutionalScopePort.findRolesActivosByUsuario(ADMINISTRADOR_INACTIVO))
                .thenReturn(Set.of(InstitutionalRole.ADMINISTRADOR));
        when(institutionalScopePort.isAdministradorActivo(ADMINISTRADOR_INACTIVO)).thenReturn(false);

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(ADMINISTRADOR_INACTIVO)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void multirrol_docente_coordinador_solo_necesita_una_via_con_scope_valido() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_COORDINADOR))
                .thenReturn(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.canCoordinadorAccessGrupo(DOCENTE_COORDINADOR, GRUPO)).thenReturn(true);
        when(queryPort.consultarReporteAsistenciaGrupo(GRUPO)).thenReturn(List.of(row("Ana Perez")));

        assertEquals(1, useCase.execute(domain(DOCENTE_COORDINADOR)).size());

        verify(institutionalScopePort, never()).findDocenteIdByUsuario(DOCENTE_COORDINADOR);
        verify(queryPort).consultarReporteAsistenciaGrupo(GRUPO);
    }

    @Test
    void multirrol_sin_identidad_docente_ni_scope_coordinador_no_bypassea_la_validacion() {
        when(institutionalScopePort.findRolesActivosByUsuario(DOCENTE_COORDINADOR))
                .thenReturn(Set.of(InstitutionalRole.DOCENTE, InstitutionalRole.COORDINADOR));
        when(institutionalScopePort.canCoordinadorAccessGrupo(DOCENTE_COORDINADOR, GRUPO)).thenReturn(false);
        when(institutionalScopePort.findDocenteIdByUsuario(DOCENTE_COORDINADOR)).thenReturn(Optional.empty());

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(DOCENTE_COORDINADOR)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void estudiante_sin_otra_via_permitida_es_rechazado() {
        when(institutionalScopePort.findRolesActivosByUsuario(ESTUDIANTE)).thenReturn(Set.of(InstitutionalRole.ESTUDIANTE));

        assertThrows(ForbiddenException.class, () -> useCase.execute(domain(ESTUDIANTE)));

        verify(queryPort, never()).consultarReporteAsistenciaGrupo(any());
    }

    @Test
    void exige_puerto_de_scope_institucional() {
        assertThrows(NullPointerException.class, () -> new GenerarReporteAsistenciaUseCaseImpl(queryPort, null));
    }

    private static GenerarReporteAsistenciaDomain domain(final UUID actor) {
        return new GenerarReporteAsistenciaDomain(GRUPO, actor);
    }

    private static ReporteAsistenciaRow row(final String student) {
        return new ReporteAsistenciaRow("G01", "Grupo 01", 1, "Sesion 1",
                LocalDateTime.of(2026, 1, 1, 8, 0), LocalDateTime.of(2026, 1, 1, 9, 0),
                "12345678", student, "estudiante@uco.edu.co", Boolean.TRUE, null);
    }
}
