package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.CrearSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.primaryports.interactor.CrearSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.sesion.crearsesion.usecase.CrearSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.primaryports.AprobarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.aprobarsolicitud.usecase.impl.AprobarSolicitudSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.primaryports.CancelarSesionCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.cancelarsesioncoordinador.usecase.impl.CancelarSesionCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.primaryports.ConsultarMotivosCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.primaryports.ConsultarSolicitudesSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarsolicitudes.usecase.impl.ConsultarSolicitudesSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.consultarmotivos.usecase.impl.ConsultarMotivosCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.NotificarSesionManualInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.primaryports.interactor.CrearSesionConAvisoInteractor;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.notificarsesionmanual.usecase.impl.NotificarSesionManualUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.primaryports.RechazarSolicitudSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.sesionmanual.rechazarsolicitud.usecase.impl.RechazarSolicitudSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionManualRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

/**
 * Composition Root de las sesiones creadas manualmente por el docente (CULM-001 / DEC-064, HU047; DEC-065 (2)): aviso inmediato,
 * solicitud con aval del coordinador (consultar, aprobar, rechazar), cancelacion por el coordinador y catalogo de motivos. El {@code CrearSesionInputPort} que reciben los controladores es
 * el decorador ({@link Primary}); el interactor original de {@link SesionWiringConfiguration} no cambia.
 */
@Configuration(proxyBeanMethods = false)
public class SesionManualWiringConfiguration {

    @Bean
    public NotificarSesionManualInputPort notificarSesionManualInputPort(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final SesionManualRepositoryPort sesionManualRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new NotificarSesionManualUseCaseImpl(sesionProcedenciaQueryPort, sesionManualRepositoryPort, notificacionPort,
                realtimePublisherPort);
    }

    @Bean
    @Primary
    public CrearSesionInputPort crearSesionConAvisoInputPort(
            final CrearSesionUseCase crearSesionUseCase,
            final NotificarSesionManualInputPort notificarSesionManualInputPort
    ) {
        return new CrearSesionConAvisoInteractor(new CrearSesionInteractor(crearSesionUseCase), notificarSesionManualInputPort);
    }

    @Bean
    public CancelarSesionCoordinadorInputPort cancelarSesionCoordinadorInputPort(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final SesionManualRepositoryPort sesionManualRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new CancelarSesionCoordinadorUseCaseImpl(sesionProcedenciaQueryPort, sesionManualRepositoryPort, notificacionPort,
                realtimePublisherPort);
    }

    @Bean
    public AprobarSolicitudSesionInputPort aprobarSolicitudSesionInputPort(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final SesionManualRepositoryPort sesionManualRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new AprobarSolicitudSesionUseCaseImpl(sesionProcedenciaQueryPort, sesionManualRepositoryPort, notificacionPort,
                realtimePublisherPort);
    }

    @Bean
    public RechazarSolicitudSesionInputPort rechazarSolicitudSesionInputPort(
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final SesionManualRepositoryPort sesionManualRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new RechazarSolicitudSesionUseCaseImpl(sesionProcedenciaQueryPort, sesionManualRepositoryPort, notificacionPort,
                realtimePublisherPort);
    }

    @Bean
    public ConsultarSolicitudesSesionInputPort consultarSolicitudesSesionInputPort(final SesionManualRepositoryPort sesionManualRepositoryPort) {
        return new ConsultarSolicitudesSesionUseCaseImpl(sesionManualRepositoryPort);
    }

    @Bean
    public ConsultarMotivosCancelacionInputPort consultarMotivosCancelacionInputPort(final SesionManualRepositoryPort sesionManualRepositoryPort) {
        return new ConsultarMotivosCancelacionUseCaseImpl(sesionManualRepositoryPort);
    }
}
