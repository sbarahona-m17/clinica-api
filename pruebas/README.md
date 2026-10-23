# Pruebas del despliegue AWS

**API probada:** http://3.145.8.190

**Estudiante:** Sofía Barahona Marín

## Archivos para entregar

- [`Clinica-AWS.postman_collection.json`](Clinica-AWS.postman_collection.json): colección importable de Postman con 22 solicitudes y 55 validaciones automatizadas.
- [`Clinica-AWS.resultados.json`](Clinica-AWS.resultados.json): reporte real generado por Newman al ejecutar la colección contra AWS el 23 de octubre de 2026. Resultado: **22 solicitudes, 55 validaciones y 0 fallos**.

El reporte contiene las URLs resueltas, cuerpos de las respuestas, códigos HTTP, tiempos y resultados de las validaciones. Todas las solicitudes de la ejecución apuntaron a **3.145.8.190**.

## Ejecutar en Postman

1. Descarga `Clinica-AWS.postman_collection.json` y ábrelo con **Import** en Postman.
2. Selecciona la colección **Clinica Medica - Pruebas AWS - Sofia Barahona**.
3. Abre **Run collection** y ejecuta las 22 solicitudes en el orden incluido, con una iteración.
4. Revisa los resultados de las validaciones.

La variable de colección `baseUrl` ya contiene `http://3.145.8.190`. No se necesita un archivo de entorno ni autenticación. La primera solicitud genera un documento ficticio único; las siguientes guardan los identificadores automáticamente. Ejecuta la colección completa para que las solicitudes finales eliminen los registros creados.

La colección verifica salud, ambos CRUD, relación 1:N, documento duplicado, campos obligatorios, protección de un paciente con citas y respuestas 404 después de eliminar los registros.

## Repetir desde una terminal

Con Node.js y npm instalados, desde la raíz del proyecto:

```powershell
npx --yes newman@6.2.1 run pruebas/Clinica-AWS.postman_collection.json --reporters cli,json --reporter-json-export pruebas/Clinica-AWS.resultados.json --timeout-request 15000 --timeout-script 5000
```

El comando vuelve a ejecutar las solicitudes y reemplaza el reporte con los resultados nuevos. La instancia AWS debe estar encendida. Si se detiene y reinicia y cambia su IPv4, actualiza `baseUrl` antes de repetir las pruebas.

Referencias: [reportes de Newman](https://learning.postman.com/latest-v-12/docs/reference/newman-cli/newman-built-in-reporters), [variables de Postman](https://learning.postman.com/docs/tests-and-scripts/write-scripts/postman-sandbox-reference/pm-variables).
