# ClamAV local para LB-004B.2H

El servicio usa `clamav/clamav:1.5.4-debian13-slim` fijado al digest multi-plataforma registrado en
`../compose.yaml`. Expone `clamd` solo en `127.0.0.1:3310` para desarrollo con el backend ejecutado
desde IntelliJ. El healthcheck oficial `clamdcheck.sh` espera a que el daemon y sus firmas estén
listos.

La validación EICAR usa únicamente la firma estándar de prueba; nunca malware real. Una caída,
timeout o respuesta inválida de `clamd` es fail-closed y se traduce a error técnico seguro.
