package co.edu.uco.asistenciasuco.infrastructure.observability.audit;

public record RequestActor(String actorId, AuditActorType actorType) {
}
