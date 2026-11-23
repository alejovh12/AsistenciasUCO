package co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ConflictException;
import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort.Comando;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort.Resultado;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.Notificacion;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.DestinatarioReceso;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort.RecesoCreado;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/** DEC-081: crear un dia de receso; la DB valida y cancela, el caso de uso avisa (un correo por persona) y publica por grupo. */
class CrearRecesoAcademicoUseCaseImplTest {

    private static final UUID PERIODO = UUID.randomUUID();
    private static final UUID ADMIN = UUID.randomUUID();
    private static final UUID RECESO = UUID.randomUUID();
    private static final UUID GRUPO_A = UUID.randomUUID();
    private static final UUID GRUPO_B = UUID.randomUUID();
    private static final UUID SESION_A = UUID.randomUUID();
    private static final UUID SESION_B = UUID.randomUUID();
    private static final UUID ESTUDIANTE = UUID.randomUUID();
    private static final UUID DOCENTE = UUID.randomUUID();
    private static final LocalDate FECHA = LocalDate.of(2026, 12, 3);
    private static final String MENSAJE = "Se registró el receso académico del 03/12/2026; se cancelaron 2 clases.";

    private final RecesoAcademicoRepositoryPort repositorio = mock(RecesoAcademicoRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);
    private final CrearRecesoAcademicoUseCaseImpl useCase = new CrearRecesoAcademicoUseCaseImpl(repositorio, notificaciones, realtime);

    private static Comando comando() {
        return new Comando(PERIODO, FECHA, "  Jornada institucional  ", ADMIN);
    }

    private static DestinatarioReceso fila(final String rol, final UUID usuario, final String nombre, final UUID sesion, final UUID grupo,
                                           final String asignatura, final String nombreGrupo, final int horaUtc) {
        return new DestinatarioReceso(rol, usuario, nombre, nombre.toLowerCase().replace(' ', '.') + "@uco.edu.co", sesion, grupo,
                asignatura, nombreGrupo, FECHA, LocalDateTime.of(2026, 12, 3, horaUtc, 0), LocalDateTime.of(2026, 12, 3, horaUtc + 2, 0),
                "Receso académico: Jornada institucional");
    }

    private void creado(final int canceladas) {
        when(repositorio.crear(any(), eq(PERIODO), eq(FECHA), eq("Jornada institucional"), eq(ADMIN)))
                .thenReturn(new RecesoCreado(RECESO, canceladas, 0, MENSAJE));
    }

    @Test
    void agrupa_las_clases_por_persona_y_encola_un_correo_por_estudiante_y_por_docente() {
        creado(3);
        final UUID sesion2 = UUID.randomUUID();
        // el estudiante tiene dos clases (08:00 y 10:00 Bogota); el docente da la primera; las filas llegan desordenadas y una repetida
        when(repositorio.consultarDestinatarios(RECESO, ADMIN)).thenReturn(List.of(
                fila("ESTUDIANTE", ESTUDIANTE, "Ana Gómez", sesion2, GRUPO_B, "Bases de Datos", "G02", 15),
                fila("ESTUDIANTE", ESTUDIANTE, "Ana Gómez", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13),
                fila("ESTUDIANTE", ESTUDIANTE, "Ana Gómez", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13),
                fila("DOCENTE", DOCENTE, "María Rostagno", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13)));

        final Resultado resultado = useCase.execute(comando());

        final ArgumentCaptor<Notificacion> avisos = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones, times(2)).notificar(avisos.capture());
        final Notificacion estudiante = avisos.getAllValues().get(0);
        assertEquals(Notificacion.TIPO_RECESO_CLASES_CANCELADAS_ESTUDIANTE, estudiante.tipo());
        assertEquals(1, estudiante.destinatarios().size());
        assertEquals(ESTUDIANTE, estudiante.destinatarios().getFirst().idUsuario());
        assertEquals("Ana Gómez", estudiante.datos().get("estudiante"));
        assertEquals("03/12/2026", estudiante.datos().get("fecha"));
        assertEquals("Jornada institucional", estudiante.datos().get("motivo"));
        // una sola linea, ordenada por hora, en hora de Bogota (UTC-5): 13:00Z = 08:00 y 15:00Z = 10:00
        assertEquals("Ingeniería de Software, G01, 08:00 a 10:00; Bases de Datos, G02, 10:00 a 12:00", estudiante.datos().get("clases"));
        final Notificacion docente = avisos.getAllValues().get(1);
        assertEquals(Notificacion.TIPO_RECESO_CLASES_CANCELADAS_DOCENTE, docente.tipo());
        assertEquals("María Rostagno", docente.datos().get("docente"));
        assertEquals("Ingeniería de Software, G01, 08:00 a 10:00", docente.datos().get("clases"));
        assertEquals(2, resultado.avisosEncolados());
        assertEquals(3, resultado.sesionesCanceladas());
        assertEquals(MENSAJE, resultado.mensajeUsuario());
        assertEquals(RECESO, resultado.id());
        assertEquals("Jornada institucional", resultado.motivo());
    }

    @Test
    void publica_un_evento_por_grupo_afectado_con_tipo_receso() {
        creado(3);
        final UUID otraSesionDelGrupoA = UUID.randomUUID();
        when(repositorio.consultarDestinatarios(RECESO, ADMIN)).thenReturn(List.of(
                fila("DOCENTE", DOCENTE, "María Rostagno", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13),
                fila("DOCENTE", DOCENTE, "María Rostagno", otraSesionDelGrupoA, GRUPO_A, "Ingeniería de Software", "G01", 15),
                new DestinatarioReceso("DOCENTE", UUID.randomUUID(), "Luis Mora", "l@uco.edu.co", SESION_B, GRUPO_B, "X", "G09", FECHA,
                        null, null, null),
                new DestinatarioReceso("ESTUDIANTE", ESTUDIANTE, "Sin grupo", "s@uco.edu.co", SESION_B, null, "X", "G09", FECHA, null,
                        null, null)));

        final Resultado resultado = useCase.execute(comando());

        final ArgumentCaptor<RealtimeEvent> eventos = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtime, times(2)).publish(eventos.capture());
        final RealtimeEvent primero = eventos.getAllValues().get(0);
        assertEquals("SESION_CANCELADA", primero.type());
        assertEquals(GRUPO_A.toString(), primero.payload().get("grupo"));
        assertEquals(SESION_A.toString(), primero.payload().get("sesion"));
        assertEquals("RECESO", primero.payload().get("tipo"));
        assertEquals("2026-12-03T13:00:00Z", primero.payload().get("fechaHoraInicioUtc"));
        final RealtimeEvent segundo = eventos.getAllValues().get(1);
        assertEquals(GRUPO_B.toString(), segundo.payload().get("grupo"));
        assertTrue(!segundo.payload().containsKey("fechaHoraInicioUtc"));
        assertEquals(2, resultado.gruposNotificados());
    }

    @Test
    void sin_sesiones_canceladas_no_envia_correos_ni_eventos_ni_consulta_destinatarios() {
        creado(0);

        final Resultado resultado = useCase.execute(comando());

        verify(repositorio, never()).consultarDestinatarios(any(), any());
        verifyNoInteractions(notificaciones);
        verifyNoInteractions(realtime);
        assertEquals(0, resultado.sesionesCanceladas());
        assertEquals(0, resultado.avisosEncolados());
        assertEquals(0, resultado.gruposNotificados());
        assertEquals(MENSAJE, resultado.mensajeUsuario());
    }

    @Test
    void un_fallo_al_notificar_no_revierte_el_receso_y_se_sigue_con_las_demas_personas() {
        creado(1);
        when(repositorio.consultarDestinatarios(RECESO, ADMIN)).thenReturn(List.of(
                fila("ESTUDIANTE", ESTUDIANTE, "Ana Gómez", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13),
                fila("DOCENTE", DOCENTE, "María Rostagno", SESION_A, GRUPO_A, "Ingeniería de Software", "G01", 13)));
        doThrow(new IllegalStateException("outbox caido")).doNothing().when(notificaciones).notificar(any());
        doThrow(new IllegalStateException("sse caido")).when(realtime).publish(any());

        final Resultado resultado = useCase.execute(comando());

        verify(notificaciones, times(2)).notificar(any());
        assertEquals(1, resultado.avisosEncolados());
        assertEquals(0, resultado.gruposNotificados());
        assertEquals(1, resultado.sesionesCanceladas());
    }

    @Test
    void un_fallo_al_consultar_los_destinatarios_no_revierte_el_receso() {
        creado(1);
        when(repositorio.consultarDestinatarios(RECESO, ADMIN)).thenThrow(new IllegalStateException("bd"));

        final Resultado resultado = useCase.execute(comando());

        verifyNoInteractions(notificaciones);
        assertEquals(0, resultado.avisosEncolados());
        assertEquals(0, resultado.gruposNotificados());
        assertEquals(1, resultado.sesionesCanceladas());
    }

    @Test
    void las_filas_sin_usuario_se_omiten_y_los_datos_nulos_se_muestran_vacios() {
        creado(1);
        when(repositorio.consultarDestinatarios(RECESO, ADMIN)).thenReturn(List.of(
                new DestinatarioReceso("ESTUDIANTE", null, "Sin usuario", null, SESION_A, GRUPO_A, "X", "G", FECHA, null, null, null),
                new DestinatarioReceso("ESTUDIANTE", ESTUDIANTE, null, "a@uco.edu.co", SESION_A, GRUPO_A, null, null, FECHA, null, null,
                        null)));

        useCase.execute(comando());

        final ArgumentCaptor<Notificacion> aviso = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificaciones).notificar(aviso.capture());
        assertEquals("", aviso.getValue().datos().get("estudiante"));
        assertEquals(", ,  a ", aviso.getValue().datos().get("clases"));
    }

    @Test
    void un_rechazo_de_la_db_se_propaga_sin_avisar() {
        when(repositorio.crear(any(), any(), any(), any(), any())).thenThrow(new ConflictException(MensajeUsuarioErrorCode.REC_006));

        assertThrows(ConflictException.class, () -> useCase.execute(comando()));

        verifyNoInteractions(notificaciones);
        verifyNoInteractions(realtime);
    }

    @Test
    void valida_la_forma_de_la_entrada_antes_de_llamar_a_la_db() {
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(null, FECHA, "m", ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PERIODO, null, "m", ADMIN)));
        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PERIODO, FECHA, "m", null)));
        assertThrows(NullPointerException.class, () -> useCase.execute(null));
        verifyNoInteractions(repositorio);
    }

    @Test
    void un_motivo_nulo_se_entrega_vacio_para_que_la_db_responda_rec_008() {
        when(repositorio.crear(any(), eq(PERIODO), eq(FECHA), eq(""), eq(ADMIN)))
                .thenThrow(new ValidationException(MensajeUsuarioErrorCode.REC_008));

        assertThrows(ValidationException.class, () -> useCase.execute(new Comando(PERIODO, FECHA, null, ADMIN)));
    }

    @Test
    void los_puertos_son_obligatorios() {
        assertThrows(NullPointerException.class, () -> new CrearRecesoAcademicoUseCaseImpl(null, notificaciones, realtime));
        assertThrows(NullPointerException.class, () -> new CrearRecesoAcademicoUseCaseImpl(repositorio, null, realtime));
        assertThrows(NullPointerException.class, () -> new CrearRecesoAcademicoUseCaseImpl(repositorio, notificaciones, null));
    }
}