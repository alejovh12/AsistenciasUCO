package co.edu.uco.asistenciasuco.application.features.usuario.dominiocorreo;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.MessageCatalogPort;
import co.edu.uco.asistenciasuco.application.secondaryports.catalog.ParameterCatalogPort;

import java.util.Locale;
import java.util.Objects;

/**
 * IDENT-001: política del correo institucional del estudiante. El dominio (incluye el {@code @}) es el parámetro
 * {@code REGISTRO/DOMINIO_CORREO_ESTUDIANTE}; no hay dominio escrito en el código. Aplica la misma regla que
 * {@code dbo.ufn_correo_cumple_dominio_institucional}: exactamente un {@code @}, parte local no vacía y dominio
 * <b>igual</b> al configurado, sin distinguir mayúsculas ni espacios laterales. Fail-closed: un dominio vacío o mal
 * formado rechaza todo correo. El SP sigue siendo la autoridad; esta política evita el viaje a la base de datos.
 */
public final class DominioCorreoInstitucionalPolicy {

    public static final String GRUPO_PARAMETRO = "REGISTRO";
    public static final String CLAVE_DOMINIO = "DOMINIO_CORREO_ESTUDIANTE";
    public static final String CODIGO_MENSAJE = "VCE_001";

    private final ParameterCatalogPort parametros;
    private final MessageCatalogPort mensajes;

    public DominioCorreoInstitucionalPolicy(final ParameterCatalogPort parametros, final MessageCatalogPort mensajes) {
        this.parametros = Objects.requireNonNull(parametros, "ParameterCatalogPort es obligatorio.");
        this.mensajes = Objects.requireNonNull(mensajes, "MessageCatalogPort es obligatorio.");
    }

    /** Dominio institucional vigente, tal como está configurado (por ejemplo {@code @uco.edu.co}). */
    public String dominio() {
        return parametros.getRequiredParameter(GRUPO_PARAMETRO, CLAVE_DOMINIO);
    }

    public boolean cumple(final String correo) {
        return cumple(correo, dominio());
    }

    /** Lanza {@link ValidationException} (VCE_001) con el mensaje del catálogo si el correo no es institucional. */
    public void exigir(final String correo) {
        final String dominio = dominio();
        if (!cumple(correo, dominio)) {
            throw new ValidationException(CODIGO_MENSAJE, mensajes.getUserMessage(CODIGO_MENSAJE, dominio));
        }
    }

    static boolean cumple(final String correo, final String dominioConfigurado) {
        if (correo == null || dominioConfigurado == null) {
            return false;
        }
        final String normalizado = correo.strip().toLowerCase(Locale.ROOT);
        final String dominio = dominioConfigurado.strip().toLowerCase(Locale.ROOT);
        if (dominio.length() < 2 || dominio.charAt(0) != '@' || dominio.indexOf('@', 1) >= 0) {
            return false;
        }
        final int arroba = normalizado.indexOf('@');
        if (arroba <= 0 || normalizado.indexOf('@', arroba + 1) >= 0) {
            return false;
        }
        return normalizado.substring(arroba).equals(dominio);
    }
}
