package co.edu.uco.asistenciasuco.application.secondaryports.repository;

/**
 * INT-003: copia en la BD ({@code dbo.CatalogoMensajeUsuario}) el texto vigente de un mensaje de usuario para que los
 * procedimientos almacenados y el proveedor {@code sqlserver} usen el mismo texto que el catalogo externo.
 */
public interface MensajeCatalogoRepositoryPort {

    /**
     * {@code usp_sincronizar_mensaje_usuario}: actualiza solo el texto de un codigo que ya existe en la BD.
     *
     * @return {@link ResultadoSincronizacion#NO_EXISTE} si el codigo no pertenece al catalogo de la BD (no se crea).
     */
    ResultadoSincronizacion sincronizarMensajeUsuario(String codigo, String contenido);

    /** Que paso con el mensaje en la BD. */
    enum ResultadoSincronizacion {
        /** El texto cambio y se guardo. */
        ACTUALIZADO,
        /** El texto ya era el mismo: no se escribio. */
        SIN_CAMBIOS,
        /** El codigo no existe en la BD: no se escribio. */
        NO_EXISTE
    }
}
