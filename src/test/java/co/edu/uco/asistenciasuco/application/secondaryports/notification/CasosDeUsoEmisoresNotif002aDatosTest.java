package co.edu.uco.asistenciasuco.application.secondaryports.notification;

import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.domain.ResolverSolicitudIngresoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.resolversolicitudingreso.usecase.impl.ResolverSolicitudIngresoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.domain.RetirarEstudianteGrupoDomain;
import co.edu.uco.asistenciasuco.application.features.docente.retirarestudiantegrupo.usecase.impl.RetirarEstudianteGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.retirarsolicitudingreso.usecase.impl.RetirarSolicitudIngresoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.estudiante.solicitaringresogrupo.usecase.impl.SolicitarIngresoGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.RetiroEstudianteGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.SolicitudIngresoGrupoPort;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioRetiroProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DestinatarioSolicitudIngresoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TEST_PLAN §2.6 (BE-U01 y BE-U02): verifica que los 4 casos de uso emisores incluyan las claves nuevas
 * del CONTRACT §4.4 manteniendo intactos tipo, asunto, mensaje y las claves previas de datos.
 */
class CasosDeUsoEmisoresNotif002aDatosTest {

    @Test
    @DisplayName("BE-U01 y BE-U02 (1/4): RetirarEstudianteGrupoUseCaseImpl agrega grupo y estudiante preservando claves y textos previos")
    void BE_U01_U02_retiro_estudiante_grupo() {
        final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
        final RetiroEstudianteGrupoPort retiroPort = mock(RetiroEstudianteGrupoPort.class);
        final NotificacionPort notificacionPort = mock(NotificacionPort.class);

        final UUID actor = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        final UUID estudianteId = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(retiroPort.consultarDestinatarios(grupoId, estudianteId, actor)).thenReturn(List.of(
                new DestinatarioRetiroProjection("ESTUDIANTE", UUID.randomUUID(), "Ana María Gómez", "ana@uco.edu.co",
                        grupoId, 1, "G01", "Ingeniería de Software"),
                new DestinatarioRetiroProjection("DOCENTE", actor, "Docente Titular", "doc@uco.edu.co",
                        grupoId, 1, "G01", "Ingeniería de Software")
        ));

        final RetirarEstudianteGrupoUseCaseImpl useCase = new RetirarEstudianteGrupoUseCaseImpl(scopePort, retiroPort, notificacionPort);
        useCase.execute(new RetirarEstudianteGrupoDomain(actor, grupoId, estudianteId, "Inasistencia del 20%"));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion n = captor.getValue();

        // BE-U02: tipo, asunto, mensaje y claves previas intactas
        assertEquals(Notificacion.TIPO_RETIRO_ESTUDIANTE_GRUPO, n.tipo());
        assertEquals("Retiro del grupo G01 de Ingeniería de Software", n.asunto());
        assertEquals("Se retiro a Ana María Gómez del grupo G01 de Ingeniería de Software. Motivo: Inasistencia del 20%", n.mensaje());
        assertEquals(grupoId.toString(), n.datos().get("idGrupo"));
        assertEquals(estudianteId.toString(), n.datos().get("idEstudiante"));
        assertEquals("Inasistencia del 20%", n.datos().get("motivo"));

        // BE-U01: claves nuevas CONTRACT §4.4
        assertEquals("G01 de Ingeniería de Software", n.datos().get("grupo"));
        assertEquals("Ana María Gómez", n.datos().get("estudiante"));
    }

    @Test
    @DisplayName("BE-U01 y BE-U02 (2/4): SolicitarIngresoGrupoUseCaseImpl agrega grupo y estudiante preservando claves y textos previos")
    void BE_U01_U02_solicitud_ingreso_registrada() {
        final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
        final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
        final NotificacionPort notificacionPort = mock(NotificacionPort.class);

        final UUID actor = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarDestinatarios(any(), eq(actor))).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", actor, "Carlos Andrés Ruiz", "carlos@uco.edu.co",
                        grupoId, 2, "G02", "Cálculo Diferencial"),
                new DestinatarioSolicitudIngresoProjection("DOCENTE", UUID.randomUUID(), "Docente", "doc@uco.edu.co",
                        grupoId, 2, "G02", "Cálculo Diferencial")
        ));

        final SolicitarIngresoGrupoUseCaseImpl useCase = new SolicitarIngresoGrupoUseCaseImpl(scopePort, port, notificacionPort);
        final UUID solicitudId = useCase.execute(actor, grupoId);

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion n = captor.getValue();

        // BE-U02: tipo, asunto, mensaje y claves previas intactas
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_REGISTRADA, n.tipo());
        assertEquals("Nueva solicitud de ingreso al grupo G02 de Cálculo Diferencial", n.asunto());
        assertEquals("Carlos Andrés Ruiz solicito ingresar al grupo G02 de Cálculo Diferencial.", n.mensaje());
        assertEquals(solicitudId.toString(), n.datos().get("idSolicitud"));
        assertEquals(grupoId.toString(), n.datos().get("idGrupo"));

        // BE-U01: claves nuevas CONTRACT §4.4
        assertEquals("G02 de Cálculo Diferencial", n.datos().get("grupo"));
        assertEquals("Carlos Andrés Ruiz", n.datos().get("estudiante"));
    }

    @Test
    @DisplayName("BE-U01 y BE-U02 (3/4): ResolverSolicitudIngresoUseCaseImpl agrega grupo y resultado (aceptada / puesta en espera / rechazada) preservando claves y textos previos")
    void BE_U01_U02_solicitud_ingreso_resuelta() {
        final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
        final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
        final NotificacionPort notificacionPort = mock(NotificacionPort.class);

        final UUID actor = UUID.randomUUID();
        final UUID solicitudId = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        when(scopePort.findDocenteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarDestinatarios(solicitudId, actor)).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", UUID.randomUUID(), "Laura Mejía", "laura@uco.edu.co",
                        grupoId, 1, "G01", "Bases de Datos")
        ));

        final ResolverSolicitudIngresoUseCaseImpl useCase = new ResolverSolicitudIngresoUseCaseImpl(scopePort, port, notificacionPort);

        // 1. ACEPTADA -> resultado = "aceptada"
        when(port.resolverSolicitud(solicitudId, "ACEPTAR", null, actor)).thenReturn("A");
        useCase.execute(new ResolverSolicitudIngresoDomain(actor, solicitudId, "ACEPTAR", null));

        // 2. EN_ESPERA -> resultado = "puesta en espera"
        when(port.resolverSolicitud(solicitudId, "ACEPTAR", null, actor)).thenReturn("E");
        useCase.execute(new ResolverSolicitudIngresoDomain(actor, solicitudId, "ACEPTAR", null));

        // 3. RECHAZADA -> resultado = "rechazada"
        when(port.resolverSolicitud(solicitudId, "RECHAZAR", "Sin prerrequisito", actor)).thenReturn("R");
        useCase.execute(new ResolverSolicitudIngresoDomain(actor, solicitudId, "RECHAZAR", "Sin prerrequisito"));

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort, times(3)).notificar(captor.capture());
        final List<Notificacion> emitidas = captor.getAllValues();

        // Asertar ACEPTADA
        final Notificacion nAceptada = emitidas.get(0);
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_RESUELTA, nAceptada.tipo());
        assertEquals("Solicitud de ingreso al grupo G01 de Bases de Datos", nAceptada.asunto());
        assertEquals("Tu solicitud de ingreso al grupo G01 de Bases de Datos fue aceptada: ya estas matriculado.", nAceptada.mensaje());
        assertEquals(solicitudId.toString(), nAceptada.datos().get("idSolicitud"));
        assertEquals("ACEPTADA", nAceptada.datos().get("estado"));
        assertEquals("G01 de Bases de Datos", nAceptada.datos().get("grupo"));
        assertEquals("aceptada", nAceptada.datos().get("resultado"));

        // Asertar EN_ESPERA
        final Notificacion nEspera = emitidas.get(1);
        assertEquals("EN_ESPERA", nEspera.datos().get("estado"));
        assertEquals("G01 de Bases de Datos", nEspera.datos().get("grupo"));
        assertEquals("puesta en espera", nEspera.datos().get("resultado"));

        // Asertar RECHAZADA
        final Notificacion nRechazada = emitidas.get(2);
        assertEquals("RECHAZADA", nRechazada.datos().get("estado"));
        assertEquals("Sin prerrequisito", nRechazada.datos().get("motivo"));
        assertEquals("G01 de Bases de Datos", nRechazada.datos().get("grupo"));
        assertEquals("rechazada", nRechazada.datos().get("resultado"));
    }

    @Test
    @DisplayName("BE-U01 y BE-U02 (4/4): RetirarSolicitudIngresoUseCaseImpl agrega grupo y estudiante preservando claves y textos previos")
    void BE_U01_U02_solicitud_ingreso_retirada() {
        final InstitutionalScopePort scopePort = mock(InstitutionalScopePort.class);
        final SolicitudIngresoGrupoPort port = mock(SolicitudIngresoGrupoPort.class);
        final NotificacionPort notificacionPort = mock(NotificacionPort.class);

        final UUID actor = UUID.randomUUID();
        final UUID solicitudId = UUID.randomUUID();
        final UUID grupoId = UUID.randomUUID();
        when(scopePort.findEstudianteIdByUsuario(actor)).thenReturn(Optional.of(UUID.randomUUID()));
        when(port.consultarDestinatarios(solicitudId, actor)).thenReturn(List.of(
                new DestinatarioSolicitudIngresoProjection("ESTUDIANTE", actor, "Sofía Restrepo", "sofia@uco.edu.co",
                        grupoId, 3, "G03", "Física Mecánica"),
                new DestinatarioSolicitudIngresoProjection("DOCENTE", UUID.randomUUID(), "Docente", "doc@uco.edu.co",
                        grupoId, 3, "G03", "Física Mecánica")
        ));

        final RetirarSolicitudIngresoUseCaseImpl useCase = new RetirarSolicitudIngresoUseCaseImpl(scopePort, port, notificacionPort);
        useCase.execute(actor, solicitudId);

        final ArgumentCaptor<Notificacion> captor = ArgumentCaptor.forClass(Notificacion.class);
        verify(notificacionPort).notificar(captor.capture());
        final Notificacion n = captor.getValue();

        // BE-U02: tipo, asunto, mensaje y claves previas intactas
        assertEquals(Notificacion.TIPO_SOLICITUD_INGRESO_RETIRADA, n.tipo());
        assertEquals("Solicitud de ingreso retirada del grupo G03 de Física Mecánica", n.asunto());
        assertEquals("Un estudiante retiro su solicitud de ingreso al grupo G03 de Física Mecánica.", n.mensaje());
        assertEquals(solicitudId.toString(), n.datos().get("idSolicitud"));

        // BE-U01: claves nuevas CONTRACT §4.4
        assertEquals("G03 de Física Mecánica", n.datos().get("grupo"));
        assertEquals("Sofía Restrepo", n.datos().get("estudiante"));
    }
}
