package co.edu.uco.asistenciasuco.application.secondaryports.identity.dto;

/**
 * Resultado neutral de la creación/verificación de una cuenta en el proveedor de identidad.
 * No transporta mensajes humanos ni estructuras propias del IdP: Application no debe depender
 * de la semántica de mensajes de un proveedor concreto para construir su propia respuesta
 * funcional.
 *
 * @param idExterno    ID único asignado por el IdP al usuario (nuevo o existente) — útil para
 *                     operaciones de compensación.
 * @param newlyCreated Indica si el IdP creó una cuenta nueva ({@code true}) o si la identidad
 *                     ya existía y fue verificada exactamente ({@code false}).
 */
public record CuentaIdentidadDTO(
        String idExterno,
        boolean newlyCreated
) {
}
