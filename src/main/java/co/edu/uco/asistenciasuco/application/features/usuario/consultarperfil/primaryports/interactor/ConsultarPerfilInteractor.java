package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.interactor;

import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.ConsultarPerfilInputPort;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.PerfilUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.primaryports.dto.VinculoUsuarioDTO;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.ConsultarPerfilUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain.PerfilUsuarioDomain;

import java.util.Objects;
import java.util.UUID;

public class ConsultarPerfilInteractor implements ConsultarPerfilInputPort {

    private final ConsultarPerfilUseCase useCase;

    public ConsultarPerfilInteractor(final ConsultarPerfilUseCase useCase) {
        this.useCase = Objects.requireNonNull(useCase, "El caso de uso es obligatorio.");
    }

    @Override
    public PerfilUsuarioDTO execute(final UUID actorUsuarioId) {
        final PerfilUsuarioDomain d = useCase.execute(actorUsuarioId);
        return new PerfilUsuarioDTO(d.idUsuario(), d.codigoTipoIdentificacion(), d.nombreTipoIdentificacion(), d.numeroIdentificacion(),
                d.primerNombre(), d.segundoNombre(), d.primerApellido(), d.segundoApellido(), d.nombreCompleto(), d.correo(),
                d.correoConfirmado(), d.estaActivo(),
                d.vinculos().stream()
                        .map(v -> new VinculoUsuarioDTO(v.rol(), v.idEntidad(), v.nombreEntidad(), v.detalle(), v.estaActivo()))
                        .toList());
    }
}
