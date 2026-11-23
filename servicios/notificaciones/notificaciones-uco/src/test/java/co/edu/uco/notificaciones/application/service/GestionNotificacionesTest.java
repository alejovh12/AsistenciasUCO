package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.EstadoCanal;
import co.edu.uco.notificaciones.application.model.FalloReciente;
import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResultadoReintento;
import co.edu.uco.notificaciones.application.model.ResumenCola;
import co.edu.uco.notificaciones.application.model.SolicitudNotificacion;
import co.edu.uco.notificaciones.application.port.out.DespertadorPort;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.ErrorNegocio;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.soporte.Datos;
import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.PlantillaRepoFake;
import co.edu.uco.notificaciones.soporte.RelojFijo;
import co.edu.uco.notificaciones.soporte.TransporteFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GestionNotificacionesTest {

    private NotificacionRepositoryPort repo;
    private DespertadorPort despertador;
    private PlantillaRepoFake plantillas;
    private TransporteFake transporte;
    private MetricasFake metricas;
    private GestionNotificaciones gestion;

    @BeforeEach
    void preparar() {
        repo = mock(NotificacionRepositoryPort.class);
        despertador = mock(DespertadorPort.class);
        plantillas = Datos.repoConTipoSaludo();
        transporte = new TransporteFake();
        metricas = new MetricasFake();
        final var reloj = new RelojFijo(Datos.AHORA);
        final var canal = Datos.canal(true, List.of("p@uco.edu.co", "q@uco.edu.co"), 50);
        final var renderizador = new RenderizadorPlantillas(plantillas, metricas, "", "{{contenido}}{{pie}}");
        final var enviador = new EnviadorCorreo(plantillas, renderizador, transporte, new TopeEnvios(60, 500, reloj, metricas), canal, metricas);
        gestion = new GestionNotificaciones(repo, plantillas, despertador, metricas, enviador, canal, Datos.despachador(java.time.Duration.ofSeconds(60), 5), reloj);
        when(repo.registrarSiNoExiste(any())).thenAnswer(i -> new ResultadoRegistro(i.getArgument(0), true));
    }

    private SolicitudNotificacion solicitud() {
        return new SolicitudNotificacion("asistencias", "clave-1", "SALUDO", null, "As", "Msg",
                List.of(new Destinatario(" a@uco.edu.co ", " Ana "), new Destinatario("b@uco.edu.co", " ")), Map.of("nombre", "Ana"), "corr-1",
                "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    }

    private SolicitudNotificacion con(final java.util.function.UnaryOperator<SolicitudNotificacion> cambio) {
        return cambio.apply(solicitud());
    }

    private String codigo(final SolicitudNotificacion s) {
        return assertThrows(ErrorNegocio.class, () -> gestion.registrar(s)).codigo();
    }

    @Test
    void registraUnaNotificacionNuevaYDespiertaAlDespachador() {
        final ResultadoRegistro r = gestion.registrar(solicitud());
        assertTrue(r.creada());
        final Notificacion n = r.notificacion();
        assertEquals(EstadoEnvio.PENDIENTE, n.estado());
        assertEquals("es", n.idioma());
        assertEquals(0, n.intentos());
        assertEquals("a@uco.edu.co", n.destinatarios().getFirst().correo());
        assertEquals("Ana", n.destinatarios().getFirst().nombre());
        assertNull(n.destinatarios().get(1).nombre());
        assertEquals(Datos.AHORA, n.proximoIntentoEn());
        verify(despertador).despertar();
        assertTrue(metricas.contiene("resultado,nueva"));
    }

    @Test
    void unaClaveRepetidaDevuelveLaMismaYNoDespierta() {
        final Notificacion existente = Datos.notificacion("SALUDO", Map.of(), "a@uco.edu.co");
        when(repo.registrarSiNoExiste(any())).thenReturn(new ResultadoRegistro(existente, false));
        final ResultadoRegistro r = gestion.registrar(solicitud());
        assertFalse(r.creada());
        assertEquals(existente.id(), r.notificacion().id());
        verify(despertador, never()).despertar();
        assertTrue(metricas.contiene("resultado,duplicada"));
    }

    @Test
    void validaLosCamposDeLaSolicitud() {
        assertEquals("APLICACION_INVALIDA", codigo(con(s -> new SolicitudNotificacion(null, s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("APLICACION_INVALIDA", codigo(con(s -> new SolicitudNotificacion("app con espacios", s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("IDEMPOTENCY_KEY_INVALIDA", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), " ", s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("TIPO_NO_SOPORTADO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), "NO_EXISTE", s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("TIPO_NO_SOPORTADO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), null, s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("TIPO_NO_SOPORTADO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), "PRUEBA_CANAL_NOTIFICACION", s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("IDIOMA_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), "ESP", s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("CORRELACION_INVALIDA", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), "con espacio", s.traceparent()))));
        assertEquals("TRACEPARENT_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), "no-es-w3c"))));
        assertEquals("ASUNTO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), "x".repeat(201), s.mensaje(), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        assertEquals("MENSAJE_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), "x".repeat(4001), s.destinatarios(), s.datos(), s.idCorrelacion(), s.traceparent()))));
        verify(repo, never()).registrarSiNoExiste(any());
    }

    @Test
    void validaDestinatarios() {
        assertEquals("DESTINATARIOS_OBLIGATORIOS", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), null, s.datos(), null, null))));
        assertEquals("DESTINATARIOS_OBLIGATORIOS", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), List.of(), s.datos(), null, null))));
        final List<Destinatario> muchos = java.util.stream.IntStream.range(0, 201).mapToObj(i -> new Destinatario("u" + i + "@uco.edu.co", null)).toList();
        assertEquals("DESTINATARIOS_EXCEDIDOS", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), muchos, s.datos(), null, null))));
        assertEquals("DESTINATARIO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), List.of(new Destinatario("malo", null)), s.datos(), null, null))));
        assertEquals("DESTINATARIO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), java.util.Collections.singletonList(null), s.datos(), null, null))));
        assertEquals("DESTINATARIO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), List.of(new Destinatario("a@uco.edu.co", "n".repeat(201))), s.datos(), null, null))));
    }

    @Test
    void validaDatos() {
        final Map<String, String> muchos = new HashMap<>();
        for (int i = 0; i < 101; i++) {
            muchos.put("k" + i, "v");
        }
        assertEquals("DATOS_EXCEDIDOS", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), muchos, null, null))));
        assertEquals("DATO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), Map.of("mala clave", "v"), null, null))));
        assertEquals("DATO_INVALIDO", codigo(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), Map.of("ok", "x".repeat(2001)), null, null))));
        final Map<String, String> conNulo = new HashMap<>();
        conNulo.put("sinValor", null);
        conNulo.put("conValor", "v");
        final ResultadoRegistro r = gestion.registrar(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), conNulo, null, null)));
        assertEquals(Map.of("conValor", "v"), r.notificacion().datos());
        final ResultadoRegistro sinDatos = gestion.registrar(con(s -> new SolicitudNotificacion(s.aplicacion(), s.idempotencyKey(), s.tipo(), s.idioma(), s.asunto(), s.mensaje(), s.destinatarios(), null, null, null)));
        assertTrue(sinDatos.notificacion().datos().isEmpty());
    }

    @Test
    void consultaNormalizaPaginaYValidaElEstado() {
        final PaginaNotificaciones vacia = new PaginaNotificaciones(null, 1, 20, 0, 1, new ResumenCola(0, 0, 0, 0, 0));
        when(repo.consultar(any(), anyInt(), anyInt())).thenReturn(vacia);
        assertEquals(vacia, gestion.consultar(null, 0, 0));
        final ArgumentCaptor<FiltroNotificaciones> filtro = ArgumentCaptor.forClass(FiltroNotificaciones.class);
        gestion.consultar(new FiltroNotificaciones(" ENTREGADA ", " ", "app"), 3, 500);
        verify(repo).consultar(filtro.capture(), org.mockito.ArgumentMatchers.eq(3), org.mockito.ArgumentMatchers.eq(20));
        assertEquals("ENTREGADA", filtro.getValue().estado());
        assertNull(filtro.getValue().tipo());
        assertEquals("ESTADO_INVALIDO", assertThrows(ErrorNegocio.class, () -> gestion.consultar(new FiltroNotificaciones("RARO", null, null), 1, 20)).codigo());
    }

    @Test
    void reintentarSegunElResultado() {
        final UUID id = UUID.randomUUID();
        when(repo.reencolar(any(), any())).thenReturn(ResultadoReintento.NO_EXISTE);
        assertEquals("NOTIFICACION_NO_ENCONTRADA", assertThrows(ErrorNegocio.class, () -> gestion.reintentar(id)).codigo());
        when(repo.reencolar(any(), any())).thenReturn(ResultadoReintento.ESTADO_INVALIDO);
        assertEquals("ESTADO_NO_REINTENTABLE", assertThrows(ErrorNegocio.class, () -> gestion.reintentar(id)).codigo());
        verify(despertador, never()).despertar();
        when(repo.reencolar(any(), any())).thenReturn(ResultadoReintento.REENCOLADA);
        gestion.reintentar(id);
        verify(despertador).despertar();
    }

    @Test
    void correoDePruebaUsaLosEjemplosDelTipoYNoPideConfirmacionDelCliente() {
        final Notificacion n = gestion.enviarCorreoPrueba("SALUDO", " ana@uco.edu.co ", "admin-1", "corr-123");
        assertEquals("SALUDO", n.tipo());
        assertEquals("ejemplo-nombre", n.datos().get("nombre"));
        assertEquals("ana@uco.edu.co", n.destinatarios().getFirst().correo());
        assertEquals("notificaciones-admin", n.aplicacion());
        assertTrue(n.idempotencyKey().startsWith("prueba-"));
        assertEquals("corr-123", n.idCorrelacion());
        assertEquals("admin-1", n.datos().get("solicitadoPor"));
        verify(despertador).despertar();
    }

    @Test
    void correoDePruebaGeneraCorrelacionSoloSiNoLlegaYNoLaTomaDeSolicitadoPor() {
        final Notificacion n = gestion.enviarCorreoPrueba("SALUDO", "ana@uco.edu.co", "admin-1", "  ");
        assertNotNull(n.idCorrelacion());
        assertNotEquals("admin-1", n.idCorrelacion());
        assertEquals("admin-1", n.datos().get("solicitadoPor"));
    }

    @Test
    void correoDePruebaGenericoSinTipo() {
        final Notificacion n = gestion.enviarCorreoPrueba(null, "ana@uco.edu.co", null, "c-1");
        assertEquals("PRUEBA_CANAL_NOTIFICACION", n.tipo());
        assertTrue(n.asunto().contains("PRUEBA_CANAL_NOTIFICACION"));
        assertEquals("c-1", n.idCorrelacion());
        assertFalse(n.datos().containsKey("solicitadoPor"));
    }

    @Test
    void correoDePruebaValidaDestinoYTipo() {
        assertEquals("DESTINATARIO_INVALIDO", assertThrows(ErrorNegocio.class, () -> gestion.enviarCorreoPrueba("SALUDO", "malo", null, null)).codigo());
        assertEquals("TIPO_NO_ENCONTRADO", assertThrows(ErrorNegocio.class, () -> gestion.enviarCorreoPrueba("NO_EXISTE", "a@uco.edu.co", null, null)).codigo());
    }

    @Test
    void estadoCanalReportaUltimoEnvioYSoloElErrorPosterior() {
        final Instant entrega = Datos.AHORA.minusSeconds(60);
        when(repo.ultimaEntrega()).thenReturn(Optional.of(entrega));
        when(repo.ultimoFallo()).thenReturn(Optional.of(new FalloReciente(entrega.minusSeconds(10), "VIEJO")));
        EstadoCanal e = gestion.estadoCanal();
        assertTrue(e.canalActivo() && e.disponible() && e.dispatcherEnabled());
        assertNull(e.ultimoError());
        assertEquals(entrega, e.ultimoEnvioUtc());
        assertEquals(2, e.testModeRedirectCount());
        assertTrue(e.tienePasswordConfigurado());
        when(repo.ultimoFallo()).thenReturn(Optional.of(new FalloReciente(entrega.plusSeconds(5), "FALLO_REINTENTABLE")));
        assertEquals("FALLO_REINTENTABLE", gestion.estadoCanal().ultimoError());
        when(repo.ultimaEntrega()).thenReturn(Optional.empty());
        e = gestion.estadoCanal();
        assertNull(e.ultimoEnvioUtc());
        assertEquals("FALLO_REINTENTABLE", e.ultimoError());
        when(repo.ultimoFallo()).thenReturn(Optional.empty());
        assertNull(gestion.estadoCanal().ultimoError());
    }

    @Test
    void estadoCanalConTransporteInactivo() {
        transporte.activo = false;
        final EstadoCanal e = gestion.estadoCanal();
        assertFalse(e.canalActivo());
        assertFalse(e.disponible());
    }
}
