package co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistencia.usecase.domain.RegistrarAsistenciaDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeEvent;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.dto.RegistrarAsistenciaRepositoryDTO;
import co.edu.uco.asistenciasuco.crosscutting.exception.CrosscuttingException;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * Cubre la integracion del caso de uso con {@link RealtimePublisherPort}: el evento realtime
 * solo se publica DESPUES de que la persistencia principal haya terminado exitosamente, y un
 * caso de uso fallido (excepcion de persistencia) nunca publica evento.
 */
class RegistrarAsistenciaUseCaseImplTest {

    private final AsistenciaRepositoryPort asistenciaRepositoryPort = mock(AsistenciaRepositoryPort.class);
    private final RealtimePublisherPort realtimePublisherPort = mock(RealtimePublisherPort.class);
    private final RegistrarAsistenciaUseCaseImpl useCase =
            new RegistrarAsistenciaUseCaseImpl(asistenciaRepositoryPort, realtimePublisherPort);

    @Test
    void execute_rechaza_dominio_nulo_y_no_publica_evento() {
        assertThrows(CrosscuttingException.class, () -> useCase.execute(null));

        verify(realtimePublisherPort, never()).publish(any());
    }

    @Test
    void execute_exitoso_persiste_y_luego_publica_el_evento_realtime_en_ese_orden() {
        final RegistrarAsistenciaDomain domain = dominioValido();

        useCase.execute(domain);

        final InOrder order = inOrder(asistenciaRepositoryPort, realtimePublisherPort);
        order.verify(asistenciaRepositoryPort).registrarAsistencia(any(RegistrarAsistenciaRepositoryDTO.class));
        order.verify(realtimePublisherPort).publish(any(RealtimeEvent.class));
    }

    @Test
    void execute_exitoso_publica_evento_con_tipo_y_payload_de_negocio_no_sensible() {
        final RegistrarAsistenciaDomain domain = dominioValido();

        useCase.execute(domain);

        final ArgumentCaptor<RealtimeEvent> captor = ArgumentCaptor.forClass(RealtimeEvent.class);
        verify(realtimePublisherPort).publish(captor.capture());
        final RealtimeEvent published = captor.getValue();
        assertEquals("ASISTENCIA_REGISTRADA", published.type());
        assertEquals(domain.getEstudiante().toString(), published.payload().get("estudiante"));
        assertEquals(domain.getGrupo().toString(), published.payload().get("grupo"));
        assertEquals(domain.getSesion().toString(), published.payload().get("sesion"));
        assertEquals(true, published.payload().get("presente"));
        // El evento de negocio no debe llevar tokens, contrasenas ni datos de sesion de seguridad.
        assertEquals(4, published.payload().size());
    }

    @Test
    void execute_fallido_por_error_de_persistencia_no_publica_evento_realtime() {
        final RegistrarAsistenciaDomain domain = dominioValido();
        doThrow(new RuntimeException("fallo de persistencia"))
                .when(asistenciaRepositoryPort).registrarAsistencia(any(RegistrarAsistenciaRepositoryDTO.class));

        assertThrows(RuntimeException.class, () -> useCase.execute(domain));

        verify(realtimePublisherPort, never()).publish(any());
    }

    private RegistrarAsistenciaDomain dominioValido() {
        return new RegistrarAsistenciaDomain(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), true, null);
    }
}
