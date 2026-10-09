package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.SubirArchivoInputPort;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.mapper.SubirArchivoMapper;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.SubirArchivoUseCase;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;

import java.util.Objects;

public final class SubirArchivoInteractor implements SubirArchivoInputPort {

    private final SubirArchivoUseCase useCase;

    public SubirArchivoInteractor(final SubirArchivoUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso SubirArchivoUseCase es obligatorio.");
    }

    @Override
    public SubirArchivoResultado execute(final SubirArchivoDTO dto) {
        final SubirArchivoResultadoEntity resultado = useCase.execute(SubirArchivoMapper.toDomain(dto));
        return SubirArchivoMapper.toResultadoDTO(resultado);
    }
}
