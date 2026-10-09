package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.primaryports.dto.SubirArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.domain.SubirArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity.SubirArchivoResultadoEntity;

public final class SubirArchivoMapper {

    private SubirArchivoMapper() {
    }

    public static SubirArchivoDomain toDomain(final SubirArchivoDTO dto) {
        return new SubirArchivoDomain(
                dto.ownerSubject(), dto.originalFilename(), dto.declaredContentType(), dto.content());
    }

    public static SubirArchivoResultado toResultadoDTO(final SubirArchivoResultadoEntity entity) {
        return new SubirArchivoResultado(entity.fileId(), entity.nombre(), entity.url(), entity.tamanio());
    }
}
