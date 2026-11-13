package co.edu.uco.asistenciasuco.application.features.decano.common.rule;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.crosscutting.util.ObjectHelper;

/**
 * Reglas de entrada compartidas por las consultas paginadas del decano y del administrador (HU104/HU105/HU145 y
 * HU112/HU128/HU143/HU157): pagina y tamano por defecto, sus rangos y la busqueda opcional. El SP vuelve a validar la
 * paginacion (PAG_001); aqui se rechaza antes con un 400 claro.
 */
public final class ConsultaPaginadaRules {

    public static final int PAGINA_POR_DEFECTO = 1;
    public static final int TAMANO_POR_DEFECTO = 10;
    public static final int TAMANO_MAXIMO = 100;
    public static final int BUSQUEDA_MAXIMA = 100;

    private ConsultaPaginadaRules() {
    }

    public static int pagina(final Integer pagina) {
        if (ObjectHelper.isNull(pagina)) {
            return PAGINA_POR_DEFECTO;
        }
        if (pagina < 1) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_038);
        }
        return pagina;
    }

    public static int tamano(final Integer tamano) {
        if (ObjectHelper.isNull(tamano)) {
            return TAMANO_POR_DEFECTO;
        }
        if (tamano < 1 || tamano > TAMANO_MAXIMO) {
            throw ValidationException.conParametros(MensajeUsuarioErrorCode.VAL_APP_039, TAMANO_MAXIMO);
        }
        return tamano;
    }

    /** Busqueda opcional: {@code null} si viene vacia; 400 si excede {@link #BUSQUEDA_MAXIMA}. */
    public static String busqueda(final String busqueda) {
        final String filtro = ObjectHelper.isNull(busqueda) || busqueda.isBlank() ? null : busqueda.trim();
        if (!ObjectHelper.isNull(filtro) && filtro.length() > BUSQUEDA_MAXIMA) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_044);
        }
        return filtro;
    }
}
