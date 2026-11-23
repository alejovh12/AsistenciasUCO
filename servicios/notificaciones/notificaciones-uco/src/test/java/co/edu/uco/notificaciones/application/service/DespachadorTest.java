package co.edu.uco.notificaciones.application.service;

import co.edu.uco.notificaciones.application.model.ConfiguracionDespachador;
import co.edu.uco.notificaciones.application.port.out.NotificacionRepositoryPort;
import co.edu.uco.notificaciones.domain.NoEntregableException;
import co.edu.uco.notificaciones.domain.Notificacion;
import co.edu.uco.notificaciones.soporte.Datos;
import co.edu.uco.notificaciones.soporte.MetricasFake;
import co.edu.uco.notificaciones.soporte.PlantillaRepoFake;
import co.edu.uco.notificaciones.soporte.RelojFijo;
import co.edu.uco.notificaciones.soporte.TransporteFake;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DespachadorTest {

    private NotificacionRepositoryPort repo;
    private MetricasFake metricas;
    private TransporteFake transporte;
    private RelojFijo reloj;
    private PlantillaRepoFake plantillas;

    @BeforeEach
    void preparar() {
        repo = mock(NotificacionRepositoryPort.class);
        metricas = new MetricasFake();
        transporte = new TransporteFake();
        reloj = new RelojFijo(Datos.AHORA);
        plantillas = Datos.repoConTipoSaludo();
    }

    private Despachador despachador(final ConfiguracionDespachador config) {
        final var canal = Datos.canal(false, List.of(), 50);
        final var renderizador = new RenderizadorPlantillas(plantillas, metricas, "", "{{contenido}}{{pie}}");
        final var enviador = new EnviadorCorreo(plantillas, renderizador, transporte, new TopeEnvios(60, 500, reloj, metricas), canal, metricas);
        return new Despachador(repo, enviador, config, metricas, reloj);
    }

    private Notificacion valida(final int intentos) {
        return Datos.notificacion(UUID.randomUUID(), "SALUDO", Map.of("nombre", "Ana"), intentos, "a@uco.edu.co");
    }

    @Test
    void entregaLoReservadoYLoMarcaEntregada() {
        final Notificacion n = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(n));
        when(repo.marcarEntregada(eq(n.id()), eq("instancia-1"), any())).thenReturn(true);
        assertEquals(1, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
        assertEquals(1, transporte.enviados.size());
        verify(repo).marcarEntregada(eq(n.id()), eq("instancia-1"), any());
        assertTrue(metricas.contiene("notification.dispatch.total|outcome,delivered"));
    }

    @Test
    void siPerdioLaReservaNoCuentaComoEntregada() {
        final Notificacion n = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(n));
        when(repo.marcarEntregada(any(), anyString(), any())).thenReturn(false);
        despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo();
        assertTrue(metricas.contiene("outcome,lost_lease"));
        when(repo.marcarEntregada(any(), anyString(), any())).thenThrow(new IllegalStateException("bd"));
        despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo();
        assertEquals(2, metricas.cuenta("outcome,lost_lease"));
    }

    @Test
    void unFalloPermanenteQuedaDefinitivo() {
        final Notificacion n = Datos.notificacion(UUID.randomUUID(), "RETIRADO", Map.of(), 1, "a@uco.edu.co");
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(n));
        despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo();
        verify(repo).registrarFallo(eq(n.id()), eq("instancia-1"), eq("FALLO_DEFINITIVO"), anyString(), eq(false), eq(5),
                eq(Duration.ofSeconds(30)), eq(Duration.ofMinutes(15)), any());
        assertTrue(metricas.contiene("outcome,dead"));
    }

    @Test
    void unFalloTransitorioSeReintentaConBackoffYAlAgotarIntentosCuentaComoMuerta() {
        final Notificacion primera = valida(1);
        final Notificacion ultima = valida(5);
        transporte.comportamiento = c -> {
            throw new IllegalStateException("red");
        };
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(primera, ultima));
        despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo();
        verify(repo).registrarFallo(eq(primera.id()), anyString(), eq("FALLO_REINTENTABLE"), anyString(), eq(true), anyInt(), any(), any(), any());
        assertTrue(metricas.contiene("outcome,retry"));
        assertTrue(metricas.contiene("outcome,dead"));
    }

    @Test
    void siNoSePuedeRegistrarElFalloNoDetieneElCiclo() {
        final Notificacion n = valida(1);
        transporte.comportamiento = c -> {
            throw new IllegalStateException("red");
        };
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(n));
        when(repo.registrarFallo(any(), anyString(), anyString(), anyString(), anyBoolean(), anyInt(), any(), any(), any())).thenReturn(false);
        despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo();
        assertTrue(metricas.contiene("outcome,lost_lease"));
        when(repo.registrarFallo(any(), anyString(), anyString(), anyString(), anyBoolean(), anyInt(), any(), any(), any())).thenThrow(new IllegalStateException("bd"));
        assertEquals(1, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
    }

    @Test
    void noHaceNadaSiElCanalNoEstaDisponible() {
        transporte.activo = false;
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
        verify(repo, never()).reservar(anyInt(), any(), anyString(), any());
    }

    @Test
    void siNoSePuedeReservarOElLoteEstaVacioTermina() {
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenThrow(new IllegalStateException("bd"));
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
        doReturn(List.of()).when(repo).reservar(anyInt(), any(), anyString(), any());
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
        doReturn(null).when(repo).reservar(anyInt(), any(), anyString(), any());
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
    }

    /** Regresion del fallo de noviembre: con el lease por agotarse se corta el ciclo y se liberan las reservas restantes. */
    @Test
    void cortaElCicloCuandoElLeaseNoAlcanzaYLiberaLoNoProcesado() {
        final Notificacion primera = valida(1);
        final Notificacion segunda = valida(1);
        final Notificacion tercera = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(primera, segunda, tercera));
        when(repo.marcarEntregada(any(), anyString(), any())).thenReturn(true);
        // Lease de 20 s: margen = min(5 s, 5 s) = 5 s. El primer envio "dura" 16 s, asi que quedan 4 s < margen.
        transporte.comportamiento = c -> reloj.avanzar(Duration.ofSeconds(16));
        assertEquals(1, despachador(Datos.despachador(Duration.ofSeconds(20), 5)).ejecutarCiclo());
        assertEquals(1, transporte.enviados.size());
        @SuppressWarnings("unchecked")
        final ArgumentCaptor<List<UUID>> ids = ArgumentCaptor.forClass(List.class);
        verify(repo).liberar(ids.capture(), eq("instancia-1"), any());
        assertEquals(List.of(segunda.id(), tercera.id()), ids.getValue());
        assertTrue(metricas.contiene("outcome,skipped_lease|2.0") || metricas.contiene("outcome,skipped_lease"));
    }

    @Test
    void elMargenSeAdaptaAlEnvioMasLentoDelLote() {
        final Notificacion primera = valida(1);
        final Notificacion segunda = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(primera, segunda));
        when(repo.marcarEntregada(any(), anyString(), any())).thenReturn(true);
        // Lease de 60 s: margen base 5 s. El primer envio dura 20 s => margen adaptativo 40 s; restan 40 s: aun alcanza... pasa a 39.
        transporte.comportamiento = c -> reloj.avanzar(Duration.ofSeconds(21));
        assertEquals(1, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo());
        verify(repo).liberar(eq(List.of(segunda.id())), anyString(), any());
    }

    @Test
    void siSeDetieneElServicioLiberaLoQueQuedaSinProcesar() {
        final Notificacion primera = valida(1);
        final Notificacion segunda = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(primera, segunda));
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo(() -> false));
        verify(repo).liberar(eq(List.of(primera.id(), segunda.id())), anyString(), any());
        assertTrue(transporte.enviados.isEmpty());
    }

    @Test
    void siFallaLiberarElLeaseVenceSolo() {
        final Notificacion primera = valida(1);
        when(repo.reservar(anyInt(), any(), anyString(), any())).thenReturn(List.of(primera));
        when(repo.liberar(anyList(), anyString(), any())).thenThrow(new IllegalStateException("bd"));
        assertEquals(0, despachador(Datos.despachador(Duration.ofSeconds(60), 5)).ejecutarCiclo(() -> false));
    }

    @Test
    void depuraEntregasTerminalesAntiguas() {
        when(repo.depurar(any(Instant.class), anyInt())).thenReturn(3);
        final Despachador d = despachador(Datos.despachador(Duration.ofSeconds(60), 5));
        assertEquals(3, d.depurar());
        final ArgumentCaptor<Instant> limite = ArgumentCaptor.forClass(Instant.class);
        verify(repo).depurar(limite.capture(), eq(100));
        assertEquals(Datos.AHORA.minus(Duration.ofDays(30)), limite.getValue());
        assertTrue(metricas.contiene("notification.outbox.purged.total"));
        when(repo.depurar(any(Instant.class), anyInt())).thenThrow(new IllegalStateException("bd"));
        assertEquals(0, d.depurar());
    }

    @Test
    void depurarSiCorrespondeRespetaElIntervalo() {
        when(repo.depurar(any(Instant.class), anyInt())).thenReturn(0);
        final Despachador d = despachador(Datos.despachador(Duration.ofSeconds(60), 5));
        d.depurarSiCorresponde();
        d.depurarSiCorresponde();
        verify(repo).depurar(any(Instant.class), anyInt());
        reloj.avanzar(Duration.ofHours(7));
        d.depurarSiCorresponde();
        verify(repo, org.mockito.Mockito.times(2)).depurar(any(Instant.class), anyInt());
    }

    @Test
    void exigeDependencias() {
        final var config = Datos.despachador(Duration.ofSeconds(60), 5);
        assertThrows(NullPointerException.class, () -> new Despachador(null, null, config, metricas, reloj));
        assertEquals(config, despachador(config).configuracion());
    }

    @Test
    void noEntregableTieneMensaje() {
        assertEquals("x", new NoEntregableException("x").getMessage());
    }
}
