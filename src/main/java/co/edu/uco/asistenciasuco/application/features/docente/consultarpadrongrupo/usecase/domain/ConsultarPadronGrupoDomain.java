package co.edu.uco.asistenciasuco.application.features.docente.consultarpadrongrupo.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.catalog.MensajeUsuarioErrorCode;
import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.UUID;

/**
 * Criterios normalizados: pagina minima 1, tamano entre 1 y 100 (por defecto 10; fuera de rango se acota) y busqueda
 * recortada (nula si queda vacia). Grupo y ejecutor son obligatorios.
 */
public record ConsultarPadronGrupoDomain(UUID grupoId, UUID usuarioEjecutor, int pagina, int tamano, String busqueda) {

    public static final int TAMANO_PREDETERMINADO = 10;
    public static final int TAMANO_MAXIMO = 100;

    public static ConsultarPadronGrupoDomain of(
            final UUID grupoId,
            final UUID usuarioEjecutor,
            final Integer pagina,
            final Integer tamano,
            final String busqueda
    ) {
        if (usuarioEjecutor == null || grupoId == null) {
            throw new ValidationException(MensajeUsuarioErrorCode.VAL_APP_008);
        }
        final int paginaSegura = pagina == null || pagina < 1 ? 1 : pagina;
        final int tamanoSeguro = tamano == null || tamano < 1 ? TAMANO_PREDETERMINADO : Math.min(tamano, TAMANO_MAXIMO);
        final String texto = busqueda == null || busqueda.isBlank() ? null : busqueda.trim();
        return new ConsultarPadronGrupoDomain(grupoId, usuarioEjecutor, paginaSegura, tamanoSeguro, texto);
    }
}
