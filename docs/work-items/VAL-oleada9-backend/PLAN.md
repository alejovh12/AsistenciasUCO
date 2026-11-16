---
status: active
type: work-item
scope: backend
owner: backend-team
last-reviewed: 2026-11-15
---

# VAL-oleada9-backend - PLAN

Fecha: 2026-11-15. Rama `fix/VAL-oleada9-backend` desde `repo-antiguo/develop` 8530bd54 (con la oleada 8 ya unida, PR #158).
BD: `fix/VAL-oleada9-mensaje-almacenamiento` en `johnjduque/gestion-asistencia-db`. No se cruza con `fix/VAL-oleada7-backend`
(PEP, cachés y réplicas), que sigue sin PR.

| Ítem | Hallazgo | Cambio | Archivos |
| :--- | :--- | :--- | :--- |
| REA-02 | Con MinIO caído, subir un soporte respondía 500 `INTERNAL_ERROR` (medido en el laboratorio) | `SubirArchivoUseCaseImpl` traduce `StorageUnavailableException` al almacenar en 503 `ERR_ALMACENAMIENTO_NO_DISPONIBLE` (como el antivirus); la descarga queda en 500 por decisión pendiente; código nuevo en `ArchivoErrorCode`; mensaje en la BD (migración 47), fixture del catálogo regenerado y alias `ERR121` | `SubirArchivoUseCaseImpl`, `ArchivoErrorCode`, `application.yml`, `catalogo-mensajes-usuario.psv`, `ArchivoAlmacenamientoNoDisponibleTest`, `MessageUcoLabCatalogoTest` |
| REA-02 | Faltaba la matriz de comportamiento por dependencia caída | `docs/architecture/degradacion-por-dependencia.md`: PEP/PDP, ClamAV, MinIO, SMTP, emisor, catálogos y SQL Server, con su evidencia | doc nuevo |
| H3 | Corregido en la oleada 3, faltaba medirlo en vivo | Medido: ClamAV detenido → 503 `ERR_ANTIVIRUS_NO_DISPONIBLE` | VALIDATION |
| ROL-12 / REA-02 | `e2e_baseline.py --pep-down` detenía un contenedor con el nombre viejo | `E2E_PEP_CONTAINER` (por defecto el nombre de la instalación única) | `e2e_baseline.py`, `test_e2e_baseline_usuarios.py` |
| MSG-05 | No se habían recontado los textos quemados de Application | Recuento: 0 excepciones de negocio con texto sin código (la prueba del catálogo ya lo exige; deuda vacía) | VALIDATION |
| PRU-09 | Sin matriz HU → pruebas en el backend | `scripts/testing/matriz_hu_pruebas.py` genera `docs/testing/MATRIZ_HU_PRUEBAS_BACKEND.md` y la compuerta del CI falla si queda desactualizada | script, matriz, `backend-ci.yml` |

## Fuera de alcance

- SQL Server caído responde 500: decidir si debe ser 503 (ver la matriz). No se detuvo SQL Server del laboratorio porque lo
  comparten varias personas.
- La medición en vivo de la caída del PEP: el laboratorio está en modo `keycloak` desde las 04:13 UTC.
- Retirar el código genérico `VALIDATION_ERROR` (MSG-05): es el contrato del 400 con `details[]`; requiere decisión de contrato.
