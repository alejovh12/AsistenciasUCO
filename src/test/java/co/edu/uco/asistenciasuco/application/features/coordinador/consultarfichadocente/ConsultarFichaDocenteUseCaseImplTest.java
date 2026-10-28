package co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.ConsultarFichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.dto.FichaDocenteDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.primaryports.interactor.ConsultarFichaDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.ConsultarFichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.domain.FichaDocenteDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.consultarfichadocente.usecase.impl.ConsultarFichaDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.DocenteProgramaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.HistorialGruposDocenteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaFiltro;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocentesProgramaPagina;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoHistorialDocenteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.HistorialGruposDocentePagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** HU094: ficha del docente del programa del coordinador (FD-01..FD-08). */
class ConsultarFichaDocenteUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID PROGRAMA = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final UUID PERIODO = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final DocenteProgramaQueryPort directorio = mock(DocenteProgramaQueryPort.class);
    private final HistorialGruposDocenteQueryPort historial = mock(HistorialGruposDocenteQueryPort.class);
    private final ConsultarFichaDocenteUseCaseImpl useCase = new ConsultarFichaDocenteUseCaseImpl(scope, directorio, historial);

    private static DocenteProgramaProjection docente() {
        return new DocenteProgramaProjection(UUID.randomUUID(), DOCENTE, UUID.randomUUID(), "CC", 1234, "Ana", null, "Perez", null,
                "Ana Perez", "ana@uco.edu.co", UUID.randomUUID(), "CAT", "Catedra", Instant.parse("2026-01-10T00:00:00Z"), 3, false);
    }

    private static GrupoHistorialDocenteProjection grupo(final String estado) {
        return new GrupoHistorialDocenteProjection(UUID.randomUUID(), 7, "Grupo 7", UUID.randomUUID(), "Algebra", PERIODO, 20261,
                "2026-1", LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 10), estado, 30, 25, 0, 2, 1);
    }

    @Test
    void FD01_combina_cabecera_del_directorio_e_historial_con_resumen() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(directorio.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(docente()), 1));
        when(historial.consultarHistorial(ACTOR, DOCENTE, PERIODO, 2, 5))
                .thenReturn(new HistorialGruposDocentePagina(List.of(grupo("VIGENTE")), 6, 6, 3, 1, 25));

        final FichaDocenteDomain ficha = useCase.execute(new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, PERIODO, 2, 5));

        assertEquals(DOCENTE, ficha.docente().idDocente());
        assertEquals("Ana Perez", ficha.docente().nombreCompleto());
        assertEquals(6, ficha.totalHistorial());
        assertEquals(1, ficha.historial().size());
        assertEquals("VIGENTE", ficha.historial().getFirst().estadoPeriodo());
        assertEquals(LocalDate.of(2026, 1, 20), ficha.historial().getFirst().fechaInicioPeriodo());
        assertEquals(6, ficha.resumen().totalGrupos());
        assertEquals(3, ficha.resumen().totalPeriodos());
        assertEquals(1, ficha.resumen().gruposVigentes());
        assertEquals(25, ficha.resumen().estudiantesActivosVigentes());

        final ArgumentCaptor<DocentesProgramaFiltro> filtro = ArgumentCaptor.forClass(DocentesProgramaFiltro.class);
        verify(directorio).consultar(eq(ACTOR), filtro.capture());
        assertEquals(PROGRAMA, filtro.getValue().idPrograma());
        assertEquals(DOCENTE, filtro.getValue().idDocente());
        assertEquals(1, filtro.getValue().tamano());
        assertNull(filtro.getValue().estaActivo());
    }

    @Test
    void FD02_paginacion_por_defecto_y_periodo_opcional() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(directorio.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(docente()), 1));
        when(historial.consultarHistorial(ACTOR, DOCENTE, null, 1, 10))
                .thenReturn(new HistorialGruposDocentePagina(List.of(), 0, 0, 0, 0, 0));

        final FichaDocenteDomain ficha = useCase.execute(new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, null, null, null));

        assertTrue(ficha.historial().isEmpty());
        assertEquals(0, ficha.resumen().totalGrupos());
        verify(historial).consultarHistorial(ACTOR, DOCENTE, null, 1, 10);
    }

    @Test
    void FD03_docente_fuera_del_programa_responde_404_sin_consultar_historial() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(directorio.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(), 0));

        final ConsultarFichaDocenteDomain consulta = new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, null, 1, 10);
        assertThrows(ResourceNotFoundException.class, () -> useCase.execute(consulta));
        verifyNoInteractions(historial);
    }

    @Test
    void FD04_sin_programa_a_cargo_responde_403() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.empty());

        final ConsultarFichaDocenteDomain consulta = new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, null, 1, 10);
        assertThrows(ForbiddenException.class, () -> useCase.execute(consulta));
        verifyNoInteractions(directorio, historial);
    }

    @Test
    void FD05_validaciones_de_entrada() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarFichaDocenteDomain(ACTOR, null, null, 1, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, null, 0, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarFichaDocenteDomain(ACTOR, DOCENTE, null, 1, 0)));
        verifyNoInteractions(scope, directorio, historial);
    }

    @Test
    void FD06_constructor_rechaza_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarFichaDocenteUseCaseImpl(null, directorio, historial));
        assertThrows(NullPointerException.class, () -> new ConsultarFichaDocenteUseCaseImpl(scope, null, historial));
        assertThrows(NullPointerException.class, () -> new ConsultarFichaDocenteUseCaseImpl(scope, directorio, null));
    }

    @Test
    void FD07_interactor_mapea_dto_dominio_dto() {
        when(scope.findProgramaIdByCoordinadorUsuario(ACTOR)).thenReturn(Optional.of(PROGRAMA));
        when(directorio.consultar(eq(ACTOR), any())).thenReturn(new DocentesProgramaPagina(List.of(docente()), 1));
        when(historial.consultarHistorial(eq(ACTOR), eq(DOCENTE), eq(PERIODO), anyInt(), anyInt()))
                .thenReturn(new HistorialGruposDocentePagina(List.of(grupo("FINALIZADO")), 1, 1, 1, 0, 0));

        final FichaDocenteDTO dto = new ConsultarFichaDocenteInteractor(useCase)
                .execute(new ConsultarFichaDocenteDTO(ACTOR, DOCENTE, PERIODO, 1, 10));

        assertEquals(DOCENTE, dto.docente().idDocente());
        assertEquals("CAT", dto.docente().codigoTipoVinculacion());
        assertEquals(1, dto.totalHistorial());
        assertEquals("FINALIZADO", dto.historial().getFirst().estadoPeriodo());
        assertEquals("Algebra", dto.historial().getFirst().nombreAsignatura());
        assertEquals(25, dto.historial().getFirst().estudiantesActivos());
        assertEquals(1, dto.resumen().totalGrupos());
    }

    @Test
    void FD08_interactor_propaga_dto_nulo_como_validacion() {
        final ConsultarFichaDocenteInteractor interactor = new ConsultarFichaDocenteInteractor(useCase);
        assertThrows(ValidationException.class, () -> interactor.execute(null));
        assertThrows(NullPointerException.class, () -> new ConsultarFichaDocenteInteractor(null));
    }
}
