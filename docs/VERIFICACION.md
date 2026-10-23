# Verificación local

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

La validación del archivo Compose de AWS es local. **No se ha creado ni verificado una instancia AWS**. El repositorio está publicado en [GitHub](https://github.com/sbarahona-m17/clinica-api); el despliegue se realiza siguiendo [AWS.md](AWS.md).

Para repetir las pruebas automatizadas y HTTP:

```powershell
.\mvnw.cmd clean verify
docker compose up --build -d
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -BaseUrl http://localhost:8080
```

Espera a que `/actuator/health` responda `UP` antes de ejecutar el script HTTP.
