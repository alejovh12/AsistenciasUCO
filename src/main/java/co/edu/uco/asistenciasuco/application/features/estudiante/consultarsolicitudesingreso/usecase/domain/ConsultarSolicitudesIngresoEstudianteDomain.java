package co.edu.uco.asistenciasuco.application.features.estudiante.consultarsolicitudesingreso.usecase.domain;

import co.edu.uco.asistenciasuco.application.exception.validation.ValidationException;

import java.util.Locale;
import java.util.Map;
import java.util.UUID;

/**
 * Criterios normalizados: estado opcional por codigo (P, A, R, E, X) o nombre (PENDIENTE, ACEPTADA, RECHAZADA,
 * EN_ESPERA, RETIRADA); pagina minima 1 y tamano 1..100 (se acotan); un estado desconocido es 400.
 */
public record ConsultarSolicitudesIngresoEstudianteDomain(UUID actorUsuarioId, String codigoEstado, int pagina, int tamano) {

    public static final int TAMANO_PREDETERMINADO = 10;
    public static final int TAMANO_MAXIMO = 100;

    private static final Map<String, String> CODIGO_POR_ESTADO = Map.ofEntries(
            Map.entry("P", "P"), Map.entry("PENDIENTE", "P"),
            Map.entry("A", "A"), Map.entry("ACEPTADA", "A"),
            Map.entry("R", "R"), Map.entry("RECHAZADA", "R"),
            Map.entry("E", "E"), Map.entry("EN_ESPERA", "E"),
            Map.entry("X", "X"), Map.entry("RETIRADA", "X"));

    public static ConsultarSolicitudesIngresoEstudianteDomain of(
            final UUID actorUsuarioId,
            final String estado,
            final Integer pagina,
            final Integer tamano
    ) {
        if (actorUsuarioId == null) {
            throw new ValidationException("El usuario autenticado es obligatorio.");
        }
        final String normalizado = estado == null || estado.isBlank() ? null : estado.strip().toUpperCase(Locale.ROOT);
        final String codigo = normalizado == null ? null : CODIGO_POR_ESTADO.get(normalizado);
        if (normalizado != null && codigo == null) {
            throw new ValidationException("El estado de la solicitud no es valido.");
        }
        final int paginaSegura = pagina == null || pagina < 1 ? 1 : pagina;
        final int tamanoSeguro = tamano == null || tamano < 1 ? TAMANO_PREDETERMINADO : Math.min(tamano, TAMANO_MAXIMO);
        return new ConsultarSolicitudesIngresoEstudianteDomain(actorUsuarioId, codigo, paginaSegura, tamanoSeguro);
    }
}
