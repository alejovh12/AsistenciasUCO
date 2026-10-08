package co.edu.uco.asistenciasuco.application.features.archivo.subirarchivo.usecase.entity;

import java.util.UUID;

/**
 * Resultado interno del caso de uso de subida; el Interactor lo mapea al DTO del InputPort
 * (nunca al reves: usecase no conoce primaryports.dto, ver CleanArchitectureRulesTest).
 */
public record SubirArchivoResultadoEntity(UUID fileId, String nombre, String url, long tamanio) {
}
