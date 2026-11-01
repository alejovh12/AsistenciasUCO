# TEST_PLAN — privacidad de logs

| ID | Escenario | Observable | Implementación incorrecta detectada |
|---|---|---|---|
| P01 | Aviso con datos ficticios personales, credenciales, asunto y cuerpo | Ni descripción ni evento de log contienen valores ni campo desconocido | Serializar mapa o lista negra incompleta |
| P02 | Outbox con los mismos datos | Misma ausencia; tipo, id y destinatarios técnicos presentes | Pérdida de trazabilidad o filtración |
| P03 | Entrada nula | Se conserva NullPointerException | Silenciar contrato de error |
| P04 | Flujo outbox y renderización IDENT | Tests existentes conservan payload/semántica | Borrar datos funcionales |

RED_SNAPSHOT: base `acc1f2c9db24a339adabf6b112f3a634c67b44fb`. SHA-256: `LogNotificacionPrivacidadTest.java` 6BE31196E66F222A7E81FCB15F0EE39B55B17833F9BCD0713CA5CAE75C0BA2BE; `LogNotificacionChannelPrivacidadTest.java` 67B1112CA36F0C202E4C21A405C935D884E4C68F65990CBB4714A9ED3947BE2B; `LogNotificacionChannelSenderTest.java` EA2D73996477E9F3932FCF2BAECEC1EA82B7E049C78F390AAF0F0EB817E19CB1. Java 25; `mvnw.cmd -o -B -ntp -Dtest=LogNotificacionPrivacidadTest,LogNotificacionChannelPrivacidadTest,LogNotificacionChannelSenderTest test`: exit 1; 4 pruebas, 3 fallos de privacidad esperados, 0 errores, 0 omitidas. Archivos congelados para GREEN.
