package co.edu.uco.asistenciasuco.infrastructure.config.resilience;

/**
 * REA-02: nombres de las instancias de circuit breaker de Resilience4j. Deben coincidir con
 * {@code resilience4j.circuitbreaker.instances.<nombre>} en {@code application.yml}; un nombre sin configuracion toma
 * la configuracion {@code default}.
 */
public final class ResilienceCircuitBreakerNames {

    /** PEP embebido de Security Baseline (decision de acceso por peticion; fail-closed 503). */
    public static final String SECURITY_BASELINE_PEP = "securityBaselinePep";
    /** Almacenamiento de soportes (MinIO). */
    public static final String FILE_STORAGE = "fileStorage";
    /** Analisis antivirus de soportes (ClamAV). */
    public static final String MALWARE_SCAN = "malwareScan";

    private ResilienceCircuitBreakerNames() {
    }
}
