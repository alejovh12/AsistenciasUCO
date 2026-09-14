package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.ProvisionarUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.mapper.ProvisionarUsuarioIdentityMapper;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;

import java.util.Objects;

public final class ProvisionarUsuarioUseCaseImpl implements ProvisionarUsuarioUseCase {

    /**
     * DEUDA PENDIENTE (2B.2-D): este UseCase genérico de {@code /api/v1/usuarios} solo provisiona
     * identidad para el rol ESTUDIANTE — el rol está tipado (ya no es un {@code String} suelto)
     * pero sigue hardcodeado aquí; no se integra todavía con decano/coordinador/docente. Tipar
     * este valor en esta microfase NO resuelve esa deuda funcional, solo evita que el hardcode
     * exista como texto libre en la frontera de Identity.
     */
    private static final InstitutionalRole ROL_ESTUDIANTE = InstitutionalRole.ESTUDIANTE;

    private final CrearUsuarioUseCase crearUsuarioUseCase;
    private final IdentityProviderPort identityProviderPort;

    public ProvisionarUsuarioUseCaseImpl(
            final CrearUsuarioUseCase crearUsuarioUseCase,
            final IdentityProviderPort identityProviderPort
    ) {
        this.crearUsuarioUseCase = Objects.requireNonNull(crearUsuarioUseCase, "CrearUsuarioUseCase es obligatorio.");
        this.identityProviderPort = Objects.requireNonNull(identityProviderPort, "IdentityProviderPort es obligatorio.");
    }

    @Override
    public ProvisionarUsuarioResultadoEntity execute(final ProvisionarUsuarioDomain domain) {
        final CrearUsuarioResultadoEntity usuario = crearUsuarioUseCase.execute(
                ProvisionarUsuarioIdentityMapper.toCrearUsuarioDomain(domain)
        );
        identityProviderPort.crearCuenta(
                ProvisionarUsuarioIdentityMapper.toCrearCuentaIdentidadDTO(domain, usuario.getUsuarioId(), ROL_ESTUDIANTE)
        );
        return ProvisionarUsuarioIdentityMapper.toResultadoEntity(usuario);
    }
}
