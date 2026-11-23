package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.domain.CorreoSaliente;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.soporte.Datos;
import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.PlantillaRepoFake;
import co.edu.uco.notificaciones.soporte.RelojFijo;
import co.edu.uco.notificaciones.soporte.TransporteFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.IntStream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EnviadorCorreoTest {

    private PlantillaRepoFake repo;
    private MetricasFake metricas;
    private TransporteFake transporte;
    private RelojFijo reloj;

    @BeforeEach
    void preparar() {
        repo = Datos.repoConTipoSaludo();
        metricas = new MetricasFake();
        transporte = new TransporteFake();
        reloj = new RelojFijo(Datos.AHORA);
    }

    private EnviadorCorreo enviador(final boolean modoPrueba, final List<String> redirecciones, final int maxBcc, final int porMinuto) {
        final var canal = Datos.canal(modoPrueba, redirecciones, maxBcc);
        final var renderizador = new RenderizadorPlantillas(repo, metricas, canal.platformUrl(), "{{contenido}}{{pie}}");
        return new EnviadorCorreo(repo, renderizador, transporte, new TopeEnvios(porMinuto, 500, reloj, metricas), canal, metricas);
    }

    @Test
    void enviaUnCorreoConLosDestinatariosEnBccYMessageIdEstable() {
        final Notificacion n = Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "a@uco.edu.co", " b@uco.edu.co ", "a@uco.edu.co", "malo");
        enviador(false, List.of(), 50, 60).enviar(n);
        assertEquals(1, transporte.enviados.size());
        final CorreoSaliente c = transporte.enviados.getFirst();
        assertEquals(List.of("a@uco.edu.co", "b@uco.edu.co"), c.bccAddresses());
        assertEquals("noreply@uco.edu.co", c.toAddress());
        assertEquals("Hola Ana", c.asunto());
        assertEquals("<notif-" + n.id() + "-0@uco.edu.co>", c.messageId());
        assertTrue(metricas.contiene("notification.email.send.total|outcome,sent"));
        assertTrue(metricas.contiene("notification.email.recipients.total|outcome,omitted|2.0"));
    }

    @Test
    void partePorBloquesDeBccYConsumeUnCupoPorBloque() {
        final String[] correos = IntStream.range(0, 5).mapToObj(i -> "u" + i + "@uco.edu.co").toArray(String[]::new);
        enviador(false, List.of(), 2, 60).enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), correos));
        assertEquals(List.of(2, 2, 1), transporte.enviados.stream().map(c -> c.bccAddresses().size()).toList());
        assertTrue(transporte.enviados.get(2).messageId().endsWith("-2@uco.edu.co>"));
    }

    @Test
    void modoPruebaRedirigeYAvisaLosDestinatariosOriginales() {
        final Notificacion n = Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "real@uco.edu.co");
        enviador(true, List.of("prueba@uco.edu.co"), 50, 60).enviar(n);
        final CorreoSaliente c = transporte.enviados.getFirst();
        assertEquals(List.of("prueba@uco.edu.co"), c.bccAddresses());
        assertEquals("[PRUEBA para real@uco.edu.co] Hola Ana", c.asunto());
        assertTrue(c.textoPlano().startsWith("MODO DE PRUEBAS: este correo no se envió a los destinatarios reales. Destinatarios originales (1): real@uco.edu.co."));
        assertTrue(metricas.contiene("notification.email.redirected.total"));
    }

    @Test
    void prefijoDePruebaResumeLosDestinatariosQueNoCaben() {
        assertEquals("[PRUEBA para a@uco.edu.co] ", EnviadorCorreo.prefijoAsuntoPrueba(List.of("a@uco.edu.co")));
        assertEquals("[PRUEBA para a@uco.edu.co, b@uco.edu.co] ", EnviadorCorreo.prefijoAsuntoPrueba(List.of("a@uco.edu.co", "b@uco.edu.co")));
        final List<String> muchos = IntStream.range(0, 20).mapToObj(i -> "persona.numero" + i + "@universidad.edu.co").toList();
        final String prefijo = EnviadorCorreo.prefijoAsuntoPrueba(muchos);
        assertTrue(prefijo.length() <= EnviadorCorreo.MAX_PREFIJO_ASUNTO_PRUEBA, prefijo);
        assertTrue(prefijo.contains(" más] "), prefijo);
    }

    @Test
    void rechazaTiposNoEntregablesYSinDestinatarios() {
        final EnviadorCorreo enviador = enviador(false, List.of(), 50, 60);
        final NoEntregableException tipo = assertThrows(NoEntregableException.class,
                () -> enviador.enviar(Datos.notificacion("RETIRADO", Map.of(), "a@uco.edu.co")));
        assertTrue(tipo.getMessage().contains("tipos entregables"));
        assertThrows(NoEntregableException.class, () -> enviador.enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "malo", "otro malo")));
        assertThrows(NoEntregableException.class, () -> enviador.enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"))));
        assertTrue(transporte.enviados.isEmpty());
        assertTrue(metricas.contiene("outcome,no_recipients"));
    }

    @Test
    void sinPlantillaNiAsuntoGuardadoNoEsEntregable() {
        final Notificacion sinTexto = new Notificacion(java.util.UUID.randomUUID(), "a", "k", "PRUEBA_CANAL_NOTIFICACION", "es", null, null,
                List.of(new co.edu.uco.notificaciones.domain.Destinatario("a@uco.edu.co", null)), Map.of(),
                co.edu.uco.notificaciones.domain.EstadoEnvio.RESERVADA, 1, Datos.AHORA, null, null, null, null, null, null, null, null, null);
        assertThrows(NoEntregableException.class, () -> enviador(false, List.of(), 50, 60).enviar(sinTexto));
    }

    @Test
    void elTipoDePruebaGenericoSeEntregaConElTextoGuardado() {
        enviador(false, List.of(), 50, 60).enviar(Datos.notificacion("PRUEBA_CANAL_NOTIFICACION", Map.of(), "a@uco.edu.co"));
        assertEquals("Asunto guardado", transporte.enviados.getFirst().asunto());
    }

    @Test
    void topeAgotadoEsFalloTransitorio() {
        final EnviadorCorreo enviador = enviador(false, List.of(), 1, 1);
        final Notificacion dos = Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "a@uco.edu.co", "b@uco.edu.co");
        assertThrows(IllegalStateException.class, () -> enviador.enviar(dos));
        assertEquals(1, transporte.enviados.size());
        assertFalse(enviador.disponible());
    }

    @Test
    void propagaFallosPermanentesYTransitoriosDelTransporte() {
        final EnviadorCorreo enviador = enviador(false, List.of(), 50, 60);
        transporte.comportamiento = c -> {
            throw new NoEntregableException("5xx");
        };
        assertThrows(NoEntregableException.class, () -> enviador.enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "a@uco.edu.co")));
        transporte.comportamiento = c -> {
            throw new IllegalStateException("red", new java.net.SocketException());
        };
        assertThrows(IllegalStateException.class, () -> enviador.enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "a@uco.edu.co")));
        transporte.comportamiento = c -> {
            throw new IllegalStateException("sin causa");
        };
        assertThrows(IllegalStateException.class, () -> enviador.enviar(Datos.notificacion("SALUDO", Map.of("nombre", "Ana"), "a@uco.edu.co")));
        assertTrue(metricas.contiene("outcome,permanent"));
        assertTrue(metricas.contiene("outcome,retry"));
    }

    @Test
    void disponibleDependeDelTransporte() {
        transporte.activo = false;
        assertFalse(enviador(false, List.of(), 50, 60).disponible());
    }

    @Test
    void sanearAsuntoLimpiaSaltosYRecorta() {
        assertEquals("", EnviadorCorreo.sanearAsunto(null));
        assertEquals("a  b", EnviadorCorreo.sanearAsunto(" a\r\nb "));
        assertEquals(200, EnviadorCorreo.sanearAsunto("x".repeat(300)).length());
    }
}
