---
status: active
type: work-item
scope: fullstack-database-backend-frontend
owner: fullstack-team
last-reviewed: 2026-10-27
---

# TEST_PLAN — HU094/HU095: ficha del docente y consolidado del directorio

## Fuentes

- [PLAN.md](PLAN.md): alcance A1-A3 acordado con John.
- [CONTRACT.md](CONTRACT.md).
- `SecurityConfig` (`/api/v1/coordinador/**`).
- `infra/security-baseline/access-catalog.json`.

## Behavioral Matrix

| ID | Requirement | Scenario | Precondition | Action | Observable | Expected | Wrong implementation caught | Level |
|---|---|---|---|---|---|---|---|---|
| H-01 | A1, A3 | Historial completo del docente en el programa | D1 con 4 grupos en P1 (vigente ×2, finalizado, próximo), 1 en P2 y otro titular en P1 | EXEC | filas y orden | 4 filas; orden por inicio del período descendente y desempate por asignatura; excluye P2 y otro titular | historial global o de otros titulares | DB real |
| H-02 | A1 | Estado del período y resumen | idem | EXEC | columnas | VIGENTE, FINALIZADO y PROXIMO; activos y capacidad del grupo; resumen 4/3/2/3 en todas las filas | resumen calculado solo sobre la página | DB real |
| H-03 | A1 | Filtro por período y paginación | idem | EXEC con período, página 2 y tamaño 1; página 0 y tamaño 500 | filas | 1 fila (G6), `totalRegistros` 2, resumen 4; normaliza a 4 filas | resumen dependiente del filtro; paginación sin acotar | DB real |
| H-04 | A1 | Docente vinculado sin grupos | D2 sin grupos | EXEC | filas | 0 filas, sin error | 404 para un docente válido | DB real |
| H-05 | A3 | Ámbito del docente | D3 sin vínculo con P1, id inexistente, coordinador de P2 | EXEC | error y filas | `GEN_001` ×2; C2 solo ve el grupo de P2 | revelar docentes de otro programa | DB real |
| H-06 | Validación | Decano, docente nulo, ejecutor nulo | — | EXEC | error | `KPI_001`, `VAL_001` y `VAL_001` | consulta sin programa | DB real |
| R-01 | A2 | Consolidado por tipo | P1: CAT activo e inactivo, PLA activo, MTI con usuario inactivo; P2: PLA activo | EXEC | filas | CAT 1/1, PLA 1/0, MTI 0/1, OCA 0/0; total 4 | contar P2, ignorar la cuenta inactiva, omitir tipos en cero | DB real |
| R-02 | A3 | Ámbito del consolidado | coordinador de P2 | EXEC | filas | total 1 (PLA) | consolidado global | DB real |
| R-03 | Validación | Decano y ejecutor nulo | — | EXEC | error | `KPI_001` y `VAL_001` | — | DB real |
| FD-01..08 | A1, A3 | Caso de uso de la ficha | puertos mock | execute | dominio y llamadas | cabecera del directorio filtrada por docente y programa; historial con paginación y período; por defecto 1/10; directorio vacío ⇒ 404 sin consultar el historial; sin programa ⇒ 403; validaciones ⇒ 400; constructores; interactor DTO↔dominio | historial de un docente ajeno; NPE | Unit |
| RD-01..05 | A2 | Caso de uso del consolidado | puertos mock | execute | totales | suma de activos e inactivos; ceros sin docentes; 403 sin consultar; DTO con total por tipo; constructores | totales mal sumados | Unit |
| HR-01..06 | Persistencia | Repositorio del historial | EM mock | consultarHistorial | SQL, parámetros y proyección | `EXEC` con parámetros nombrados y correlación; mapeo de 21 columnas y del resumen; vacío ⇒ ceros; ejecutor nulo sin DB; `DatabaseOperationException`; fechas `LocalDate`, `LocalDateTime` y `java.util.Date` sin java.sql | SP o columna equivocados | Unit |
| RR-01..03 | Persistencia | Repositorio del consolidado | EM mock | consultarResumen | SQL y proyección | `EXEC` nombrado; mapeo con nulos a 0; ejecutor nulo sin DB; fallo técnico traducido | — | Unit |
| CLH-01..02 | Errores | `GEN_001` | — | classify | código | 404 en el historial; sin clasificar en otra operación | 500 genérico o 404 global | Unit |
| FC-01..05, RC-01..02 | HTTP | Controlador | puertos mock | GET | status y JSON | ficha serializada con valores por defecto; período y paginación propagados; 400 fuera de rango sin llamar al caso de uso; 404; consolidado serializado; 403 | validación ausente | MockMvc |
| CAT | Seguridad | Catálogo vs Spring vs `SecurityConfig` | 2 filas nuevas | `AccessCatalogConsistencyTest` | — | consistente | endpoint sin regla | Unit |
| FM-01..06 | UI | Modal de la ficha | servicio mock | abrir y paginar | DOM y llamadas | consulta con página 1 y tamaño 5; cabecera, resumen, historial con estado; reconsulta al paginar; estado vacío; error visible; cerrar | filtrar en el cliente | Frontend unit |
| DIR-094 | UI | Directorio abre la ficha | — | ver y cerrar ficha | DOM y llamadas | la ficha solo se consulta al abrirla | consulta al cargar la pantalla | Frontend unit |
| DIR-095-01..04 | UI | Consolidado y filtro | — | cargar, clic en chip, selector, cambio de estado | DOM y llamadas | contadores visibles; chip y selector filtran en el servidor y vuelven a la página 1; segundo clic quita el filtro; el consolidado se relee tras el cambio de estado; un error no rompe la pantalla | filtro local o contadores obsoletos | Frontend unit |
| SRV / MOCK | UI | Servicio y mock | HttpTestingController | llamadas | URL y parámetros | `/ficha` con página, tamaño y período opcional; `/resumen`; el mock responde ambos | URL equivocada | Frontend unit |

## Integration Requirement

- **REAL_PROVIDER_REQUIRED:** sí para la DB; no para backend ni frontend.
- **ENVIRONMENT:** SQL Server 2022 desechable (réplica del job DB Quality Gate).
- **MOCK_SUFFICIENT:** sí para backend y frontend.
- **WHY:** el ámbito del programa, el estado del período y los contadores viven en la DB.

## Negative / Boundary Coverage

- Ejecutor nulo, decano, docente nulo, docente inexistente, docente de otro programa y docente sin grupos.
- Coordinador de otro programa.
- Página 0, tamaño 0, 101 y 500.
- Filtro por período.
- Cuenta de usuario inactiva, tipo de vinculación sin docentes y error HTTP en la ficha y en el consolidado.

## Side Effects / Rollback

Solo lectura. Las pruebas de DB insertan los fixtures dentro de una transacción con ROLLBACK.

## Pruebas RED requeridas

- **DB:** `test/test_usp_consultar_historial_grupos_docente_programa.sql` y `test/test_usp_consultar_resumen_docentes_programa.sql`. Fallo esperado: SP inexistentes.
- **Backend:** `ConsultarFichaDocenteUseCaseImplTest`, `ConsultarResumenDocentesUseCaseImplTest`, `DocenteProgramaReporteJpaRepositoryTest`, `CoordinadorDocentesReporteControllerTest` y `DbFailureClassifierHistorialDocenteTest`. Fallo esperado: clases inexistentes.
- **Frontend:** `coordinator-docente-ficha-modal.component.spec.ts` y los casos HU094/HU095 de `coordinator-docentes.component.spec.ts`. Fallo esperado: componente y métodos inexistentes.
