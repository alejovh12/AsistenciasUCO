package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor.GenerarReporteAsistenciaInteractor;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.GenerarReporteAsistenciaUseCase;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.usecase.impl.GenerarReporteAsistenciaUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteAsistenciaQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.GenerarReporteAsistenciaExcelInputPort;
import co.edu.uco.asistenciasuco.application.features.reporte.generarreporteasistencia.primaryports.interactor.GenerarReporteAsistenciaExcelInteractor;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.report.ReporteExcelPort;
import co.edu.uco.asistenciasuco.infrastructure.adapter.secondary.report.PoiReporteExcelAdapter;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ReporteWiringConfiguration {

    @Bean GenerarReporteAsistenciaUseCase generarReporteAsistenciaUseCase(final ReporteAsistenciaQueryPort queryPort, final InstitutionalScopePort institutionalScopePort) { return new GenerarReporteAsistenciaUseCaseImpl(queryPort, institutionalScopePort); }
    @Bean GenerarReporteAsistenciaInputPort generarReporteAsistenciaInputPort(final GenerarReporteAsistenciaUseCase useCase) { return new GenerarReporteAsistenciaInteractor(useCase); }

    /** M-03: la generacion del .xlsx vive en un adaptador; el tope de filas se ajusta con {@code app.reporte.asistencia-excel.max-filas}. */
    @Bean ReporteExcelPort reporteExcelPort(
            final ObjectProvider<MessageCatalogPort> messageCatalogPort,
            @Value("${app.reporte.asistencia-excel.max-filas:" + PoiReporteExcelAdapter.TOPE_FILAS_POR_DEFECTO + "}") final int maxFilas) {
        return new PoiReporteExcelAdapter(messageCatalogPort.getIfAvailable(), maxFilas);
    }

    @Bean GenerarReporteAsistenciaExcelInputPort generarReporteAsistenciaExcelInputPort(
            final GenerarReporteAsistenciaUseCase useCase, final ReporteExcelPort reporteExcelPort) {
        return new GenerarReporteAsistenciaExcelInteractor(useCase, reporteExcelPort);
    }
}
