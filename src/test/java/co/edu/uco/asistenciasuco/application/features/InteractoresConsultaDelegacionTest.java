package co.edu.uco.asistenciasuco.application.features;

import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.dto.ResolverSolicitudRevisionAsistenciaDTO;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.interactor.ResolverSolicitudRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.mapper.ResolverSolicitudRevisionAsistenciaMapper;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.domain.ResolverSolicitudRevisionAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.primaryports.interactor.ConsultarConfiguracionRegistroEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.configuracion.consultarregistroestudiante.usecase.ConsultarConfiguracionRegistroEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.dto.ActualizarCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.primaryports.interactor.ActualizarCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.actualizarcoordinador.usecase.ActualizarCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.dto.CambiarEstadoCoordinadorDTO;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.primaryports.interactor.CambiarEstadoCoordinadorInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.cambiarestadocoordinador.usecase.CambiarEstadoCoordinadorUseCase;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.primaryports.interactor.ConsultarGruposDecanoInteractor;
import co.edu.uco.asistenciasuco.application.features.decano.consultargrupos.usecase.ConsultarGruposDecanoUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.primaryports.interactor.ConsultarAsignaturasDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarasignaturas.usecase.ConsultarAsignaturasDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.primaryports.interactor.ConsultarHorariosDocenteInteractor;
import co.edu.uco.asistenciasuco.application.features.docente.consultarhorarios.usecase.ConsultarHorariosDocenteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.dto.ConsultarEstudiantesDTO;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.primaryports.interactor.ConsultarEstudiantesInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.ConsultarEstudiantesUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.primaryports.interactor.ConsultarHorariosEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarhorarios.usecase.ConsultarHorariosEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.primaryports.interactor.ConsultarMateriasEstudianteInteractor;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarmaterias.usecase.ConsultarMateriasEstudianteUseCase;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.primaryports.interactor.ReenviarVerificacionCorreoInteractor;
import co.edu.uco.asistenciasuco.application.features.grupo.reenviarverificacion.usecase.ReenviarVerificacionCorreoUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.primaryports.interactor.VerificarCorreoInteractor;
import co.edu.uco.asistenciasuco.application.features.usuario.verificarcorreo.usecase.VerificarCorreoUseCase;
import co.edu.uco.asistenciasuco.application.features.estudiante.consultarestudiantes.usecase.entity.EstudiantePaginaEntity;
import co.edu.uco.asistenciasuco.application.features.grupo.consultargrupos.usecase.entity.GrupoEntity;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import co.edu.uco.asistenciasuco.testsupport.MuestraAnswer;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * M-10: interactores sin prueba propia. Cada uno delega en su caso de uso y convierte el resultado con su mapeador; los casos
 * de uso se sustituyen por dobles que devuelven datos de muestra para ejercitar el mapeo completo.
 */
class InteractoresConsultaDelegacionTest {

    private static final UUID ACTOR = UUID.randomUUID();

    @Test
    void consultar_grupos_decano_mapea_la_lista() {
        final var useCase = mock(ConsultarGruposDecanoUseCase.class);
        when(useCase.execute(ACTOR)).thenReturn(List.of(new GrupoEntity(UUID.randomUUID(), "G01", "Grupo 01", UUID.randomUUID(),
                "Calculo", UUID.randomUUID(), 30, 10, 20, true, LocalDate.of(2026, 1, 20), LocalDate.of(2026, 6, 1), List.of())));

        final var resultado = new ConsultarGruposDecanoInteractor(useCase).execute(ACTOR);

        verify(useCase).execute(ACTOR);
        assertEquals(1, resultado.size());
        assertEquals("G01", resultado.get(0).getCodigo());
    }

    @Test
    void consultar_asignaturas_docente_mapea_la_lista() {
        final var useCase = mock(ConsultarAsignaturasDocenteUseCase.class, new MuestraAnswer());

        final var resultado = new ConsultarAsignaturasDocenteInteractor(useCase).execute(ACTOR);

        verify(useCase).execute(ACTOR);
        assertEquals(1, resultado.size());
    }

    @Test
    void consultar_horarios_docente_mapea_la_lista() {
        final var useCase = mock(ConsultarHorariosDocenteUseCase.class, new MuestraAnswer());

        final var resultado = new ConsultarHorariosDocenteInteractor(useCase).execute(ACTOR);

        verify(useCase).execute(ACTOR);
        assertEquals(1, resultado.size());
    }

    @Test
    void consultar_horarios_estudiante_mapea_la_lista() {
        final var useCase = mock(ConsultarHorariosEstudianteUseCase.class, new MuestraAnswer());

        final var resultado = new ConsultarHorariosEstudianteInteractor(useCase).execute(ACTOR);

        verify(useCase).execute(ACTOR);
        assertEquals(1, resultado.size());
    }

    @Test
    void consultar_materias_estudiante_mapea_la_lista() {
        final var useCase = mock(ConsultarMateriasEstudianteUseCase.class, new MuestraAnswer());

        final var resultado = new ConsultarMateriasEstudianteInteractor(useCase).execute(ACTOR);

        verify(useCase).execute(ACTOR);
        assertEquals(1, resultado.size());
    }

    @Test
    void consultar_estudiantes_convierte_dto_a_dominio_y_pagina_a_dto() {
        final var useCase = mock(ConsultarEstudiantesUseCase.class);
        when(useCase.execute(any())).thenReturn(new EstudiantePaginaEntity(List.of(), 0L, 0, 2, 10));
        final var dto = (ConsultarEstudiantesDTO) MuestraAnswer.muestra(ConsultarEstudiantesDTO.class, 0);

        final var resultado = new ConsultarEstudiantesInteractor(useCase).execute(dto);

        verify(useCase).execute(any());
        assertEquals(2, resultado.page());
        assertEquals(10, resultado.size());
    }

    @Test
    void actualizar_coordinador_convierte_dto_a_dominio_y_resultado_a_dto() {
        final var useCase = mock(ActualizarCoordinadorUseCase.class, new MuestraAnswer());
        final var dto = (ActualizarCoordinadorDTO) MuestraAnswer.muestra(ActualizarCoordinadorDTO.class, 0);

        final var resultado = new ActualizarCoordinadorInteractor(useCase).execute(dto);

        verify(useCase).execute(any());
        assertNotNull(resultado);
    }

    @Test
    void cambiar_estado_coordinador_convierte_dto_a_dominio_y_resultado_a_dto() {
        final var useCase = mock(CambiarEstadoCoordinadorUseCase.class, new MuestraAnswer());
        final var dto = (CambiarEstadoCoordinadorDTO) MuestraAnswer.muestra(CambiarEstadoCoordinadorDTO.class, 0);

        final var resultado = new CambiarEstadoCoordinadorInteractor(useCase).execute(dto);

        verify(useCase).execute(any());
        assertNotNull(resultado);
    }

    @Test
    void consultar_configuracion_registro_estudiante_devuelve_el_valor_del_caso_de_uso() {
        final var useCase = mock(ConsultarConfiguracionRegistroEstudianteUseCase.class);
        when(useCase.execute()).thenReturn("@uco.edu.co");

        assertEquals("@uco.edu.co", new ConsultarConfiguracionRegistroEstudianteInteractor(useCase).execute());
    }

    @Test
    void reenviar_verificacion_correo_delega_con_los_tres_identificadores() {
        final var useCase = mock(ReenviarVerificacionCorreoUseCase.class);
        final UUID grupo = UUID.randomUUID();
        final UUID estudiante = UUID.randomUUID();

        new ReenviarVerificacionCorreoInteractor(useCase).execute(ACTOR, grupo, estudiante);

        verify(useCase).execute(ACTOR, grupo, estudiante);
    }

    @Test
    void verificar_correo_delega_token_y_contrasena_y_devuelve_el_mensaje() {
        final var useCase = mock(VerificarCorreoUseCase.class);
        when(useCase.execute("token", "Clave123!")).thenReturn("Correo verificado.");

        assertEquals("Correo verificado.", new VerificarCorreoInteractor(useCase).execute("token", "Clave123!"));
    }

    @Test
    void resolver_solicitud_revision_convierte_el_dto_y_delega() {
        final var useCase = mock(ResolverSolicitudRevisionAsistenciaUseCase.class);
        final var dto = new ResolverSolicitudRevisionAsistenciaDTO();
        dto.setSolicitud(UUID.randomUUID());
        dto.setAccion("APROBADA");
        dto.setRespuestaDocente("Se acepta.");
        dto.setResultado("PRESENTE");
        dto.setUsuario(ACTOR);

        new ResolverSolicitudRevisionAsistenciaInteractor(useCase).execute(dto);

        verify(useCase).execute(any(ResolverSolicitudRevisionAsistenciaDomain.class));
    }

    @Test
    void el_mapeador_de_resolver_solicitud_rechaza_un_dto_nulo() {
        assertThrows(CrosscuttingException.class, () -> ResolverSolicitudRevisionAsistenciaMapper.toDomain(null));
    }

    @Test
    void los_interactores_exigen_su_caso_de_uso() {
        assertThrows(NullPointerException.class, () -> new ConsultarGruposDecanoInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarAsignaturasDocenteInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarHorariosDocenteInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarHorariosEstudianteInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarMateriasEstudianteInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarEstudiantesInteractor(null));
        assertThrows(NullPointerException.class, () -> new ActualizarCoordinadorInteractor(null));
        assertThrows(NullPointerException.class, () -> new CambiarEstadoCoordinadorInteractor(null));
        assertThrows(NullPointerException.class, () -> new ConsultarConfiguracionRegistroEstudianteInteractor(null));
        assertThrows(NullPointerException.class, () -> new ReenviarVerificacionCorreoInteractor(null));
        assertThrows(NullPointerException.class, () -> new VerificarCorreoInteractor(null));
        assertThrows(NullPointerException.class, () -> new ResolverSolicitudRevisionAsistenciaInteractor(null));
    }
}
