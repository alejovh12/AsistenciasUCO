---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU094-HU095-directorio-docentes-coordinador

## Estado

**DONE local:** los gates locales están en verde ([VALIDATION.md](VALIDATION.md)). El trabajo está en commits locales en la rama `feature/HU094-HU095-directorio-docentes-coordinador` de DB, backend y frontend, sin subir hasta que John lo confirme.

Pendiente:
- push y PR hacia `develop`, en orden DB → backend → frontend;
- CI remoto y SonarCloud;
- reaprovisionar el PDP con 2 rutas;
- MV-008.

## Entregado

- **HU094:** `GET /api/v1/coordinador/docentes/{idDocente}/ficha`, con la cabecera del vínculo, el resumen de carga y el historial paginado de grupos en el programa (VIGENTE/FINALIZADO/PRÓXIMO). En el frontend, el modal "Ver ficha e historial" reemplaza el que filtraba cursos en el navegador.
- **HU095 (opción A aprobada por John):** `GET /api/v1/coordinador/docentes/resumen` devuelve los activos e inactivos por tipo de vinculación. El directorio muestra contadores y chips por tipo, y un selector que filtra en el servidor.

## Documentación

**Completa y actualizada:**
- **Work item:** PLAN, CONTRACT, TEST_PLAN, VALIDATION, CONTRACT_MATRIX y CLOSURE en backend; PLAN, TEST_PLAN y VALIDATION en DB y en frontend.
- **Backend:** `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, `docs/work-items/README.md` y `MANUAL_VALIDATION_LEDGER` (MV-008).
- **DB:** `DOCUMENTACION_PROCEDIMIENTOS_PUBLICOS` (addendum) y `especificacion_sp_endpoints`.
- **Frontend:** `frontend-backend-contract.md` y `MAPA_COMPONENTES_FRONTEND.md`.

## Riesgos residuales

- Las 2 rutas nuevas requieren reaprovisionar el PDP del Security Baseline (Alejo).
- La ficha ya no muestra horario ni aula de los cursos del período; siguen disponibles en las pantallas de grupos y horarios.
- Sin commit RED separado: las pruebas se escribieron junto con el código.

## Siguiente paso

Que John confirme el push y los PR. Después: MV-008 con un coordinador real.
