package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.ConsultarPadronGrupoDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.dto.PadronGrupoPaginaDTO;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.primaryports.interactor.ConsultarPadronGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain.ConsultarPadronGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.impl.ConsultarPadronGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.PadronGrupoQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.EstudianteGrupoRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.PadronGrupoPaginaProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** CIERRE-W1A: padron del grupo del docente titular (normalizacion, titularidad, mapeo, paginacion y busqueda en el servidor). */
class ConsultarPadronGrupoFeatureTest {

    private static final UUID ACTOR = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();

    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final PadronGrupoQueryPort query = mock(PadronGrupoQueryPort.class);
    private final ConsultarPadronGrupoInteractor interactor =
            new ConsultarPadronGrupoInteractor(new ConsultarPadronGrupoUseCaseImpl(scope, query));

    @Test
    void la_normalizacion_toma_valores_por_defecto_y_acota_la_paginacion() {
        final ConsultarPadronGrupoDomain porDefecto = ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, null, null, null);
        assertEquals(1, porDefecto.pagina());
        assertEquals(10, porDefecto.tamano());
        assertNull(porDefecto.busqueda());
        assertEquals(1, ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, -3, 0, null).pagina());
        assertEquals(10, ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 1, 0, null).tamano());
        assertEquals(100, ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 1, 5000, null).tamano());
        assertEquals(3, ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 3, 12, null).pagina());
    }

    @Test
    void la_busqueda_se_recorta_y_si_queda_vacia_es_nula() {
        assertEquals("ana", ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 1, 10, "  ana  ").busqueda());
        assertNull(ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 1, 10, "   ").busqueda());
        assertNull(ConsultarPadronGrupoDomain.of(GRUPO, ACTOR, 1, 10, "").busqueda());
    }

    @Test
    void sin_grupo_o_sin_usuario_es_error_de_validacion() {
        assertThrows(ValidationException.class, () -> ConsultarPadronGrupoDomain.of(null, ACTOR, 1, 10, null));
        assertThrows(ValidationException.class, () -> ConsultarPadronGrupoDomain.of(GRUPO, null, 1, 10, null));
        assertThrows(ValidationException.class, () -> interactor.execute(null));
    }

    @Test
    void quien_no_es_docente_institucional_recibe_forbidden_y_sql_no_se_consulta() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.empty());

        final var consultarPadronGrupoDTO2 = new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 1, 10, null);
        assertThrows(ForbiddenException.class, () -> interactor.execute(consultarPadronGrupoDTO2));

        verifyNoInteractions(query);
    }

    @Test
    void un_docente_que_no_es_titular_del_grupo_recibe_forbidden_y_sql_no_se_consulta() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(scope.canDocenteAccessGrupo(ACTOR, GRUPO)).thenReturn(false);

        final var consultarPadronGrupoDTO = new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 1, 10, null);
        assertThrows(ForbiddenException.class, () -> interactor.execute(consultarPadronGrupoDTO));

        verifyNoInteractions(query);
    }

    @Test
    void delega_en_sql_con_los_criterios_normalizados_y_mapea_filas_y_total() {
        final UUID matricula = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(scope.canDocenteAccessGrupo(ACTOR, GRUPO)).thenReturn(true);
        when(query.consultarPadronPagina(GRUPO, ACTOR, 2, 12, "ana")).thenReturn(new PadronGrupoPaginaProjection(
                List.of(new EstudianteGrupoRepositoryProjection(matricula, estudiante, "100", "Ana Perez", "a@x.edu", "A", "Activo")),
                25L));

        final PadronGrupoPaginaDTO pagina = interactor.execute(new ConsultarPadronGrupoDTO(GRUPO, ACTOR, 2, 12, " ana "));

        assertEquals(25L, pagina.totalRegistros());
        assertEquals(2, pagina.numeroPagina());
        assertEquals(12, pagina.tamanoPagina());
        assertEquals(1, pagina.estudiantes().size());
        assertEquals(matricula, pagina.estudiantes().get(0).id());
        assertEquals(estudiante, pagina.estudiantes().get(0).idEstudiante());
        assertEquals("Ana Perez", pagina.estudiantes().get(0).nombreCompleto());
        assertEquals("Activo", pagina.estudiantes().get(0).nombreEstado());
        verify(query).consultarPadronPagina(GRUPO, ACTOR, 2, 12, "ana");
    }

    @Test
    void sin_filas_el_total_es_cero_y_se_usan_los_valores_por_defecto() {
        when(scope.findDocenteIdByUsuario(ACTOR)).thenReturn(Optional.of(UUID.randomUUID()));
        when(scope.canDocenteAccessGrupo(ACTOR, GRUPO)).thenReturn(true);
        when(query.consultarPadronPagina(GRUPO, ACTOR, 1, 10, null)).thenReturn(new PadronGrupoPaginaProjection(null, 0L));

        final PadronGrupoPaginaDTO pagina = interactor.execute(new ConsultarPadronGrupoDTO(GRUPO, ACTOR, null, null, null));

        assertEquals(0L, pagina.totalRegistros());
        assertEquals(0, pagina.estudiantes().size());
        assertEquals(1, pagina.numeroPagina());
        assertEquals(10, pagina.tamanoPagina());
    }
}
