# Verificación local y en AWS

Fecha: **23 de octubre de 2026**.

| Comprobación ejecutada | Resultado |
| --- | --- |
| `mvnw.cmd -B -ntp verify` en Windows con JDK 25, compilando para Java 21 | BUILD SUCCESS; 11 pruebas, 0 fallos, 0 errores |
| Construcción multietapa del Dockerfile con Maven y JDK 21 | BUILD SUCCESS; 11 pruebas, 0 fallos, 0 errores |
| `docker compose up --build -d` | API y PostgreSQL iniciados; base de datos healthy |
| `scripts/smoke-test.ps1` contra `http://localhost:8080` | CRUD de ambas entidades, relación 1:N, documentos duplicados y protección de paciente con citas verificados |
| Persistencia con PostgreSQL | Se crearon un paciente y una cita; se ejecutó `docker compose down` y `docker compose up -d`; ambos registros y su relación se conservaron |
| Limpieza | Se eliminaron los registros ficticios usados en las pruebas HTTP |
| `GET /actuator/health` después de recrear los contenedores | HTTP 200, `{"status":"UP"}` |
| `docker compose --env-file .env.example -f compose.aws.yaml config --quiet` | Configuración válida |
| `cfn-lint -t infra/aws.yaml -r us-east-2` | Plantilla de infraestructura válida, sin errores ni advertencias |
| `bash -n scripts/deploy-aws.sh` y script UserData extraído de la plantilla | Sintaxis Bash válida |

## Verificación del despliegue AWS

Despliegue en **EC2, us-east-2 (Ohio)**, con Docker Compose y PostgreSQL. Inicio de disponibilidad comunicado por el script: **2026-10-23 17:25:15 UTC** (12:25:15 de Colombia).

Pruebas ejecutadas desde un equipo externo a AWS el **23 de octubre de 2026, aproximadamente a las 17:26 UTC**:

| Comprobación | Resultado |
| --- | --- |
| Portada `http://3.145.8.190/` | HTTP 200 |
| `/actuator/health` | HTTP 200, `{"status":"UP"}` |
| Crear, listar, consultar, actualizar y eliminar pacientes | Correcto: 201, 200 y 204 |
| Crear, listar, consultar, actualizar y eliminar citas | Correcto: 201, 200 y 204 |
| Un paciente con dos citas | Relación 1:N verificada |
| Documento duplicado | HTTP 409 |
| Eliminar paciente con citas | HTTP 409 |
| Consultar registros después de eliminarlos | HTTP 404 |
| Limpieza de datos ficticios de prueba | Completada |

Comando ejecutado con resultado correcto:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -BaseUrl http://3.145.8.190
```

El repositorio está publicado en [GitHub](https://github.com/sbarahona-m17/clinica-api). La instancia debe mantenerse encendida durante la revisión; las comprobaciones anteriores describen el estado observado, no un monitoreo continuo. La guía de operación está en [AWS.md](AWS.md).

## Colección Postman y reporte JSON del despliegue

Se ejecutó la colección [`Clinica-AWS.postman_collection.json`](../pruebas/Clinica-AWS.postman_collection.json) usando Newman contra **http://3.145.8.190**, con resultado **22 solicitudes, 55 validaciones, 0 fallos**. La ejecución incluyó ambos CRUD, la relación 1:N y los casos de error; los registros ficticios se eliminaron al terminar.

El reporte original de la ejecución está en [`Clinica-AWS.resultados.json`](../pruebas/Clinica-AWS.resultados.json). Incluye solicitudes con URLs resueltas del despliegue, respuestas y resultados de las validaciones.

## Repetir las pruebas locales

Para repetir las pruebas automatizadas y HTTP:

```powershell
.\mvnw.cmd clean verify
docker compose up --build -d
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

Espera a que `/actuator/health` responda `UP` antes de ejecutar el script HTTP.
