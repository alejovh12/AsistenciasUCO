package co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.impl;

import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.CrearUsuarioUseCase;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.domain.CrearUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.crearusuario.usecase.entity.CrearUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.domain.ProvisionarUsuarioDomain;
import co.edu.uco.asistenciasuco.application.features.usuario.provisionarusuario.usecase.entity.ProvisionarUsuarioResultadoEntity;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.IdentityProviderPort;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Demuestra que Application ya no consume mensajes del proveedor de identidad y que el rol
 * institucional cruza {@link IdentityProviderPort} tipado, nunca como texto libre.
 */
class ProvisionarUsuarioUseCaseImplTest {

    private static final UUID TIPO_IDENTIFICACION = UUID.fromString("13641bab-e3cd-485c-b275-47e7b731e18c");
    private static final UUID USUARIO_ID = UUID.fromString("23641bab-e3cd-485c-b275-47e7b731e18c");

    @Test
    void execute_envia_rol_estudiante_tipado_e_ignora_el_mensaje_del_idp() {
        final AtomicReference<InstitutionalRole> rolEnviado = new AtomicReference<>();
        final ProvisionarUsuarioUseCaseImpl useCase = new ProvisionarUsuarioUseCaseImpl(
                crearUsuarioUseCase("Usuario creado exitosamente."),
                identityProviderPort(dto -> {
                    rolEnviado.set(dto.rolInstitucional());
                    return new CuentaIdentidadDTO("kc-externo-1", true);
                })
        );

        final ProvisionarUsuarioResultadoEntity resultado = useCase.execute(domainValido());

        assertEquals(InstitutionalRole.ESTUDIANTE, rolEnviado.get());
        assertEquals("Usuario creado exitosamente.", resultado.getMensajeUsuario());
        assertTrue(resultado.isExitoso());
    }

    @Test
    void execute_no_propaga_texto_alguno_del_resultado_de_identity_aunque_sea_distinto_del_mensaje_db() {
        final ProvisionarUsuarioUseCaseImpl useCase = new ProvisionarUsuarioUseCaseImpl(
                crearUsuarioUseCase("Mensaje funcional de la base de datos."),
                identityProviderPort(dto -> new CuentaIdentidadDTO("kc-externo-2", false))
        );

        final ProvisionarUsuarioResultadoEntity resultado = useCase.execute(domainValido());

        assertEquals("Mensaje funcional de la base de datos.", resultado.getMensajeUsuario());
        assertFalse(resultado.getMensajeUsuario().toLowerCase().contains("idp"));
        assertFalse(resultado.getMensajeUsuario().toLowerCase().contains("keycloak"));
    }

    private ProvisionarUsuarioDomain domainValido() {
        return ProvisionarUsuarioDomain.crear(
                TIPO_IDENTIFICACION, 123456789, "Perez", "Gomez", "Ana", "Maria",
                "ana.perez@uco.edu.co", "Clave123!"
        );
    }

    private CrearUsuarioUseCase crearUsuarioUseCase(final String mensajeUsuario) {
        return (final CrearUsuarioDomain domain) -> new CrearUsuarioResultadoEntity(USUARIO_ID, true, mensajeUsuario);
    }

    private IdentityProviderPort identityProviderPort(final CrearCuentaExecutor executor) {
        return new IdentityProviderPort() {
            @Override
            public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
                return executor.crearCuenta(dto);
            }

            @Override
            public void eliminarCuenta(final String idExterno) {
                throw new UnsupportedOperationException("No usado por este test.");
            }

            @Override
            public void asignarRol(final String idExterno, final InstitutionalRole rol) {
                throw new UnsupportedOperationException("No usado por este test.");
            }
        };
    }

    @FunctionalInterface
    private interface CrearCuentaExecutor {
        CuentaIdentidadDTO crearCuenta(CrearCuentaIdentidadDTO dto);
    }
}
