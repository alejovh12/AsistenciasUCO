package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.ConsultarConfiguracionReclamosInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.primaryports.interactor.ConsultarConfiguracionReclamosInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.ConsultarConfiguracionReclamosUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarconfiguracionreclamos.usecase.impl.ConsultarConfiguracionReclamosUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.ConsultarAsistenciasPorGrupoInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.primaryports.interactor.ConsultarAsistenciasPorGrupoInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.ConsultarAsistenciasPorGrupoUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.consultarasistenciasporgrupo.usecase.impl.ConsultarAsistenciasPorGrupoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.RegistrarAsistenciasSesionInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.primaryports.interactor.RegistrarAsistenciasSesionInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.RegistrarAsistenciasSesionUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.registrarasistenciassesion.usecase.impl.RegistrarAsistenciasSesionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.ResolverSolicitudRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.primaryports.interactor.ResolverSolicitudRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.ResolverSolicitudRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.resolversolicitudrevisionasistencia.usecase.impl.ResolverSolicitudRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.SolicitarRevisionAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.primaryports.interactor.SolicitarRevisionAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.SolicitarRevisionAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.asistencia.solicitarrevisionasistencia.usecase.impl.SolicitarRevisionAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AsistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionProcedenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration(proxyBeanMethods = false)
public class AsistenciaWiringConfiguration {

    @Bean
    public ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort
    ) {
        return new ConsultarAsistenciasPorGrupoUseCaseImpl(asistenciaRepositoryPort, institutionalScopePort);
    }

    @Bean
    public ConsultarAsistenciasPorGrupoInputPort consultarAsistenciasPorGrupoInputPort(
            final ConsultarAsistenciasPorGrupoUseCase consultarAsistenciasPorGrupoUseCase
    ) {
        return new ConsultarAsistenciasPorGrupoInteractor(consultarAsistenciasPorGrupoUseCase);
    }

    @Bean
    public RegistrarAsistenciasSesionUseCase registrarAsistenciasSesionUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final SesionProcedenciaQueryPort sesionProcedenciaQueryPort,
            final InstitutionalScopePort institutionalScopePort,
            final RealtimePublisherPort realtimePublisherPort,
            final EvaluadorAlertasInasistencia evaluadorAlertasInasistencia
    ) {
        // SES-01: el reloj autoritativo de la ventana de registro es el del servidor en UTC.
        return new RegistrarAsistenciasSesionUseCaseImpl(
                asistenciaRepositoryPort,
                sesionProcedenciaQueryPort,
                institutionalScopePort,
                realtimePublisherPort,
                Clock.systemUTC(),
                evaluadorAlertasInasistencia
        );
    }

    @Bean
    public RegistrarAsistenciasSesionInputPort registrarAsistenciasSesionInputPort(
            final RegistrarAsistenciasSesionUseCase registrarAsistenciasSesionUseCase
    ) {
        return new RegistrarAsistenciasSesionInteractor(registrarAsistenciasSesionUseCase);
    }

    @Bean
    public SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort,
            final org.springframework.beans.factory.ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort> notificacionPortProvider,
            final org.springframework.beans.factory.ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort> destinatariosPortProvider
    ) {
        return new SolicitarRevisionAsistenciaUseCaseImpl(
                asistenciaRepositoryPort,
                institutionalScopePort,
                notificacionPortProvider.getIfAvailable(() -> notificacion -> {}),
                destinatariosPortProvider.getIfAvailable(() -> null)
        );
    }

    @Bean
    public SolicitarRevisionAsistenciaInputPort solicitarRevisionAsistenciaInputPort(
            final SolicitarRevisionAsistenciaUseCase solicitarRevisionAsistenciaUseCase
    ) {
        return new SolicitarRevisionAsistenciaInteractor(solicitarRevisionAsistenciaUseCase);
    }

    /** CULM-001 D.2: configuracion del formulario de reclamos (ESTUDIANTE y DOCENTE; el rol lo valida la base de datos). */
    @Bean
    public ConsultarConfiguracionReclamosUseCase consultarConfiguracionReclamosUseCase(
            final co.edu.uco.asistenciasuco.application.secondaryports.academic.ConfiguracionReclamosQueryPort configuracionReclamosQueryPort
    ) {
        return new ConsultarConfiguracionReclamosUseCaseImpl(configuracionReclamosQueryPort);
    }

    @Bean
    public ConsultarConfiguracionReclamosInputPort consultarConfiguracionReclamosInputPort(
            final ConsultarConfiguracionReclamosUseCase consultarConfiguracionReclamosUseCase
    ) {
        return new ConsultarConfiguracionReclamosInteractor(consultarConfiguracionReclamosUseCase);
    }

    @Bean
    public ResolverSolicitudRevisionAsistenciaUseCase resolverSolicitudRevisionAsistenciaUseCase(
            final AsistenciaRepositoryPort asistenciaRepositoryPort,
            final InstitutionalScopePort institutionalScopePort,
            final org.springframework.beans.factory.ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort> notificacionPortProvider,
            final org.springframework.beans.factory.ObjectProvider<co.edu.uco.asistenciasuco.application.secondaryports.academic.DestinatariosReclamoPort> destinatariosPortProvider,
            final EvaluadorAlertasInasistencia evaluadorAlertasInasistencia
    ) {
        return new ResolverSolicitudRevisionAsistenciaUseCaseImpl(
                asistenciaRepositoryPort,
                institutionalScopePort,
                notificacionPortProvider.getIfAvailable(() -> notificacion -> {}),
                destinatariosPortProvider.getIfAvailable(() -> null),
                evaluadorAlertasInasistencia
        );
    }

    @Bean
    public ResolverSolicitudRevisionAsistenciaInputPort resolverSolicitudRevisionAsistenciaInputPort(
            final ResolverSolicitudRevisionAsistenciaUseCase resolverSolicitudRevisionAsistenciaUseCase
    ) {
        return new ResolverSolicitudRevisionAsistenciaInteractor(resolverSolicitudRevisionAsistenciaUseCase);
    }
}
