package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.common.AvisoRevisionCancelacionService;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.primaryports.ConsultarRevisionesCoordinadorInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionescoordinador.usecase.impl.ConsultarRevisionesCoordinadorUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.primaryports.ConsultarRevisionesDecanoInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdecano.usecase.impl.ConsultarRevisionesDecanoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.primaryports.ConsultarRevisionesDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.consultarrevisionesdocente.usecase.impl.ConsultarRevisionesDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.primaryports.DecidirRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.decidirrevision.usecase.impl.DecidirRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.primaryports.ResolverRevisionCancelacionInputPort;
import co.edu.uco.asistenciasuco.application.features.revisioncancelacion.resolverrevision.usecase.impl.ResolverRevisionCancelacionUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.RevisionCancelacionRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de la revision de cancelacion por inasistencia (CULM-001 C.1, HU026C). */
@Configuration(proxyBeanMethods = false)
public class RevisionCancelacionWiringConfiguration {

    @Bean
    public DecidirRevisionCancelacionInputPort decidirRevisionCancelacionInputPort(
            final RevisionCancelacionRepositoryPort repositorio, final AvisoRevisionCancelacionService avisos) {
        return new DecidirRevisionCancelacionUseCaseImpl(repositorio, avisos);
    }

    @Bean
    public ResolverRevisionCancelacionInputPort resolverRevisionCancelacionInputPort(
            final RevisionCancelacionRepositoryPort repositorio, final AvisoRevisionCancelacionService avisos) {
        return new ResolverRevisionCancelacionUseCaseImpl(repositorio, avisos);
    }

    @Bean
    public ConsultarRevisionesCoordinadorInputPort consultarRevisionesCoordinadorInputPort(final RevisionCancelacionRepositoryPort repositorio) {
        return new ConsultarRevisionesCoordinadorUseCaseImpl(repositorio);
    }

    @Bean
    public ConsultarRevisionesDecanoInputPort consultarRevisionesDecanoInputPort(final RevisionCancelacionRepositoryPort repositorio) {
        return new ConsultarRevisionesDecanoUseCaseImpl(repositorio);
    }

    @Bean
    public ConsultarRevisionesDocenteInputPort consultarRevisionesDocenteInputPort(final RevisionCancelacionRepositoryPort repositorio) {
        return new ConsultarRevisionesDocenteUseCaseImpl(repositorio);
    }
}
