package co.edu.uco.asistenciasuco.application.secondaryports.identity;

import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.ActualizarCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CrearCuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.secondaryports.identity.dto.CuentaIdentidadDTO;
import co.edu.uco.asistenciasuco.application.security.InstitutionalRole;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * IAM-001 §1: las operaciones de ciclo de vida son capacidades opcionales del proveedor. Un proveedor que solo
 * sabe crear cuentas (p. ej. un componente de seguridad de otro equipo) no rompe la compilacion ni el arranque:
 * invocar una capacidad que no implementa falla de forma controlada con {@code IdentityProviderException}, que la
 * sincronizacion trata como desfase pendiente.
 */
class IdentityProviderPortCapacidadesTest {

    private final IdentityProviderPort soloCrea = new IdentityProviderPort() {
        @Override
        public CuentaIdentidadDTO crearCuenta(final CrearCuentaIdentidadDTO dto) {
            return new CuentaIdentidadDTO("x", true);
        }

        @Override
        public void eliminarCuenta(final String idExterno) {
            // no-op
        }

        @Override
        public void asignarRol(final String idExterno, final InstitutionalRole rol) {
            // no-op
        }
    };

    @Test
    void buscar_cuenta_no_soportada_falla_de_forma_controlada() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> soloCrea.buscarCuentaPorIdUsuario(UUID.randomUUID()));
    }

    @Test
    void deshabilitar_y_habilitar_no_soportados_fallan_de_forma_controlada() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> soloCrea.deshabilitarCuenta("kc-1"));
        assertThrows(IdentityProviderPort.IdentityProviderException.class, () -> soloCrea.habilitarCuenta("kc-1"));
    }

    @Test
    void actualizar_datos_y_retirar_rol_no_soportados_fallan_de_forma_controlada() {
        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> soloCrea.actualizarDatosCuenta("kc-1", new ActualizarCuentaIdentidadDTO("a@uco.edu.co", "Ana", "Perez")));
        assertThrows(IdentityProviderPort.IdentityProviderException.class,
                () -> soloCrea.retirarRol("kc-1", InstitutionalRole.COORDINADOR));
    }
}
