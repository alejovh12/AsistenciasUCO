package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.primaryports.ConsultarRecesosPeriodoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.consultarrecesos.usecase.impl.ConsultarRecesosPeriodoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.primaryports.CrearRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.crearreceso.usecase.impl.CrearRecesoAcademicoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.primaryports.RetirarRecesoAcademicoInputPort;
import co.edu.uco.asistenciasuco.application.features.recesoacademico.retirarreceso.usecase.impl.RetirarRecesoAcademicoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.notification.NotificacionPort;
import co.edu.uco.asistenciasuco.application.secondaryports.realtime.RealtimePublisherPort;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RecesoAcademicoRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root del receso academico del periodo (DEC-081): crear con avisos, retirar y consultar. */
@Configuration(proxyBeanMethods = false)
public class RecesoAcademicoWiringConfiguration {

    @Bean
    public CrearRecesoAcademicoInputPort crearRecesoAcademicoInputPort(
            final RecesoAcademicoRepositoryPort recesoAcademicoRepositoryPort,
            final NotificacionPort notificacionPort,
            final RealtimePublisherPort realtimePublisherPort
    ) {
        return new CrearRecesoAcademicoUseCaseImpl(recesoAcademicoRepositoryPort, notificacionPort, realtimePublisherPort);
    }

    @Bean
    public RetirarRecesoAcademicoInputPort retirarRecesoAcademicoInputPort(final RecesoAcademicoRepositoryPort recesoAcademicoRepositoryPort) {
        return new RetirarRecesoAcademicoUseCaseImpl(recesoAcademicoRepositoryPort);
    }

    @Bean
    public ConsultarRecesosPeriodoInputPort consultarRecesosPeriodoInputPort(final RecesoAcademicoRepositoryPort recesoAcademicoRepositoryPort) {
        return new ConsultarRecesosPeriodoUseCaseImpl(recesoAcademicoRepositoryPort);
    }
}
