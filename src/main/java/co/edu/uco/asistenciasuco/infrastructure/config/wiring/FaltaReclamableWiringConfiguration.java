package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.primaryports.ConsultarFaltasReclamablesInputPort;
import co.edu.uco.asistenciasuco.application.features.faltareclamable.consultarfaltasreclamables.usecase.impl.ConsultarFaltasReclamablesUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.FaltaReclamableRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root de las faltas por reclamar del estudiante (ATA D10). */
@Configuration(proxyBeanMethods = false)
public class FaltaReclamableWiringConfiguration {

    @Bean
    public ConsultarFaltasReclamablesInputPort consultarFaltasReclamablesInputPort(final FaltaReclamableRepositoryPort repositorio) {
        return new ConsultarFaltasReclamablesUseCaseImpl(repositorio);
    }
}
