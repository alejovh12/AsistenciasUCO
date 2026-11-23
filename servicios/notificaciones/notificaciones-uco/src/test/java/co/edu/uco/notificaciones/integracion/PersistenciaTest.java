package co.edu.uco.notificaciones.integracion;

import co.edu.uco.notificaciones.application.model.FiltroNotificaciones;
import co.edu.uco.notificaciones.application.model.PaginaNotificaciones;
import co.edu.uco.notificaciones.application.model.ResultadoRegistro;
import co.edu.uco.notificaciones.application.model.ResultadoReintento;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.application.port.out.PlantillaRepositoryPort;
import co.edu.uco.notificaciones.domain.Destinatario;
import co.edu.uco.notificaciones.domain.EstadoEnvio;
import co.edu.uco.notificaciones.domain.Marcador;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.domain.VersionPlantilla;
import co.edu.uco.notificaciones.infrastructure.adapter.out.persistence.NotificacionJpaRepository;
import co.edu.uco.notificaciones.infrastructure.adapter.out.persistence.NotificacionRepositoryAdapter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Adaptadores JPA contra SQL Server real: idempotencia, reserva con lease, backoff, depuracion y versionado de plantillas. */
@SpringBootTest
class PersistenciaTest extends BaseIntegracion {

    private static final Instant T0 = Instant.parse("2026-11-20T10:00:00Z");

    @Autowired
    private NotificacionRepositoryPort cola;
    @Autowired
    private PlantillaRepositoryPort plantillas;
    @Autowired
    private NotificacionJpaRepository jpa;

    @AfterEach
    void limpiar() {
        jpa.deleteAll();
    }

    private Notificacion nueva(final String aplicacion, final String clave, final Instant creadoEn) {
        return new Notificacion(UUID.randomUUID(), aplicacion, clave, "SESION_CANCELADA", "es", "Asunto ñ", "Mensaje",
                List.of(new Destinatario("ana@uco.edu.co", "Ana María")), Map.of("estudiante", "Ana"), EstadoEnvio.PENDIENTE, 0, creadoEn, creadoEn,
                null, null, null, null, null, null, "corr-1", "00-0af7651916cd43dd8448eb211c80319c-b7ad6b7169203331-01");
    }

    @Test
    void laSiembraDeFabricaTraeLosTiposMarcadoresYLaVersionUno() {
        assertEquals(22, plantillas.listarTipos().size());
        assertTrue(plantillas.buscarTipo("PRUEBA_CANAL_NOTIFICACION").orElseThrow().interno());
        assertTrue(plantillas.buscarTipo("SOLICITUD_INGRESO_REGISTRADA").isEmpty());
        assertTrue(plantillas.buscarTipo(null).isEmpty());
        final List<Marcador> marcadores = plantillas.marcadores("SESION_SOLICITUD_RECHAZADA_DOCENTE");
        assertEquals(9, marcadores.size());
        assertTrue(marcadores.stream().allMatch(m -> m.obligatorio() && !m.ejemplo().isBlank()));
        assertEquals(1, plantillas.activa("RECESO_CLASES_CANCELADAS_DOCENTE", "es").orElseThrow().version());
        assertTrue(plantillas.marcadoresPorTipo().containsKey("FALTA_ASISTENCIA_SESION_ESTUDIANTE"));
        assertEquals(22, plantillas.versionesPorTipo("es").size());
        assertTrue(plantillas.version("SESION_CANCELADA", "es", 1).isPresent());
        assertTrue(plantillas.version("SESION_CANCELADA", "es", 7).isEmpty());
    }

    @Test
    void versionarActivarYVolverAUnaAnterior() {
        final VersionPlantilla dos = plantillas.crearYActivar("ALERTA_PERDIDA_INASISTENCIA", "es", "Asunto {grupo}", "Cuerpo {grupo} {estudiante}", "admin-1", T0);
        assertEquals(2, dos.version());
        assertTrue(dos.activa());
        final VersionPlantilla tres = plantillas.crearYActivar("ALERTA_PERDIDA_INASISTENCIA", "es", "Tres {grupo}", "Cuerpo {grupo} {estudiante}", null, T0);
        assertEquals(3, tres.version());
        final List<VersionPlantilla> historial = plantillas.versiones("ALERTA_PERDIDA_INASISTENCIA", "es");
        assertEquals(List.of(3, 2, 1), historial.stream().map(VersionPlantilla::version).toList());
        assertEquals(List.of(true, false, false), historial.stream().map(VersionPlantilla::activa).toList());
        final VersionPlantilla vuelta = plantillas.activar("ALERTA_PERDIDA_INASISTENCIA", "es", 1).orElseThrow();
        assertTrue(vuelta.activa());
        assertEquals(1, plantillas.activa("ALERTA_PERDIDA_INASISTENCIA", "es").orElseThrow().version());
        assertEquals(1, plantillas.versiones("ALERTA_PERDIDA_INASISTENCIA", "es").stream().filter(VersionPlantilla::activa).count());
        assertEquals(1, plantillas.activar("ALERTA_PERDIDA_INASISTENCIA", "es", 1).orElseThrow().version());
        assertTrue(plantillas.activar("ALERTA_PERDIDA_INASISTENCIA", "es", 99).isEmpty());
        assertEquals("admin-1", historial.get(1).creadoPor());
        plantillas.activar("ALERTA_PERDIDA_INASISTENCIA", "es", 1);
    }

    @Test
    void laClaveDeIdempotenciaNoDuplicaNiEnCarrera() throws Exception {
        final Notificacion n = nueva("app-idem", "k-1", T0);
        final ResultadoRegistro primera = cola.registrarSiNoExiste(n);
        assertTrue(primera.creada());
        assertEquals("Ana María", primera.notificacion().destinatarios().getFirst().nombre());
        assertEquals("Ana", primera.notificacion().datos().get("estudiante"));
        final ResultadoRegistro repetida = cola.registrarSiNoExiste(nueva("app-idem", "k-1", T0));
        assertFalse(repetida.creada());
        assertEquals(n.id(), repetida.notificacion().id());
        assertTrue(cola.registrarSiNoExiste(nueva("otra-app", "k-1", T0)).creada());
        assertTrue(cola.buscarPorId(n.id()).isPresent());
        assertTrue(cola.buscarPorId(UUID.randomUUID()).isEmpty());

        final ExecutorService pool = Executors.newFixedThreadPool(6);
        final CountDownLatch salida = new CountDownLatch(1);
        final List<Future<ResultadoRegistro>> futuros = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            final Callable<ResultadoRegistro> tarea = () -> {
                salida.await();
                return cola.registrarSiNoExiste(nueva("app-carrera", "k-carrera", T0));
            };
            futuros.add(pool.submit(tarea));
        }
        salida.countDown();
        final List<ResultadoRegistro> resultados = new ArrayList<>();
        for (final Future<ResultadoRegistro> f : futuros) {
            resultados.add(f.get());
        }
        pool.shutdown();
        assertEquals(1, resultados.stream().filter(ResultadoRegistro::creada).count());
        assertEquals(1, resultados.stream().map(r -> r.notificacion().id()).distinct().count());
    }

    @Test
    void reservarCuentaIntentosRespetaElLeaseYElOrden() {
        final Notificacion a = nueva("app-res", "a", T0);
        final Notificacion b = nueva("app-res", "b", T0.plusSeconds(1));
        final Notificacion c = nueva("app-res", "c", T0.plusSeconds(2));
        final Notificacion futura = new Notificacion(UUID.randomUUID(), "app-res", "f", "SESION_CANCELADA", "es", null, null, List.of(), Map.of(),
                EstadoEnvio.PENDIENTE, 0, T0, T0.plusSeconds(3600), null, null, null, null, null, null, null, null);
        for (final Notificacion n : List.of(c, a, b, futura)) {
            cola.registrarSiNoExiste(n);
        }
        final List<Notificacion> lote = cola.reservar(2, Duration.ofSeconds(60), "inst-A", T0.plusSeconds(10));
        assertEquals(List.of(a.id(), b.id()), lote.stream().map(Notificacion::id).toList());
        assertTrue(lote.stream().allMatch(n -> n.estado() == EstadoEnvio.RESERVADA && n.intentos() == 1 && "inst-A".equals(n.reservadoPor())));
        assertEquals(T0.plusSeconds(70), lote.getFirst().reservadoHasta());

        // Otra instancia solo ve lo que no esta reservado y vigente; la que aun no toca (proximoIntentoEn) tampoco.
        assertEquals(List.of(c.id()), cola.reservar(10, Duration.ofSeconds(60), "inst-B", T0.plusSeconds(20)).stream().map(Notificacion::id).toList());
        assertTrue(cola.reservar(10, Duration.ofSeconds(60), "inst-B", T0.plusSeconds(21)).isEmpty());

        // Con el lease vencido se vuelve a reservar y cuenta otro intento.
        final List<Notificacion> vencidas = cola.reservar(10, Duration.ofSeconds(60), "inst-B", T0.plusSeconds(200));
        assertEquals(3, vencidas.size());
        assertTrue(vencidas.stream().allMatch(n -> n.intentos() == 2 && "inst-B".equals(n.reservadoPor())));
    }

    @Test
    void entregarFallarConBackoffLiberarYReencolar() {
        final Notificacion a = nueva("app-est", "a", T0);
        final Notificacion b = nueva("app-est", "b", T0);
        final Notificacion c = nueva("app-est", "c", T0);
        for (final Notificacion n : List.of(a, b, c)) {
            cola.registrarSiNoExiste(n);
        }
        cola.reservar(10, Duration.ofSeconds(60), "inst-A", T0);

        assertFalse(cola.marcarEntregada(a.id(), "otra-instancia", T0));
        assertTrue(cola.marcarEntregada(a.id(), "inst-A", T0.plusSeconds(1)));
        assertFalse(cola.marcarEntregada(a.id(), "inst-A", T0.plusSeconds(2)));
        final Notificacion entregada = cola.buscarPorId(a.id()).orElseThrow();
        assertEquals(EstadoEnvio.ENTREGADA, entregada.estado());
        assertEquals(T0.plusSeconds(1), entregada.entregadoEn());
        assertNull(entregada.reservadoPor());

        assertFalse(cola.registrarFallo(b.id(), "otra-instancia", "X", "x", true, 5, Duration.ofSeconds(30), Duration.ofMinutes(15), T0));
        assertFalse(cola.registrarFallo(UUID.randomUUID(), "inst-A", "X", "x", true, 5, Duration.ofSeconds(30), Duration.ofMinutes(15), T0));
        assertTrue(cola.registrarFallo(b.id(), "inst-A", "FALLO_REINTENTABLE", "x".repeat(400), true, 5, Duration.ofSeconds(30), Duration.ofMinutes(15), T0.plusSeconds(5)));
        final Notificacion reintentable = cola.buscarPorId(b.id()).orElseThrow();
        assertEquals(EstadoEnvio.FALLIDA_REINTENTABLE, reintentable.estado());
        assertEquals(T0.plusSeconds(35), reintentable.proximoIntentoEn());
        assertEquals(300, reintentable.detalleError().length());
        assertEquals(T0.plusSeconds(5), reintentable.falloEn());

        assertTrue(cola.registrarFallo(c.id(), "inst-A", null, null, false, 5, Duration.ofSeconds(30), Duration.ofMinutes(15), T0));
        final Notificacion definitiva = cola.buscarPorId(c.id()).orElseThrow();
        assertEquals(EstadoEnvio.FALLIDA_DEFINITIVA, definitiva.estado());
        assertEquals("ERR_DESCONOCIDO", definitiva.codigoError());
        assertNull(definitiva.proximoIntentoEn());

        // Backoff exponencial acotado: intento 3 => 30 s * 4; intento 20 => tope de 15 min.
        assertEquals(Duration.ofSeconds(120), NotificacionRepositoryAdapter.calcularBackoff(3, Duration.ofSeconds(30), Duration.ofMinutes(15)));
        assertEquals(Duration.ofMinutes(15), NotificacionRepositoryAdapter.calcularBackoff(20, Duration.ofSeconds(30), Duration.ofMinutes(15)));

        assertEquals("FALLO_REINTENTABLE", cola.ultimoFallo().orElseThrow().codigo());

        assertEquals(ResultadoReintento.NO_EXISTE, cola.reencolar(UUID.randomUUID(), T0));
        assertEquals(ResultadoReintento.ESTADO_INVALIDO, cola.reencolar(a.id(), T0));
        assertEquals(ResultadoReintento.REENCOLADA, cola.reencolar(c.id(), T0.plusSeconds(60)));
        final Notificacion reencolada = cola.buscarPorId(c.id()).orElseThrow();
        assertEquals(EstadoEnvio.PENDIENTE, reencolada.estado());
        assertEquals(0, reencolada.intentos());
        assertNull(reencolada.codigoError());
        assertEquals(ResultadoReintento.REENCOLADA, cola.reencolar(b.id(), T0.plusSeconds(60)));

        // Liberar devuelve la reserva sin contar el intento.
        final List<Notificacion> reservadas = cola.reservar(10, Duration.ofSeconds(60), "inst-C", T0.plusSeconds(61));
        assertEquals(2, reservadas.size());
        assertEquals(1, reservadas.getFirst().intentos());
        assertEquals(0, cola.liberar(List.of(reservadas.getFirst().id()), "otra", T0));
        assertEquals(2, cola.liberar(reservadas.stream().map(Notificacion::id).toList(), "inst-C", T0.plusSeconds(62)));
        final Notificacion liberada = cola.buscarPorId(reservadas.getFirst().id()).orElseThrow();
        assertEquals(EstadoEnvio.PENDIENTE, liberada.estado());
        assertEquals(0, liberada.intentos());

        // Ultimo envio / ultimo fallo.
        assertEquals(T0.plusSeconds(1), cola.ultimaEntrega().orElseThrow());
        assertTrue(cola.ultimoFallo().isEmpty(), "reencolar deja sin vigencia el error anterior");
    }

    @Test
    void consultaPaginadaConFiltrosYResumen() {
        for (int i = 0; i < 5; i++) {
            cola.registrarSiNoExiste(nueva("app-q", "q" + i, T0.plusSeconds(i)));
        }
        cola.registrarSiNoExiste(new Notificacion(UUID.randomUUID(), "app-q2", "z", "RETIRO_ESTUDIANTE_GRUPO", "es", null, null, List.of(), Map.of(),
                EstadoEnvio.PENDIENTE, 0, T0.plusSeconds(10), T0, null, null, null, null, null, null, null, null));
        final PaginaNotificaciones pagina1 = cola.consultar(new FiltroNotificaciones(null, null, "app-q"), 1, 2);
        assertEquals(5, pagina1.totalElementos());
        assertEquals(3, pagina1.totalPaginas());
        assertEquals(List.of("q4", "q3"), pagina1.elementos().stream().map(Notificacion::idempotencyKey).toList());
        final PaginaNotificaciones pagina3 = cola.consultar(new FiltroNotificaciones(null, null, "app-q"), 3, 2);
        assertEquals(1, pagina3.elementos().size());
        assertEquals(5, pagina3.resumen().pendientes());
        assertEquals(1, cola.consultar(new FiltroNotificaciones("PENDIENTE", "RETIRO_ESTUDIANTE_GRUPO", null), 1, 20).totalElementos());
        assertEquals(0, cola.consultar(new FiltroNotificaciones("ENTREGADA", null, null), 1, 20).totalElementos());
        assertEquals(1, cola.consultar(new FiltroNotificaciones(null, "RETIRO_ESTUDIANTE_GRUPO", null), 1, 20).resumen().pendientes());
        assertEquals(1, cola.consultar(new FiltroNotificaciones(null, null, "app-q2"), 1, 20).resumen().pendientes());
        assertEquals(1, cola.consultar(new FiltroNotificaciones(null, null, "sin-datos"), 1, 20).totalPaginas());
    }

    @Test
    void depuraSoloLasTerminalesAnteriores() {
        final Notificacion vieja = nueva("app-dep", "vieja", T0.minus(Duration.ofDays(41)));
        final Notificacion pendienteVieja = nueva("app-dep", "pendiente", T0.minus(Duration.ofDays(40)));
        final Notificacion reciente = nueva("app-dep", "reciente", T0);
        for (final Notificacion n : List.of(vieja, pendienteVieja, reciente)) {
            cola.registrarSiNoExiste(n);
        }
        cola.reservar(1, Duration.ofSeconds(60), "inst-A", T0.minus(Duration.ofDays(40)));
        cola.marcarEntregada(vieja.id(), "inst-A", T0);
        assertEquals(0, cola.depurar(T0.minus(Duration.ofDays(60)), 100));
        assertEquals(1, cola.depurar(T0.minus(Duration.ofDays(30)), 100));
        assertTrue(cola.buscarPorId(vieja.id()).isEmpty());
        assertTrue(cola.buscarPorId(pendienteVieja.id()).isPresent());
        assertNotNull(cola.buscarPorId(reciente.id()).orElseThrow());
        assertEquals(Optional.empty(), cola.buscarPorId(vieja.id()));
    }
}
