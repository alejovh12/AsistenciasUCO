package co.edu.uco.asistenciasuco.application.features.admin;

import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.dto.CierrePeriodoReporteDTO;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.primaryports.interactor.ConsultarHistorialCierresMasivosInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.ConsultarHistorialCierresMasivosUseCase;
import co.edu.uco.asistenciasuco.application.features.admin.consultarhistorialcierres.usecase.domain.CierrePeriodoReporteDomain;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.dto.PaginaPeriodosAdminDTO;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.primaryports.interactor.GestionarPeriodoAcademicoInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.gestionarperiodoacademico.usecase.GestionarPeriodoAcademicoUseCase;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.dto.GrupoFacultadDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.primaryports.interactor.AsignaturaPlanInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.asignaturaplan.usecase.AsignaturaPlanUseCase;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoFacultadProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PaginaPeriodosAdminProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.PeriodoAdminProjection;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/** Interactores de historial de cierres (F-ADM-006), periodos academicos y detalle de grupo de la facultad (D.9-a). */
class InteractoresAdminCulm001Test {

    @Test
    void el_historial_de_cierres_mapea_cada_reporte_al_dto() {
        final ConsultarHistorialCierresMasivosUseCase useCase = mock(ConsultarHistorialCierresMasivosUseCase.class);
        final UUID id = UUID.randomUUID();
        when(useCase.execute()).thenReturn(List.of(new CierrePeriodoReporteDomain(id, "2026-2", OffsetDateTime.parse("2026-11-08T10:00:00Z"), 40, 7, 35, 5,
                "COMPLETADO", "admin-1")));

        final List<CierrePeriodoReporteDTO> historial = new ConsultarHistorialCierresMasivosInteractor(useCase).execute();

        assertEquals(1, historial.size());
        assertEquals(id, historial.getFirst().id());
        assertEquals(35, historial.getFirst().totalAprobadosAsistencia());
        assertEquals("admin-1", historial.getFirst().ejecutadoPor());
        when(useCase.execute()).thenReturn(List.of());
        assertTrue(new ConsultarHistorialCierresMasivosInteractor(useCase).execute().isEmpty());
        assertThrows(NullPointerException.class, () -> new ConsultarHistorialCierresMasivosInteractor(null));
    }

    @Test
    void los_periodos_se_consultan_paginados_y_se_mapean_al_dto() {
        final GestionarPeriodoAcademicoUseCase useCase = mock(GestionarPeriodoAcademicoUseCase.class);
        final UUID usuario = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();
        final UUID institucion = UUID.randomUUID();
        when(useCase.consultar(usuario, 2, 5)).thenReturn(new PaginaPeriodosAdminProjection(List.of(new PeriodoAdminProjection(
                periodo, institucion, "UCO", "2026-2", "2026-2", LocalDate.of(2026, 8, 1), LocalDate.of(2026, 12, 15), 2026, true)), 11));

        final PaginaPeriodosAdminDTO pagina = new GestionarPeriodoAcademicoInteractor(useCase).consultar(usuario, 2, 5);

        assertEquals(11, pagina.total());
        assertEquals(1, pagina.elementos().size());
        assertEquals(periodo, pagina.elementos().getFirst().id());
        assertEquals("UCO", pagina.elementos().getFirst().nombreInstitucion());
        assertEquals(LocalDate.of(2026, 12, 15), pagina.elementos().getFirst().fechaFin());
        assertTrue(pagina.elementos().getFirst().esVigente());
    }

    @Test
    void el_detalle_de_grupo_de_la_facultad_copia_todos_los_campos_de_la_proyeccion() {
        final AsignaturaPlanUseCase useCase = mock(AsignaturaPlanUseCase.class);
        final UUID usuario = UUID.randomUUID();
        final UUID grupo = UUID.randomUUID();
        final UUID periodo = UUID.randomUUID();
        final UUID asignatura = UUID.randomUUID();
        final UUID docente = UUID.randomUUID();
        when(useCase.consultarGrupoFacultad(usuario, grupo)).thenReturn(new GrupoFacultadProjection(
                grupo, 3, "G03", periodo, asignatura, "Calculo", docente, 30, 12, 18, true, List.of("Sistemas", "Derecho")));

        final GrupoFacultadDTO dto = new AsignaturaPlanInteractor(useCase).consultarGrupoFacultad(usuario, grupo);

        assertEquals(grupo, dto.idGrupo());
        assertEquals(3, dto.codigo());
        assertEquals("G03", dto.nombre());
        assertEquals(periodo, dto.idPeriodoAcademico());
        assertEquals(asignatura, dto.idAsignatura());
        assertEquals("Calculo", dto.nombreAsignatura());
        assertEquals(docente, dto.idDocente());
        assertEquals(30, dto.capacidadMaximaPermitida());
        assertEquals(12, dto.estudiantesActivos());
        assertEquals(18, dto.cuposDisponibles());
        assertEquals(List.of("Sistemas", "Derecho"), dto.programas());
    }
}
