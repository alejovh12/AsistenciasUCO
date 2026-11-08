package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.AvisoAlertasInasistenciaService;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.common.EvaluadorAlertasInasistencia;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.primaryports.ConsultarAlertasEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.consultaralertasestudiante.usecase.impl.ConsultarAlertasEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.primaryports.EvaluarAlertasInasistenciaAutomaticoInputPort;
import co.edu.uco.asistenciasuco.application.features.alertainasistencia.evaluaralertasautomatico.usecase.impl.EvaluarAlertasInasistenciaAutomaticoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.primaryports.ResolverAudienciaRealtimeInputPort;
import co.edu.uco.asistenciasuco.application.features.realtime.resolveraudiencia.usecase.impl.ResolverAudienciaRealtimeUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimeAudienciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.AlertaInasistenciaRepositoryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.primary.scheduler.EvaluacionAlertasInasistenciaScheduler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Duration;

/** Composition Root de las alertas de inasistencia, el job de evaluacion y las audiencias del SSE por ambito (CULM-001 C.1, HU026C, F-TRX-006). */
@Configuration(proxyBeanMethods = false)
public class AlertaInasistenciaWiringConfiguration {

    @Bean
    public AvisoAlertasInasistenciaService avisoAlertasInasistenciaService(
            final AlertaInasistenciaRepositoryPort repositorio,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new AvisoAlertasInasistenciaService(repositorio, notificacionPort, realtimePublisherPort);
    }

    @Bean
    public EvaluadorAlertasInasistencia evaluadorAlertasInasistencia(final AvisoAlertasInasistenciaService servicio) {
        return servicio;
    }

    @Bean
    public ConsultarAlertasEstudianteInputPort consultarAlertasEstudianteInputPort(final AlertaInasistenciaRepositoryPort repositorio,
                                                                                 final MessageCatalogPort mensajes) {
        return new ConsultarAlertasEstudianteUseCaseImpl(repositorio, mensajes);
    }

    @Bean
    public EvaluarAlertasInasistenciaAutomaticoInputPort evaluarAlertasInasistenciaAutomaticoInputPort(
            final AlertaInasistenciaRepositoryPort repositorio,
            final EvaluadorAlertasInasistencia evaluador,
            final AvisoRevisionCancelacionService avisos
    ) {
        return new EvaluarAlertasInasistenciaAutomaticoUseCaseImpl(repositorio, evaluador, avisos);
    }

    @Bean
    @ConditionalOnProperty(name = "app.alertas-inasistencia.evaluacion.enabled", havingValue = "true", matchIfMissing = true)
    public EvaluacionAlertasInasistenciaScheduler evaluacionAlertasInasistenciaScheduler(
            final EvaluarAlertasInasistenciaAutomaticoInputPort evaluar,
            @Value("${app.alertas-inasistencia.evaluacion.tick:PT1M}") final Duration tick
    ) {
        return new EvaluacionAlertasInasistenciaScheduler(evaluar, tick);
    }

    @Bean
    public ResolverAudienciaRealtimeInputPort resolverAudienciaRealtimeInputPort(final RealtimeAudienciaQueryPort consulta) {
        return new ResolverAudienciaRealtimeUseCaseImpl(consulta);
    }

    @Bean
    public AvisoRevisionCancelacionService avisoRevisionCancelacionService(
            final RevisionCancelacionRepositoryPort repositorio,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new AvisoRevisionCancelacionService(repositorio, notificacionPort, realtimePublisherPort);
    }
}
