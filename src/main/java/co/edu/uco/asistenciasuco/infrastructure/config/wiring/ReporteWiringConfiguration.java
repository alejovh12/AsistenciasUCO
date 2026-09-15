package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor.GenerarReporteAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.impl.GenerarReporteAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReporteWiringConfiguration {

    @Bean GenerarReporteAsistenciaUseCase generarReporteAsistenciaUseCase(final ReporteAsistenciaQueryPort queryPort) { return new GenerarReporteAsistenciaUseCaseImpl(queryPort); }
    @Bean GenerarReporteAsistenciaInputPort generarReporteAsistenciaInputPort(final GenerarReporteAsistenciaUseCase useCase) { return new GenerarReporteAsistenciaInteractor(useCase); }
}
