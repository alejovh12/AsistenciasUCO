# TEST PLAN — FIX-003

## Backend

| ID | Escenario | Resultado observable |
|---|---|---|
| BK-01 | Coordinador nuevo | comando DB, rerelectura por documento y `crearCuenta` con el mismo `idUsuario` |
| BK-02 | Reintento con usuario existente por documento y correo | no se crea otra identidad; se ejecuta la reprovisión idempotente sobre el usuario canónico |
| BK-03 | Correo y documento pertenecen a usuarios distintos | `ConflictException`; no se ejecuta el comando |
| BK-04 | El comando no permite releer la identidad | error interno controlado; no se declara aprovisionamiento completo |

## Frontend

| ID | Escenario | Resultado observable |
|---|---|---|
| FE-01 | Doble clic durante alta de decano | un solo POST; botón ocupado/deshabilitado |
| FE-02 | Doble clic durante alta de coordinador | un solo POST; botón ocupado/deshabilitado |
| FE-03 | GET inicial antiguo termina después del GET posterior | la respuesta antigua no reemplaza la lista nueva |
| FE-04 | Recarga confirmada no contiene la identidad creada | se informa al usuario que no debe repetir el alta |
| FE-05 | Error HTTP en alta | se muestra error y el formulario vuelve a estar disponible |
| FE-06 | Estado PDP pendiente | el panel conserva `PENDIENTE DE SINCRONIZACIÓN`; no se presenta como acceso activo |

No se ejecutan escrituras contra `gestionasistenciadb` ni se crean identidades
sintéticas. La validación E2E con la sesión legítima queda separada del gate local.
