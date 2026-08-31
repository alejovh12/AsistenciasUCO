# Backend roadmap

Estas son evoluciones planificadas del producto, no deuda tecnica bloqueante actual.

1. Idempotencia de Commands HTTP.
2. CQRS ligero.
3. Redis cache-aside sobre Query Side.
4. Metricas operativas adicionales: audit persistence failures, validation failures e idempotency metrics.
5. OpenAPI / contract-first.
6. Testing tecnico avanzado: parameterized tests, property-based tests y mutation testing.
7. DevSecOps: empaquetado limpio que excluya `.env`, `.git`, `.idea`, `target`, logs y repositorio Maven local; SBOM; image/dependency scanning.
8. Seguridad institucional definitiva: revisar `SecurityErrorResponseWriter` para compartir contrato/serializacion comun con `ApiErrorResponse`, y restringir/exponer Actuator/Prometheus segun la topologia real de despliegue.
9. Estrategia transaccional para Commands multi-adapter.
10. Limpieza progresiva de `ObjectHelper` / `CrosscuttingException` solamente cuando se toque codigo relacionado.
