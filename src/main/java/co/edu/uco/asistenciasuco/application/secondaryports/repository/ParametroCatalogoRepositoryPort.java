package co.edu.uco.asistenciasuco.application.secondaryports.repository;

import java.time.Instant;

/**
 * INT-004: copia en la BD ({@code dbo.CatalogoParametro}) el valor vigente de un parametro del Catalogo de Parametros externo.
 * Es el unico camino de escritura de esos valores ({@code usp_sincronizar_parametro_catalogo_externo}).
 */
public interface ParametroCatalogoRepositoryPort {

    /**
     * Sincroniza un parametro que ya existe en la BD (no crea filas).
     *
     * @param grupo       grupo del parametro.
     * @param clave       clave del parametro.
     * @param valor       valor nuevo; ignorado con {@link Operacion#DELETE}.
     * @param operacion   {@link Operacion#UPSERT} o {@link Operacion#DELETE} (vuelve al valor por defecto de la fila).
     * @param idEvento    identificador unico del evento (idempotencia); nulo en la reconciliacion.
     * @param ocurridoEn  instante del evento ("el ultimo evento gana"); en la reconciliacion, el de la lectura.
     * @return que paso en la BD. Un fallo tecnico (BD caida, SQL) se propaga como excepcion para reintentar.
     */
    ResultadoSincronizacion sincronizarParametroExterno(String grupo, String clave, String valor, Operacion operacion,
                                                        String idEvento, Instant ocurridoEn);

    /** Operacion sobre el valor. */
    enum Operacion {
        UPSERT,
        DELETE
    }

    /** Que paso con el parametro en la BD. */
    enum ResultadoSincronizacion {
        /** El valor cambio y se guardo. */
        APLICADO,
        /** El valor ya era el mismo: no se escribio. */
        SIN_CAMBIOS,
        /** El evento ya se habia procesado: no se escribio. */
        DUPLICADO,
        /** Habia un valor aplicado de un evento mas reciente: no se escribio. */
        OBSOLETO,
        /** El grupo y la clave no existen (o estan inactivos) en la BD: no se escribio. */
        DESCONOCIDO,
        /** El valor no cumple el tipo, rango o lista permitida del parametro: no se escribio. */
        RECHAZADO
    }
}
