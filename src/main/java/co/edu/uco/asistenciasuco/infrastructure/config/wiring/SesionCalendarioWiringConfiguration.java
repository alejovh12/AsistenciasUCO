package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.ConsultarCalendarioDocenteInputPort;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.primaryports.ConsultarCalendarioEstudianteInputPort;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl.ConsultarCalendarioDocenteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.sesioncalendario.consultarcalendario.usecase.impl.ConsultarCalendarioEstudianteUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.repository.SesionCalendarioRepositoryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Composition Root del calendario de sesiones (ATA D7): horario semanal con fechas del estudiante y del docente. */
@Configuration(proxyBeanMethods = false)
public class SesionCalendarioWiringConfiguration {

    @Bean
    public ConsultarCalendarioEstudianteInputPort consultarCalendarioEstudianteInputPort(final SesionCalendarioRepositoryPort repositorio) {
        return new ConsultarCalendarioEstudianteUseCaseImpl(repositorio);
    }

    @Bean
    public ConsultarCalendarioDocenteInputPort consultarCalendarioDocenteInputPort(final SesionCalendarioRepositoryPort repositorio) {
        return new ConsultarCalendarioDocenteUseCaseImpl(repositorio);
    }
}
