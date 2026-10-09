package co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.mapper;

import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoDTO;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.primaryports.dto.DescargarArchivoResultado;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.domain.DescargarArchivoDomain;
import co.edu.uco.asistenciasuco.application.features.archivo.descargararchivo.usecase.entity.DescargarArchivoResultadoEntity;

public final class DescargarArchivoMapper {

    private DescargarArchivoMapper() {
    }

    public static DescargarArchivoDomain toDomain(final DescargarArchivoDTO dto) {
        return new DescargarArchivoDomain(dto.fileId(), dto.requesterSubject());
    }

    public static DescargarArchivoResultado toResultadoDTO(final DescargarArchivoResultadoEntity entity) {
        return new DescargarArchivoResultado(entity.content(), entity.contentType(), entity.filename(), entity.size());
    }
}
