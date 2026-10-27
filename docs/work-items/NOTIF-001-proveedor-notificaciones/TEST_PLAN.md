---
status: frozen-approved
type: test-plan
scope: database-backend
last-reviewed: 2026-10-27
---

# TEST_PLAN — NOTIF-001

Regla innegociable: las pruebas se escriben primero (RED, fallando por la razón correcta: objeto/clase inexistente o aserción de comportamiento) y **no se relajan, eliminan ni se acomodan** para que pasen. Si una prueba parece errónea, se documenta como `TEST_CONTRACT_CONFLICT` con justificación y se consulta al owner. Cada caso lleva un identificador trazable; el commit RED y el GREEN de cada capa son separados.

## 1. Base de datos (T-SQL transaccional con rollback; cero residuos; `@@TRANCOUNT` = 0 al final)

Archivos nuevos en `test/` (patrón de `test_usp_crear_tipo_programa.sql`: fixtures con `NEWID()`, `THROW 53xxx` con código único por aserción, sin depender de la hora del día — márgenes de al menos 3 días para fechas).

### 1.1 `test_usp_registrar_notificacion_saliente.sql`
| ID | Caso | Esperado |
| :-- | :-- | :-- |
| DB-N01 | Alta válida con 1 destinatario | `estadoResultado=1`, fila en `PENDIENTE`, `intentos=0`, `creadoEn` UTC, `proximoIntentoEn=creadoEn` |
| DB-N02 | `tipo` en minúscula con espacios | normalizado a mayúsculas sin espacios |
| DB-N03 | `tipo` nulo, vacío, 2 caracteres, 61 caracteres, con símbolos | `NTF_001`, sin fila |
| DB-N04 | `destinatariosJson` nulo, `'[]'`, no-JSON, objeto en vez de arreglo, 101 elementos, `idUsuario` no-UUID, `rol` vacío | `NTF_001` |
| DB-N05 | `datosJson` no-JSON o > 8000 caracteres | `NTF_001`; nulo → se guarda `{}` |
| DB-N06 | `asunto` de 201 caracteres; `mensaje` de 4001 | `NTF_001` |
| DB-N07 | `asunto` y `mensaje` nulos | éxito (columnas NULL) |
| DB-N08 | `@idCorrelacion` nulo | `CORR_001` |
| DB-N09 | `traceId`/`spanId` informados | persistidos tal cual |
| DB-N10 | Estado resuelto desde el catálogo | si se borra temporalmente la fila `PENDIENTE` del catálogo dentro de la transacción de prueba, el SP falla de forma controlada (no usa GUID quemado) |
| DB-N11 | El SP no altera el nivel de transacción del llamador | `@@TRANCOUNT` igual antes y después |

### 1.2 `test_usp_reservar_notificaciones_salientes.sql`
| ID | Caso | Esperado |
| :-- | :-- | :-- |
| DB-R01 | 3 `PENDIENTE`, `@lote=2` | devuelve 2 filas más antiguas; pasan a `RESERVADA`, `reservadoPor`, `reservadoHasta≈ahora+lease`, `intentos=1` |
| DB-R02 | Fila `FALLIDA_REINTENTABLE` con `proximoIntentoEn` futuro | no se reserva |
| DB-R03 | Fila `FALLIDA_REINTENTABLE` con `proximoIntentoEn` pasado | se reserva |
| DB-R04 | Fila `RESERVADA` con `reservadoHasta` pasado | se recupera (reservada por la nueva instancia, `intentos` incrementa) |
| DB-R05 | Fila `RESERVADA` con reserva vigente | no se devuelve |
| DB-R06 | Filas `ENTREGADA` y `FALLIDA_DEFINITIVA` | nunca se devuelven |
| DB-R07 | `@lote` 0 / 501; `@leaseSegundos` 4 / 3601; `@instancia` nula | `NTF_001` |
| DB-R08 | Sin elegibles | conjunto vacío, `estadoResultado` coherente con el contrato |
| DB-R09 | Orden | por `creadoEn` ascendente |
| DB-R10 | Columnas devueltas | exactamente las del CONTRACT §2.2 |

### 1.3 `test_usp_marcar_notificacion_entregada.sql`
DB-M01 reserva vigente de la misma instancia → `ENTREGADA`, `entregadoEn`, reserva y errores limpios (`GEN_005`); DB-M02 id inexistente → `NTF_002`; DB-M03 otra instancia → `NTF_003` y la fila no cambia; DB-M04 reserva vencida → `NTF_003`; DB-M05 ya `ENTREGADA` → `NTF_003`; DB-M06 correlación nula → `CORR_001`.

### 1.4 `test_usp_registrar_fallo_notificacion.sql`
| ID | Caso | Esperado |
| :-- | :-- | :-- |
| DB-F01 | Reintentable, `intentos=1`, `base=30`, `max=900` | `FALLIDA_REINTENTABLE`, `proximoIntentoEn≈ahora+30 s`, reserva limpia |
| DB-F02 | `intentos=2`/`3`/`4` | +60 s / +120 s / +240 s |
| DB-F03 | `intentos` tal que `base*2^(n-1) > max` | se aplica el tope `max` |
| DB-F04 | `intentos >= maxIntentos` | `FALLIDA_DEFINITIVA` |
| DB-F05 | `@reintentable=0` | `FALLIDA_DEFINITIVA` aunque queden intentos |
| DB-F06 | `detalleError` de 500 caracteres | truncado a 300 |
| DB-F07 | `@reintentable` nulo; `maxIntentos` 0/21; `base` 0; `max < base` | `NTF_001` |
| DB-F08 | Id inexistente / otra instancia / reserva vencida | `NTF_002` / `NTF_003` / `NTF_003` |

### 1.5 `test_usp_depurar_notificaciones_salientes.sql`
DB-P01 terminales más viejas que la retención se borran y `eliminadas` es exacto; DB-P02 terminales recientes se conservan; DB-P03 `PENDIENTE`/`RESERVADA`/`FALLIDA_REINTENTABLE` **nunca** se borran aunque sean viejas; DB-P04 respeta `@lote`; DB-P05 retención 0 y 3651 → `NTF_001`.

### 1.6 Migración y catálogo
DB-S01 la migración `up` ejecutada dos veces no falla y no duplica; DB-S02 `down` revierte y `up` vuelve a aplicar; DB-S03 `EstadoNotificacion` contiene exactamente los 5 códigos con `esTerminal` correcto y el `MERGE` de semilla es idempotente; DB-S04 `CHECK ISJSON` rechaza JSON inválido en inserción directa; DB-S05 FK de `idEstado`.

### 1.7 Concurrencia — `scripts/database/tests/test_notificacion_concurrencia.ps1`
Cargar 200 `PENDIENTE`; lanzar 4 sesiones simultáneas que ejecutan `usp_reservar_notificaciones_salientes(@lote=50)` con instancias distintas. Esperado: la unión de filas devueltas = 200, **intersección vacía** (ninguna fila reservada dos veces), sin bloqueos prolongados (`READPAST`), todas las filas en `RESERVADA` con su dueño correcto. Limpieza de las filas creadas al final; conteo final = inicial.

### 1.8 Regresión DB
`test_suite.sql` completo (con las pruebas nuevas registradas) y `test/test_contract_closure.sql` sin `TEST_FAIL`. Las pruebas ya existentes no se modifican salvo el registro de las nuevas en `test_suite.sql`.

## 2. Backend — unitarias (JUnit 5 + Mockito; sin Spring salvo donde se indica)

### 2.1 `OutboxNotificacionAdapter`
| ID | Caso | Esperado |
| :-- | :-- | :-- |
| BE-O01 | `notificar(null)` | `NullPointerException` |
| BE-O02 | Notificación válida | el repositorio recibe `tipo`, `asunto`, `mensaje`, destinatarios **solo con rol e idUsuario**, `datos`; correlación y `traceId/spanId` del contexto actual; luego `signal()` del despachador |
| BE-O03 | Destinatarios con `nombre` y `correo` | el JSON enviado **no contiene** esos valores (aserción sobre el string serializado) |
| BE-O04 | Sin destinatarios | no llama al repositorio; `skipped` incrementa |
| BE-O05 | El repositorio lanza `RuntimeException` | no se propaga; `failed` incrementa; `WARN` sin `asunto`/`mensaje`/correo |
| BE-O06 | Sin contexto de correlación | no falla (correlación nula tolerada según el contrato del repositorio) |
| BE-O07 | `datos` con más de 8000 caracteres serializados | se rechaza internamente como fallo contabilizado (sin propagar) |

### 2.2 `NotificacionOutboxDispatcher` (con repositorio y sender dobles, reloj y señal controlables)
BE-D01 éxito → `marcarEntregada`; BE-D02 sender lanza `NotificacionNoEntregableException` → `registrarFallo(reintentable=false)`; BE-D03 sender lanza `RuntimeException` → `registrarFallo(reintentable=true)` con los parámetros de backoff/intentos configurados; BE-D04 `marcarEntregada` lanza (`reserva perdida`) → `WARN`, contador `lost_lease`, continúa con la siguiente fila; BE-D05 `reservar` lanza → el bucle sobrevive y reintenta en el siguiente barrido; BE-D06 `signal()` despierta antes del `sweeper-interval`; BE-D07 `stop()` termina el lote en curso y no reserva más; BE-D08 `dispatcher-enabled=false` → no inicia hilo; BE-D09 depuración al arrancar y cada `retention-interval`; BE-D10 un sender idempotente recibe el mismo `id` en reintentos; BE-D11 gauges `pending`/`dead` se actualizan; BE-D12 `instance-id` vacío genera `<hostname>-<8 hex>` y es estable durante la vida del proceso.

### 2.3 `CompositeNotificacionAdapter`
BE-C01 invoca todos los destinos en orden; BE-C02 un destino lanza `RuntimeException` → los demás se invocan igual, no se propaga, `failures{target}` incrementa; BE-C03 todos fallan → no se propaga; BE-C04 `null` → `NullPointerException` y **ningún** destino invocado; BE-C05 el mismo objeto `Notificacion` llega a cada destino.

### 2.4 `LogNotificacionChannelSender`
BE-S01 registra `tipo`, `rol:idUsuario`, claves de `datos` salvo `motivo`, `id`; BE-S02 captura los logs y verifica que **no** aparecen `asunto`, `mensaje`, nombres ni correos; BE-S03 `null` → `NullPointerException`.

### 2.5 Properties
BE-P01 `NotificationAdapterProperties`: `provider` ausente → error; valor desconocido (`kafka`) → no enlaza; `composite` con 0/1 destino, repetidos o `composite` anidado → error con mensaje que nombra la propiedad; `composite` válido (`log,outbox`) en cualquier orden. BE-P02 `OutboxNotificationProperties`: cada rango fuera de límite (batch 0/501, lease 4s, max-attempts 0/21, backoff-max < base, retention 0, etc.) → error con el nombre de la clave; valores por defecto conformes a CONTRACT §3.1; duraciones `60s`/`15m`/`6h` se interpretan correctamente.

## 3. Backend — contrato, wiring y arquitectura

### 3.1 `NotificacionPortContractTest` (abstracta; heredada por `log`, `outbox` y `composite`)
CT-01 acepta una notificación completa sin lanzar; CT-02 `null` → `NullPointerException`; CT-03 sin destinatarios no lanza; CT-04 `datos` vacío y `asunto`/`mensaje` nulos no lanzan; CT-05 con la infraestructura subyacente fallando **nunca** lanza; CT-06 los logs que produce no contienen `asunto`, `mensaje`, `nombre` ni `correo`; CT-07 es segura bajo concurrencia (50 hilos × 20 llamadas, sin excepciones ni pérdida de conteos de la métrica); CT-08 llamar dos veces con el mismo objeto produce dos avisos independientes (sin deduplicación implícita).

### 3.2 Wiring (`ApplicationContextRunner`, sin base de datos; infraestructura JPA/SP sustituida por dobles)
| ID | Configuración | Esperado |
| :-- | :-- | :-- |
| WI-01 | sin propiedades | 1 bean `NotificacionPort` de tipo `LogNotificacionAdapter`; sin despachador ni repositorio outbox |
| WI-02 | `provider=log` | igual a WI-01 |
| WI-03 | `provider=outbox` | 1 `NotificacionPort` de tipo `OutboxNotificacionAdapter`; despachador y sender presentes |
| WI-04 | `provider=composite`, `targets=log,outbox` | 1 `NotificacionPort` de tipo `CompositeNotificacionAdapter` con 2 destinos en orden; los hojas existen como tipo concreto y **no** como `NotificacionPort` |
| WI-05 | `provider=composite`, `targets=outbox,log` | orden invertido respetado |
| WI-06 | `provider=composite` con 1 destino / repetidos / `composite` dentro / lista vacía | el contexto **falla** al arrancar con mensaje claro |
| WI-07 | `provider=kafka` | el contexto falla |
| WI-08 | `provider=outbox`, `sender=smtp` | el contexto falla |
| WI-09 | `provider=outbox`, `dispatcher-enabled=false` | adaptador presente, despachador sin hilo |
| WI-10 | `provider=log` | no se crea ningún bean del paquete `outbox` |
| WI-11 | Ningún caso crea dos beans `NotificacionPort` | |
| WI-12 | `RetirarEstudianteGrupoUseCaseImpl` sigue construible con el `NotificacionPort` resultante en cada modo | |

### 3.3 ArchUnit (`MessagingIsolation…Test`, nuevo)
AR-01 `application..` no depende de `infrastructure..notification..`; AR-02 ningún `@RestController` depende de las clases de `…notification.outbox`/`composite`; AR-03 las clases adaptadoras seleccionables (`OutboxNotificacionAdapter`, `CompositeNotificacionAdapter`) no llevan `@Component/@Service/@Repository` (solo el repositorio JPA lleva `@Repository`); AR-04 solo clases `*Configuration` instancian adaptadores; AR-05 no existe `Map<String, NotificacionPort>` ni `getBean(` en el paquete `notification`; AR-06 `ApplicationShouldNotCarryTechnicalCorrelationTest` y `ApplicationShouldNotDependOnInfrastructureTest` siguen verdes sin cambios.

### 3.4 Repositorio JPA
BE-J01 `usp_registrar_…` invocado con los nombres de parámetro exactos del CONTRACT; BE-J02 `usp_reservar_…` mapea las filas a `NotificacionReservada` (incluidos `datos`/`destinatarios` JSON); BE-J03 códigos `NTF_*` y `CORR_001` se traducen a excepciones de infraestructura (el adaptador/despachador deciden); BE-J04 `JpaRepositoryBeanUniquenessTest` actualizado (+1) y verde.

## 4. Integración (failsafe, `-Pintegration`, SQL Server real del contenedor `sql_server_asistencias`)

`NotificacionOutboxIT` (`@SpringBootTest` con `provider=outbox`, sender de prueba que registra las entregas, parámetros de tiempo reducidos: `sweeper-interval=1s`, `backoff-base=1s`, `lease=5s`):
IT-01 registrar → el despachador entrega → fila `ENTREGADA` (sin polling en la prueba: se usa espera acotada con `Awaitility` o equivalente ya presente); IT-02 sender que falla 2 veces y luego tiene éxito → `intentos=3`, `ENTREGADA`; IT-03 sender que siempre falla → `FALLIDA_DEFINITIVA` tras `max-attempts`, métrica `dead` = 1; IT-04 `NotificacionNoEntregableException` → definitiva en el primer intento; IT-05 **dos** despachadores (dos instancias del bean con distinto `instance-id`) sobre 100 filas → cada `id` se entrega exactamente una vez; IT-06 reserva vencida (despachador detenido tras reservar) → otra instancia la recupera; IT-07 depuración elimina terminales viejas y respeta las demás; IT-08 `provider=composite targets=log,outbox` registra una fila y también escribe el log; IT-09 sin filas residuales al terminar (limpieza por `id` creado en la prueba). Se prohíbe dejar datos en `NotificacionSaliente`.

Baseline de IT: antes de cambiar nada, ejecutar `-Pintegration` sobre `origin/develop` y guardar el resultado; después, sobre la rama. La única diferencia admisible son las IT nuevas en verde. Fallos preexistentes se reportan con su causa, no se ocultan.

## 5. Backend — suites completas y calidad
- `mvnw -o test` y `mvnw -o verify`: 0 fallos; JaCoCo global sobre el gate (LINE ≥ 80 %, BRANCH ≥ 70 %) y **≥ 90 % de líneas y ≥ 85 % de ramas en el código nuevo** (`…notification…` y properties nuevas), medido con el informe y declarado con cifras reales.
- Pruebas de gobernanza de `develop` que deben seguir verdes: `AccessCatalogConsistencyTest` (sin cambios: no hay rutas nuevas), `JpaRepositoryBeanUniquenessTest` (+1), ArchUnit completo, `SourcePackageConsistencyTest`.

## 6. E2E con JWT reales (backend de la rama en `:8090`, nunca en 8080/4200)

Arranque con `APP_ADAPTERS_SECURITY_PROVIDER=legacy`, `APP_ADAPTERS_IDENTITY_PROVIDER=legacy`, `APP_ADAPTERS_NOTIFICATION_PROVIDER=outbox`, `SERVER_PORT=8090` (declarar que **no** se probó contra el PDP del baseline). Script `scratch/test_notif001.ps1` (patrón de `scratch/test_hu133_…ps1`: tokens por `password grant`, `Check`, conteo de PASS/FAIL, SQL de conciliación con `-Q`, **no** por pipe):

| ID | Verificación |
| :-- | :-- |
| E2E-01 | El backend arranca con `outbox` y con `composite log,outbox` (reinicio entre ambos); sin errores de contexto |
| E2E-02 | Con el docente titular real, `POST /api/v1/docente/grupos/{grupoId}/estudiantes/{estudianteId}/retiro` (HU051) responde 2xx y deja **una** fila nueva en `NotificacionSaliente` con `tipo=RETIRO_ESTUDIANTE_GRUPO`, sin `nombre`/`correo` en `destinatarios`, que pasa a `ENTREGADA` en pocos segundos; los roles/ids coinciden con los destinatarios reales de la consulta de control |
| E2E-03 | Modo `composite`: además de la fila, aparece el log del adaptador `log` (`NOTIFICACION_PENDIENTE_DE_CANAL`) |
| E2E-04 | Con `provider=log` (default) el mismo retiro no crea filas en `NotificacionSaliente` |
| E2E-05 | Con el despachador deshabilitado la fila queda `PENDIENTE` y el retiro igualmente responde 2xx (mejor esfuerzo) |
| E2E-06 | Simular fallo del registro (p. ej. renombrar temporalmente el SP **dentro de una base desechable**, o usar un doble) no hace fallar el retiro; si no es seguro hacerlo en la BD local, declararlo como `NO_EJECUTADO` con la razón |
| E2E-07 | Métricas `notification.publish.total` y `notification.dispatch.total` presentes en `/actuator/prometheus` (o la forma de exposición vigente) |
| E2E-08 | Restablecer los datos que el retiro modificó (matrícula del estudiante) o usar un estudiante de prueba creado para el caso; **cero residuos** al terminar (conteo antes = después en `NotificacionSaliente` tras limpiar las filas propias, y matrícula restaurada) |

Evidencia: archivo crudo de la salida (PASS/FAIL/SKIP) guardado fuera del repositorio y cifras copiadas tal cual al `VALIDATION.md`. Prohibido redactar cifras de memoria.

## 7. Matriz de trazabilidad (CONTRACT → pruebas)
| CONTRACT | Pruebas |
| :-- | :-- |
| §2.1 esquema | DB-S01–S05 |
| §2.2 SP | DB-N*, DB-R*, DB-M*, DB-F*, DB-P*, concurrencia |
| §2.3 códigos | DB-N03/N04/N08, DB-M02/M03, DB-F07/F08 |
| §3.2 properties | BE-P01, BE-P02, WI-06–WI-08 |
| §3.3 Composition Roots | WI-01–WI-12, AR-01–AR-05 |
| §3.5 outbox | BE-O*, BE-D*, BE-S*, BE-J*, IT-01–IT-09, E2E-02/05/08 |
| §3.6 composite | BE-C*, WI-04/05, IT-08, E2E-03 |
| §3.7 observabilidad | BE-D11, E2E-07 |
| §4 contrato del puerto | CT-01–CT-08 |
| §5 compatibilidad | WI-12, E2E-04, suites de HU051 y `LogNotificacionAdapterTest` sin cambios |
| §6 seguridad y datos | BE-O03, BE-S02, CT-06, E2E-02 |

## 8. Evidencia mínima exigida en `VALIDATION.md`
Tabla por capa con comando, fecha, cifras reales y enlace al archivo crudo; lista exacta de pruebas existentes modificadas (esperado: solo `JpaRepositoryBeanUniquenessTest` y el registro en `test_suite.sql`); hallazgos y desvíos (CONTRACT §9); lo no ejecutado y por qué; cifras de cobertura del código nuevo.
