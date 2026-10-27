---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# CLOSURE — HU008-oferta-grupos-estudiante

## Estado

**DONE local:** los gates locales están en verde ([VALIDATION.md](VALIDATION.md)).

La rama `feature/HU008-oferta-grupos-estudiante` está en commits locales en DB, backend y frontend, sin subir hasta que John confirme.

Pendiente:
- push y PR hacia `develop` (DB → backend → frontend);
- CI remoto;
- reaprovisionar el PDP con 1 ruta;
- MV-008.

## Entregado

- **Oferta:** `GET /api/v1/estudiante/oferta-academica` y la pantalla "Oferta de Grupos".
  - Muestra las específicas de los programas del estudiante y las transversales (básicas e institucionales) de su institución.
  - Cada grupo trae docente, horario, cupos y estado del período.
  - Desde ahí el estudiante solicita el ingreso (flujo HU053), o ve que ya está matriculado o que tiene una solicitud abierta.
- **Regla de John (J4):** una matrícula en una transversal ya no inscribe al estudiante en el programa de esa asignatura. Solo las ESP lo inscriben.
- **Opción futura (J5):** el parámetro `OFERTA.INCLUIR_TRANSVERSALES` permite ocultar las transversales (en `0`, solo se ingresa con el enlace del docente). Se cambia con:

  ```sql
  UPDATE dbo.CatalogoParametro SET valor = '0' WHERE grupo = 'OFERTA' AND clave = 'INCLUIR_TRANSVERSALES';
  ```

## Documentación

Completa y actualizada:
- Work items: PLAN, CONTRACT, TEST_PLAN, VALIDATION, CONTRACT_MATRIX y CLOSURE en backend; PLAN, TEST_PLAN y VALIDATION en DB y en frontend.
- Nota de la regla J4 en los work items de HU053-055 (backend CLOSURE y DB PLAN).
- Backend: `HTTP_AS_IS_MATRIX`, `LINEA_BASE`, `docs/work-items/README.md` y `MANUAL_VALIDATION_LEDGER` (MV-008).
- DB: `DOCUMENTACION_PROCEDIMIENTOS_PUBLICOS` (addendum) y `especificacion_sp_endpoints`.
- Frontend: `frontend-backend-contract.md` y `MAPA_COMPONENTES_FRONTEND.md`.

## Riesgos residuales

- La ruta nueva requiere reaprovisionar el PDP (Alejo).
- Las filas `EstudiantePrograma` creadas antes por transversales siguen en la base. La depuración queda a decisión de John.
- Clasificar asignaturas como obligatorias o electivas, o hacer que una asignatura pertenezca a varios programas, requeriría tablas nuevas. Fuera de alcance.
- No hay commit RED separado.

## Siguiente paso

Que John confirme el push y los PR. Después, MV-008 con un estudiante real.
