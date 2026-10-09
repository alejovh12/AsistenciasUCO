---
name: uco-files
description: Gobernar validación antivirus, MinIO, compresión, metadatos y descarga autorizada de soportes de reclamos LB-004.
---
# uco-files — archivos y reclamos

Leer AGENTS.md; docs/work-items/LB-004-stateless-serverless-readiness/{PROFESSOR_DECISION,PAUSE,CONTENT_SECURITY,METADATA_CONTRACT_TARGET,HTTP_CONTRACT_TARGET,OWNERSHIP_DECISION,MINIO_STORAGE_CONTRACT,STORAGE_LIFECYCLE,E2E_PLAN}.md; skills uco-seguridad, uco-persistencia, uco-testing cuando aplique.

## Invariantes
- Backend único actor que contacta MinIO; identidad técnica de mínimo privilegio, bucket privado, sin acceso frontend directo ni presigned URLs en baseline.
- MinIO conserva bytes; SQL Server dueño del metadata/binding; no persistir bytes en SQL ni filesystem local contractual.
- fileId UUID opaco identifica archivo en HTTP/Application; objectKey/credenciales sólo Infrastructure. Nunca confiar filename, MIME, URL del cliente como identidad/autoridad.
- PDF/PNG/JPEG con extensión+MIME+magic comprobados, no vacío, 5MiB exactos; ClamAV CLEAN antes del put. INFECTED rechazo; timeout/error/invalid/EOF FAIL CLOSED.
- Comprimir sólo ahorro >=10% y tamaño estrictamente menor; descarga devuelve bytes originales con checksum verificado; lectura e inflate acotados.
- Bearer y ownership real antes de retornar stream con Content-Type verificado. Docente relacionado DENY_BY_DEFAULT hasta binding DB liberado. Docente ajeno no infiere existencia.
- delete interno para compensación/purga; política de retención/cold storage pendiente de decisión. No borrar solo por edad local.
- Contract DB es propiedad del repositorio DB. REVIEW_BINDING BLOCKED_BY_DB_CONTRACT. No implementar attach/docente ni SQL desde QUALITY Q0.
- TESTS: unit de ClamAV/MinIO, límites, checksum, compresión, auth; IT con proveedores reales y EICAR sintético; E2E posterior estudiante→reclamo→docente con fixture SQL. Mock controller NO equivale E2E.

Resultado con DoR/RED/Green, fixture, evidencia provider, cleanup, skips, bloqueos y autorización institucional. No alterar contrato OpenAPI unilateralmente.
