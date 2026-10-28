package co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.ConsultarOfertaGruposDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.dto.OfertaGruposPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.primaryports.interactor.ConsultarOfertaGruposInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.ConsultarOfertaGruposDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.domain.OfertaGruposPaginaDomain;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarofertagrupos.usecase.impl.ConsultarOfertaGruposUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.OfertaGruposEstudianteQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.GrupoOfertaEstudianteProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.OfertaGruposEstudiantePagina;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
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

/** HU008: oferta de grupos del estudiante (OG-01..OG-07). */
class ConsultarOfertaGruposUseCaseImplTest {

    private static final UUID ACTOR = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final OfertaGruposEstudianteQueryPort port = mock(OfertaGruposEstudianteQueryPort.class);
    private final ConsultarOfertaGruposUseCaseImpl useCase = new ConsultarOfertaGruposUseCaseImpl(scope, port);

    private static GrupoOfertaEstudianteProjection grupo(final String codigoSolicitud, final boolean matriculado) {
        return new GrupoOfertaEstudianteProjection(UUID.randomUUID(), 7, "Grupo 7", UUID.randomUUID(), "MAT1", "Algebra", "Ana Perez",
                UUID.randomUUID(), "2026-2", LocalDate.of(2026, 7, 20), LocalDate.of(2026, 12, 10), "VIGENTE",
                "Lunes 08:00-10:00", 30, 25, 5, matriculado, codigoSolicitud == null ? null : UUID.randomUUID(), codigoSolicitud, "ESP");
    }

    @Test
    void OG01_consulta_la_oferta_con_busqueda_recortada_y_paginacion() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarOferta(ACTOR, "algebra", 2, 5))
                .thenReturn(new OfertaGruposEstudiantePagina(List.of(grupo(null, false)), 6));

        final OfertaGruposPaginaDomain pagina = useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, "  algebra ", 2, 5));

        assertEquals(6, pagina.total());
        assertEquals("Algebra", pagina.grupos().getFirst().nombreAsignatura());
        assertEquals(5, pagina.grupos().getFirst().cuposDisponibles());
        assertEquals("Lunes 08:00-10:00", pagina.grupos().getFirst().horario());
        assertNull(pagina.grupos().getFirst().estadoSolicitud());
    }

    @Test
    void OG02_valores_por_defecto_y_busqueda_vacia_como_nula() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarOferta(eq(ACTOR), any(), anyInt(), anyInt())).thenReturn(new OfertaGruposEstudiantePagina(null, 0));

        final OfertaGruposPaginaDomain pagina = useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, "   ", null, null));

        assertTrue(pagina.grupos().isEmpty());
        verify(port).consultarOferta(ACTOR, null, 1, 10);
    }

    @Test
    void OG03_traduce_el_estado_de_la_solicitud_abierta() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarOferta(eq(ACTOR), any(), anyInt(), anyInt())).thenReturn(new OfertaGruposEstudiantePagina(
                List.of(grupo("P", false), grupo("E", false), grupo("Z", false), grupo(null, true)), 4));

        final var grupos = useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, null, 1, 10)).grupos();

        assertEquals("PENDIENTE", grupos.get(0).estadoSolicitud());
        assertEquals("EN_ESPERA", grupos.get(1).estadoSolicitud());
        assertEquals("Z", grupos.get(2).estadoSolicitud());
        assertTrue(grupos.get(3).matriculado());
    }

    @Test
    void OG08_clasifica_el_tipo_de_asignatura_y_marca_las_transversales() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarOferta(eq(ACTOR), any(), anyInt(), anyInt())).thenReturn(new OfertaGruposEstudiantePagina(List.of(
                conComponente("ESP"), conComponente("BAS"), conComponente("INS"), conComponente("XYZ"), conComponente(null)), 5));

        final var grupos = useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, null, 1, 10)).grupos();

        assertEquals("ESPECIFICA", grupos.get(0).tipoAsignatura());
        assertFalse(grupos.get(0).transversal());
        assertEquals("BASICA", grupos.get(1).tipoAsignatura());
        assertTrue(grupos.get(1).transversal());
        assertEquals("INSTITUCIONAL", grupos.get(2).tipoAsignatura());
        assertTrue(grupos.get(2).transversal());
        assertEquals("XYZ", grupos.get(3).tipoAsignatura());
        assertFalse(grupos.get(3).transversal());
        assertNull(grupos.get(4).tipoAsignatura());
    }

    private static GrupoOfertaEstudianteProjection conComponente(final String componente) {
        return new GrupoOfertaEstudianteProjection(UUID.randomUUID(), 1, "Grupo 1", UUID.randomUUID(), "C1", "Asignatura", "Doc",
                UUID.randomUUID(), "2026-2", null, null, "VIGENTE", null, 10, 0, 10, false, null, null, componente);
    }

    @Test
    void OG04_usuario_sin_estudiante_responde_403_sin_consultar() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        final ConsultarOfertaGruposDomain consulta = new ConsultarOfertaGruposDomain(ACTOR, null, 1, 10);
        assertThrows(ForbiddenException.class, () -> useCase.execute(consulta));
        verifyNoInteractions(port);
    }

    @Test
    void OG05_validaciones_de_entrada() {
        assertThrows(ValidationException.class, () -> useCase.execute(null));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, null, 0, 10)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, null, 1, 0)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, null, 1, 101)));
        assertThrows(ValidationException.class, () -> useCase.execute(new ConsultarOfertaGruposDomain(ACTOR, "x".repeat(51), 1, 10)));
        verifyNoInteractions(scope, port);
    }

    @Test
    void OG06_interactor_mapea_a_dto() {
        when(scope.findEstudianteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarOferta(eq(ACTOR), any(), anyInt(), anyInt()))
                .thenReturn(new OfertaGruposEstudiantePagina(List.of(grupo("E", false)), 1));

        final OfertaGruposPaginaDTO dto = new ConsultarOfertaGruposInteractor(useCase).execute(new ConsultarOfertaGruposDTO(ACTOR, null, 1, 10));

        assertEquals(1, dto.total());
        assertEquals("EN_ESPERA", dto.grupos().getFirst().estadoSolicitud());
        assertEquals("Ana Perez", dto.grupos().getFirst().nombreDocente());
        assertEquals(LocalDate.of(2026, 7, 20), dto.grupos().getFirst().fechaInicioPeriodo());
        assertEquals("ESPECIFICA", dto.grupos().getFirst().tipoAsignatura());
        assertFalse(dto.grupos().getFirst().transversal());
        assertThrows(ValidationException.class, () -> new ConsultarOfertaGruposInteractor(useCase).execute(null));
    }

    @Test
    void OG07_constructores_rechazan_dependencias_nulas() {
        assertThrows(NullPointerException.class, () -> new ConsultarOfertaGruposUseCaseImpl(null, port));
        assertThrows(NullPointerException.class, () -> new ConsultarOfertaGruposUseCaseImpl(scope, null));
        assertThrows(NullPointerException.class, () -> new ConsultarOfertaGruposInteractor(null));
    }
}
