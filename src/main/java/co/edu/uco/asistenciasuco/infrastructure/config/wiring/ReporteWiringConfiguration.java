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
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;
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
            final ObjectProvider<ParameterCatalogPort> parameterCatalogPort,
            @Value("${app.reporte.asistencia-excel.max-filas:" + PoiReporteExcelAdapter.TOPE_FILAS_POR_DEFECTO + "}") final int maxFilas) {
        return new PoiReporteExcelAdapter(messageCatalogPort.getIfAvailable(), maxFilas, zonaInstitucional(parameterCatalogPort.getIfAvailable()));
    }

    @Bean GenerarReporteAsistenciaExcelInputPort generarReporteAsistenciaExcelInputPort(
            final GenerarReporteAsistenciaUseCase useCase, final ReporteExcelPort reporteExcelPort) {
        return new GenerarReporteAsistenciaExcelInteractor(useCase, reporteExcelPort);
    }

    /** Zona IANA del parametro TIEMPO/ZONA_HORARIA_IANA; si falta o es invalida se usa la institucional por defecto (America/Bogota). */
    static java.time.ZoneId zonaInstitucional(final ParameterCatalogPort parametros) {
        if (parametros == null) {
            return PoiReporteExcelAdapter.ZONA_INSTITUCIONAL_POR_DEFECTO;
        }
        try {
            return parametros.getParameter("TIEMPO", "ZONA_HORARIA_IANA").filter(valor -> !valor.isBlank())
                    .map(valor -> java.time.ZoneId.of(valor.trim())).orElse(PoiReporteExcelAdapter.ZONA_INSTITUCIONAL_POR_DEFECTO);
        } catch (RuntimeException exception) {
            return PoiReporteExcelAdapter.ZONA_INSTITUCIONAL_POR_DEFECTO;
        }
    }
}
