package co.edu.uco.notificaciones.application.model;

import java.time.Instant;

public record FalloReciente(Instant en, String codigo) {
}
