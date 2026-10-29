package co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.impl;

import co.edu.uco.asistenciasuco.application.exception.business.ResourceNotFoundException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.ConsultarPerfilUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain.PerfilUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.consultarperfil.usecase.domain.VinculoUsuarioDomain;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.PerfilUsuarioQueryPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.projection.PerfilUsuarioProjection;

import java.util.Objects;
import java.util.UUID;

/**
 * HU006: perfil del usuario autenticado leido de la DB (datos personales y vinculos institucionales). Solo el propio;
 * un usuario institucional inexistente responde 404.
 */
public final class ConsultarPerfilUseCaseImpl implements ConsultarPerfilUseCase {

    private final PerfilUsuarioQueryPort port;

    public ConsultarPerfilUseCaseImpl(final PerfilUsuarioQueryPort port) {
        this.port = Objects.requireNonNull(port, "PerfilUsuarioQueryPort es obligatorio.");
    }

    @Override
    public PerfilUsuarioDomain execute(final UUID actorUsuarioId) {
        if (actorUsuarioId == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        final PerfilUsuarioProjection p = port.consultarPerfil(actorUsuarioId)
                .orElseThrow(() -> new ResourceNotFoundException("No se encontro el perfil del usuario autenticado."));
        return new PerfilUsuarioDomain(p.idUsuario(), p.codigoTipoIdentificacion(), p.nombreTipoIdentificacion(),
                p.numeroIdentificacion(), p.primerNombre(), p.segundoNombre(), p.primerApellido(), p.segundoApellido(),
                p.nombreCompleto(), p.correo(), p.correoConfirmado(), p.estaActivo(),
                p.vinculos().stream()
                        .map(v -> new VinculoUsuarioDomain(v.rol(), v.idEntidad(), v.nombreEntidad(), v.detalle(), v.estaActivo()))
                        .toList());
    }
}
