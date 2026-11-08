package co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones;

import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.primaryports.interactor.AdministrarNotificacionesInteractor;
import co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.impl.AdministrarNotificacionesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.admin.AdministracionNotificacionPort;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoMoreInteractions;

class AdministrarNotificacionesUseCaseImplTest {

    private final AdministracionNotificacionPort port = mock(AdministracionNotificacionPort.class);
    private final AdministrarNotificacionesUseCaseImpl useCase = new AdministrarNotificacionesUseCaseImpl(port);
    private final AdministrarNotificacionesInteractor interactor = new AdministrarNotificacionesInteractor(useCase);
    private final UUID admin = UUID.randomUUID();

    @Test
    void exigeDependencias() {
        assertThrows(NullPointerException.class, () -> new AdministrarNotificacionesUseCaseImpl(null));
        assertThrows(NullPointerException.class, () -> new AdministrarNotificacionesInteractor(null));
    }

    @Test
    void delegaConsultasEnElPuertoSecundario() {
        when(port.consultarPlantillas("es", admin)).thenReturn(List.of());
        assertEquals(List.of(), interactor.consultarPlantillas("es", admin));
        interactor.consultarEstadoCanal();
        interactor.consultarCola("TODOS", "TODOS", 1, 20, admin);
        interactor.crearVersionPlantilla("T", "es", "a", "c", admin);
        interactor.activarVersionPlantilla("T", "es", 2, admin);
        interactor.generarVistaPrevia("T", "es", "a", "c");
        final UUID id = UUID.randomUUID();
        interactor.reintentarNotificacion(id, admin);
        verify(port).consultarEstadoCanal();
        verify(port).consultarCola("TODOS", "TODOS", 1, 20, admin);
        verify(port).crearVersionPlantilla("T", "es", "a", "c", admin);
        verify(port).activarVersionPlantilla("T", "es", 2, admin);
        verify(port).generarVistaPrevia("T", "es", "a", "c");
        verify(port).reintentarNotificacion(id, admin);
    }

    @Test
    void correoDePruebaConTipoUsaLaFirmaConTipo() {
        interactor.enviarCorreoPrueba(admin, "VERIFICACION_CORREO", "a@b.co");
        verify(port).enviarCorreoPrueba(admin, "VERIFICACION_CORREO", "a@b.co");
    }

    @Test
    void correoDePruebaSinTipoUsaLaFirmaSimple() {
        interactor.enviarCorreoPrueba(admin, " ", "a@b.co");
        verify(port).enviarCorreoPrueba(admin, "a@b.co");
        verifyNoMoreInteractions(port);
    }

    /** CULM-001 D.9-a (F-ADM-029): estado publico del canal a partir de la cola; sin datos de la cuenta tecnica. */
    @Test
    void estadoCanalPublico_activo_con_ultima_entrega_y_sin_error_si_la_falla_es_anterior() {
        final java.time.Instant entrega = java.time.Instant.parse("2026-11-08T14:00:00Z");
        when(port.consultarEstadoCanal()).thenReturn(estado(true, true));
        when(port.consultarCola("ENTREGADA", null, 1, 1, admin)).thenReturn(pagina(cola("ENTREGADA", entrega, entrega, null)));
        when(port.consultarCola("FALLIDA_DEFINITIVA", null, 1, 1, admin)).thenReturn(
                pagina(cola("FALLIDA_DEFINITIVA", java.time.Instant.parse("2026-11-08T10:00:00Z"), null, "SMTP_AUTH")));
        when(port.consultarCola("FALLIDA_REINTENTABLE", null, 1, 1, admin)).thenReturn(pagina());

        final var publico = interactor.consultarEstadoCanalPublico(admin);

        assertEquals(true, publico.canalActivo());
        assertEquals(entrega, publico.ultimoEnvioUtc());
        assertEquals(null, publico.ultimoError());
    }

    @Test
    void estadoCanalPublico_informa_el_error_mas_reciente_posterior_a_la_ultima_entrega() {
        final java.time.Instant entrega = java.time.Instant.parse("2026-11-08T09:00:00Z");
        when(port.consultarEstadoCanal()).thenReturn(estado(true, true));
        when(port.consultarCola("ENTREGADA", null, 1, 1, admin)).thenReturn(pagina(cola("ENTREGADA", entrega, entrega, null)));
        when(port.consultarCola("FALLIDA_DEFINITIVA", null, 1, 1, admin)).thenReturn(
                pagina(cola("FALLIDA_DEFINITIVA", java.time.Instant.parse("2026-11-08T10:00:00Z"), null, "SMTP_AUTH")));
        when(port.consultarCola("FALLIDA_REINTENTABLE", null, 1, 1, admin)).thenReturn(
                pagina(cola("FALLIDA_REINTENTABLE", java.time.Instant.parse("2026-11-08T11:00:00Z"), null, "SMTP_TIMEOUT")));

        assertEquals("SMTP_TIMEOUT", interactor.consultarEstadoCanalPublico(admin).ultimoError());
    }

    @Test
    void estadoCanalPublico_sin_historial_y_canal_inactivo() {
        when(port.consultarEstadoCanal()).thenReturn(estado(true, false));
        when(port.consultarCola("ENTREGADA", null, 1, 1, admin)).thenReturn(pagina());
        when(port.consultarCola("FALLIDA_DEFINITIVA", null, 1, 1, admin)).thenReturn(
                pagina(cola("FALLIDA_DEFINITIVA", java.time.Instant.parse("2026-11-08T10:00:00Z"), null, "SMTP_AUTH")));
        when(port.consultarCola("FALLIDA_REINTENTABLE", null, 1, 1, admin)).thenReturn(pagina());

        final var publico = interactor.consultarEstadoCanalPublico(admin);

        assertEquals(false, publico.canalActivo());
        assertEquals(null, publico.ultimoEnvioUtc());
        assertEquals("SMTP_AUTH", publico.ultimoError());
    }

    @Test
    void estadoCanalPublico_con_despachador_apagado_no_esta_activo() {
        when(port.consultarEstadoCanal()).thenReturn(estado(false, true));
        when(port.consultarCola(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.isNull(),
                org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(1), org.mockito.ArgumentMatchers.eq(admin))).thenReturn(pagina());

        assertEquals(false, interactor.consultarEstadoCanalPublico(admin).canalActivo());
    }

    private static co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection estado(
            final boolean despachador, final boolean disponible) {
        return new co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.EstadoCanalNotificacionProjection(
                "outbox", "smtp", despachador, disponible, "smtp.uco.edu.co", 587, "cuenta@uco.edu.co", true, "no-reply@uco.edu.co",
                "UCO", true, false, 50, 60, 500, false, "https://x", false, 0);
    }

    private static co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection cola(
            final String estado, final java.time.Instant creado, final java.time.Instant entregado, final String codigoError) {
        return new co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection(
                UUID.randomUUID(), "T", "a", estado, estado, false, 1, 1, creado, null, entregado, codigoError, null, null);
    }

    private static co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection pagina(
            final co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.NotificacionSalienteAdminProjection... filas) {
        return new co.edu.uco.asistenciasuco.application.features.admin.administrarnotificaciones.usecase.domain.PaginaColaNotificacionProjection(
                List.of(filas), 1, 1, filas.length, filas.length, null);
    }
}
