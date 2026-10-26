package co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.domain.ConsultarSesionDomain;
import co.edu.uco.asistenciasuco.application.features.sesion.consultarsesion.usecase.entity.SesionConsultadaEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.projection.SesionRepositoryProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** Ownership de {@code GET /api/v1/sesiones/{sesionId}} y su equivalente legacy {@code POST /consultas}. */
class ConsultarSesionUseCaseImplTest {

    private static final UUID SESION = UUID.randomUUID();
    private static final UUID GRUPO = UUID.randomUUID();
    private static final UUID USUARIO = UUID.randomUUID();

    private final SesionRepositoryPort sesionRepositoryPort = mock(SesionRepositoryPort.class);
    private final InstitutionalScopePort scope = mock(InstitutionalScopePort.class);
    private final ConsultarSesionUseCaseImpl useCase = new ConsultarSesionUseCaseImpl(sesionRepositoryPort, scope);

    private void sesionExistente() {
        when(sesionRepositoryPort.consultarSesion(any())).thenReturn(new SesionRepositoryProjection(
                SESION, GRUPO, "Tema", 1, "S1", 1, "G1", "Grupo 1", null, null));
    }

    private SesionConsultadaEntity consultar() {
        return useCase.execute(new ConsultarSesionDomain(SESION, USUARIO));
    }

    @Test
    void docente_titular_consulta_la_sesion() {
        sesionExistente();
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);

        assertEquals(SESION, consultar().getSesion());
    }

    @Test
    void docente_ajeno_es_rechazado() {
        sesionExistente();

        assertThrows(ForbiddenException.class, this::consultar);
    }

    @Test
    void estudiante_matriculado_consulta_la_sesion() {
        sesionExistente();
        when(scope.canEstudianteAccessGrupo(USUARIO, GRUPO)).thenReturn(true);

        assertEquals(GRUPO, consultar().getGrupo());
    }

    @Test
    void estudiante_ajeno_al_grupo_es_rechazado() {
        sesionExistente();
        when(scope.canEstudianteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, this::consultar);
    }

    @Test
    void coordinador_del_programa_del_grupo_consulta_la_sesion() {
        sesionExistente();
        when(scope.canCoordinadorAccessGrupo(USUARIO, GRUPO)).thenReturn(true);

        assertEquals(SESION, consultar().getSesion());
    }

    @Test
    void coordinador_de_otro_programa_es_rechazado() {
        sesionExistente();
        when(scope.canCoordinadorAccessGrupo(USUARIO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, this::consultar);
    }

    @Test
    void decano_de_la_facultad_del_grupo_consulta_la_sesion() {
        sesionExistente();
        when(scope.canDecanoAccessGrupo(USUARIO, GRUPO)).thenReturn(true);

        assertEquals(SESION, consultar().getSesion());
    }

    @Test
    void decano_de_otra_facultad_es_rechazado() {
        sesionExistente();
        when(scope.canDecanoAccessGrupo(USUARIO, GRUPO)).thenReturn(false);

        assertThrows(ForbiddenException.class, this::consultar);
    }

    @Test
    void usuario_con_varios_roles_accede_si_alguna_identidad_cubre_el_grupo() {
        sesionExistente();
        when(scope.canDocenteAccessGrupo(USUARIO, GRUPO)).thenReturn(false);
        when(scope.canDecanoAccessGrupo(USUARIO, GRUPO)).thenReturn(true);

        assertEquals(SESION, consultar().getSesion());
    }

    @Test
    void sesion_inexistente_responde_no_encontrada_sin_evaluar_ownership() {
        when(sesionRepositoryPort.consultarSesion(any())).thenReturn(null);

        assertThrows(ResourceNotFoundException.class, this::consultar);
        verifyNoInteractions(scope);
    }

    @Test
    void dominio_nulo_es_rechazado() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));
    }

    @Test
    void constructor_rechaza_puertos_nulos() {
        assertThrows(NullPointerException.class, () -> new ConsultarSesionUseCaseImpl(null, scope));
        assertThrows(NullPointerException.class, () -> new ConsultarSesionUseCaseImpl(sesionRepositoryPort, null));
    }
}
