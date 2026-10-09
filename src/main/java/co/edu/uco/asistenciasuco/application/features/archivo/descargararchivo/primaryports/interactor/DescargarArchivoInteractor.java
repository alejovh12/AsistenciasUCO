package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.DescargarArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.mapper.DescargarArchivoMapper;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.DescargarArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;

import java.util.Objects;

public final class DescargarArchivoInteractor implements DescargarArchivoInputPort {

    private final DescargarArchivoUseCase useCase;

    public DescargarArchivoInteractor(final DescargarArchivoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso DescargarArchivoUseCase es obligatorio.");
    }

    @Override
    public DescargarArchivoResultado execute(final DescargarArchivoDTO dto) {
        final DescargarArchivoResultadoEntity resultado = useCase.execute(DescargarArchivoMapper.toDomain(dto));
        return DescargarArchivoMapper.toResultadoDTO(resultado);
    }
}
