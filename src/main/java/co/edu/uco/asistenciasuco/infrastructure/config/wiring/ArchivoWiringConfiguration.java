package co.edu.uco.asistenciasuco.infrastructure.config.wiring;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.interactor.DescargarArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.DescargarArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.impl.DescargarArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.interactor.SubirArchivoInteractor;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.SubirArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.impl.SubirArchivoUseCaseImpl;
import co.edu.uco.asistenciasuco.application.secondaryports.malwarescan.MalwareScanPort;
import co.edu.uco.asistenciasuco.application.secondaryports.storage.FileStoragePort;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class ArchivoWiringConfiguration {

    @Bean
    public SubirArchivoUseCase subirArchivoUseCase(
            final FileStoragePort fileStoragePort,
            final MalwareScanPort malwareScanPort
    ) {
        return new SubirArchivoUseCaseImpl(fileStoragePort, malwareScanPort);
    }

    @Bean
    public SubirArchivoInputPort subirArchivoInputPort(final SubirArchivoUseCase subirArchivoUseCase) {
        return new SubirArchivoInteractor(subirArchivoUseCase);
    }

    @Bean
    public DescargarArchivoUseCase descargarArchivoUseCase(final FileStoragePort fileStoragePort) {
        return new DescargarArchivoUseCaseImpl(fileStoragePort);
    }

    @Bean
    public DescargarArchivoInputPort descargarArchivoInputPort(final DescargarArchivoUseCase descargarArchivoUseCase) {
        return new DescargarArchivoInteractor(descargarArchivoUseCase);
    }
}
