package co.edu.uco.asistenciasuco.application.features.coordinador.common.mapper;

import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteCandidatoDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.domain.DocenteProgramaDomain;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteCandidatoDTO;
import co.edu.uco.asistenciasuco.application.features.coordinador.common.dto.DocenteProgramaDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteCandidatoProjection;
import co.edu.uco.asistenciasuco.application.secondaryports.academic.projection.DocenteProgramaProjection;

/** Conversiones proyeccion -> dominio -> DTO de la vertical de docentes del programa. */
public final class DocenteProgramaMapper {

    private DocenteProgramaMapper() {
        throw new IllegalStateException("Clase utilitaria.");
    }

    public static DocenteProgramaDomain toDomain(final DocenteProgramaProjection p) {
        return new DocenteProgramaDomain(p.idDocentePrograma(), p.idDocente(), p.idUsuario(), p.codigoTipoIdentificacion(),
                p.numeroIdentificacion(), p.primerNombre(), p.segundoNombre(), p.primerApellido(), p.segundoApellido(),
                p.nombreCompleto(), p.correo(), p.idTipoVinculacion(), p.codigoTipoVinculacion(), p.nombreTipoVinculacion(),
                p.fechaVinculacionUtc(), p.totalGruposPrograma(), p.estaActivo());
    }

    public static DocenteProgramaDTO toDTO(final DocenteProgramaDomain d) {
        return new DocenteProgramaDTO(d.idDocentePrograma(), d.idDocente(), d.idUsuario(), d.codigoTipoIdentificacion(),
                d.numeroIdentificacion(), d.primerNombre(), d.segundoNombre(), d.primerApellido(), d.segundoApellido(),
                d.nombreCompleto(), d.correo(), d.idTipoVinculacion(), d.codigoTipoVinculacion(), d.nombreTipoVinculacion(),
                d.fechaVinculacionUtc(), d.totalGruposPrograma(), d.estaActivo());
    }

    public static DocenteCandidatoDomain toDomain(final DocenteCandidatoProjection p) {
        return new DocenteCandidatoDomain(p.idDocente(), p.idUsuario(), p.codigoTipoIdentificacion(),
                p.numeroIdentificacion(), p.nombreCompleto());
    }

    public static DocenteCandidatoDTO toDTO(final DocenteCandidatoDomain d) {
        return new DocenteCandidatoDTO(d.idDocente(), d.idUsuario(), d.codigoTipoIdentificacion(),
                d.numeroIdentificacion(), d.nombreCompleto());
    }
}
