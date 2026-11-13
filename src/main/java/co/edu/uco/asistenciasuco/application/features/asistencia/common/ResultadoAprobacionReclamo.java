package co.edu.uco.asistenciasuco.application.features.asistencia.common;

import java.util.Locale;
import java.util.Optional;

/**
 * PC (puntos de control): resultado de aprobar un reclamo de una toma. {@code PRESENTE} deja la toma como asistencia normal
 * (codigo de razon {@code AN}); {@code JUSTIFICADA} la deja como excusa ({@code EX}). Es la traduccion entre el contrato HTTP
 * y los codigos de la base de datos; la regla de negocio la valida el procedimiento almacenado.
 */
public enum ResultadoAprobacionReclamo {
    PRESENTE("AN"),
    JUSTIFICADA("EX");

    private final String codigo;

    ResultadoAprobacionReclamo(final String codigo) {
        this.codigo = codigo;
    }

    /** Codigo de razon que espera {@code @codigoResultadoAprobacion}. */
    public String codigo() {
        return codigo;
    }

    /** Nombre del contrato HTTP ({@code PRESENTE} o {@code JUSTIFICADA}, sin distinguir mayusculas); vacio si no es valido. */
    public static Optional<ResultadoAprobacionReclamo> desdeNombre(final String nombre) {
        if (nombre == null) {
            return Optional.empty();
        }
        final String normalizado = nombre.strip().toUpperCase(Locale.ROOT);
        for (final ResultadoAprobacionReclamo valor : values()) {
            if (valor.name().equals(normalizado)) {
                return Optional.of(valor);
            }
        }
        return Optional.empty();
    }

    /** Nombre del contrato HTTP de un codigo de razon de la base de datos ({@code AN}, {@code EX}); nulo si no corresponde. */
    public static String nombreDesdeCodigo(final String codigo) {
        if (codigo == null) {
            return null;
        }
        final String normalizado = codigo.strip().toUpperCase(Locale.ROOT);
        for (final ResultadoAprobacionReclamo valor : values()) {
            if (valor.codigo.equals(normalizado)) {
                return valor.name();
            }
        }
        return null;
    }
}
