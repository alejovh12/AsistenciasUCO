package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.usecase.impl.CancelarSesionCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.usecase.impl.ConsultarMotivosCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.interactor.CrearSesionConAvisoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.impl.NotificarSesionManualUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.Primary;

import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;

/** CULM-001 / DEC-064: composition root de las sesiones manuales; el decorador de creacion es el bean primario. */
class SesionManualWiringConfigurationTest {

    private final SesionManualWiringConfiguration config = new SesionManualWiringConfiguration();
    private final SesionProcedenciaQueryPort sesiones = mock(SesionProcedenciaQueryPort.class);
    private final SesionManualRepositoryPort repositorio = mock(SesionManualRepositoryPort.class);
    private final NotificacionPort notificaciones = mock(NotificacionPort.class);
    private final RealtimePublisherPort realtime = mock(RealtimePublisherPort.class);

    @Test
    void los_beans_se_construyen_con_sus_implementaciones() {
        final var notificar = config.notificarSesionManualInputPort(sesiones, repositorio, notificaciones, realtime);
        assertInstanceOf(NotificarSesionManualUseCaseImpl.class, notificar);
        assertInstanceOf(CrearSesionConAvisoInteractor.class, config.crearSesionConAvisoInputPort(mock(CrearSesionUseCase.class), notificar));
        assertInstanceOf(CancelarSesionCoordinadorUseCaseImpl.class, config.cancelarSesionCoordinadorInputPort(sesiones, repositorio, realtime));
        assertInstanceOf(ConsultarMotivosCancelacionUseCaseImpl.class, config.consultarMotivosCancelacionInputPort(repositorio));
    }

    @Test
    void el_decorador_de_creacion_es_el_bean_primario() throws NoSuchMethodException {
        assertNotNull(SesionManualWiringConfiguration.class
                .getMethod("crearSesionConAvisoInputPort", CrearSesionUseCase.class,
                        co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort.class)
                .getAnnotation(Primary.class));
    }
}
