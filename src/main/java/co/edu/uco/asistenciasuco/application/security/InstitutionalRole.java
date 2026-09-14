package co.edu.uco.asistenciasuco.application.security;

/**
 * Roles institucionales reconocidos por el dominio. No conoce tecnologías, protocolos ni
 * convenciones de nombres de infraestructura: cualquier mapping técnico vive fuera de
 * Application.
 */
public enum InstitutionalRole {
    ADMINISTRADOR,
    DECANO,
    COORDINADOR,
    DOCENTE,
    ESTUDIANTE
}
