package co.edu.uco.asistenciasuco.application.features.coordinador.common.rule;

import co.edu.uco.asistenciasuco.application.exception.business.ForbiddenException;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.security.InstitutionalScopePort;
import java.util.Set;
import java.util.UUID;

/** Reglas de forma compartidas por los casos de uso de KPIs del coordinador. El calculo no vive aqui: esta en la DB. */
public final class IndicadoresRules {

    public static final int PAGINA_POR_DEFECTO = 1;
    public static final int TAMANO_POR_DEFECTO = 10;
    public static final Set<String> SEMAFOROS = Set.of("VERDE", "AMBAR", "ROJO");

    private IndicadoresRules() {
    }

    public static UUID programaDelCoordinador(final InstitutionalScopePort scopePort, final UUID actor) {
        return scopePort.findProgramaIdByCoordinadorUsuario(actor)
                .orElseThrow(() -> new ForbiddenException("No tiene un programa academico a su cargo para consultar estos indicadores."));
    }

    public static int pagina(final Integer valor) {
        final int pagina = valor == null ? PAGINA_POR_DEFECTO : valor;
        if (pagina < 1) {
            throw new ValidationException("El numero de pagina debe ser mayor o igual a 1.");
        }
        return pagina;
    }

    public static int tamano(final Integer valor) {
        final int tamano = valor == null ? TAMANO_POR_DEFECTO : valor;
        if (tamano < 1) {
            throw new ValidationException("El tamano de pagina debe ser mayor o igual a 1.");
        }
        return tamano;
    }

    public static String textoOpcional(final String valor) {
        return valor == null || valor.isBlank() ? null : valor.trim();
    }

    public static UUID requerido(final UUID valor, final String mensaje) {
        if (valor == null) {
            throw new ValidationException(mensaje);
        }
        return valor;
    }
}
