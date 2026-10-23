# Despliegue en AWS EC2

Esta guía despliega el proyecto académico en una instancia **EC2 Ubuntu 24.04 LTS x86_64**. Docker Compose ejecuta Spring Boot y PostgreSQL. La API se publica en el puerto HTTP 80; PostgreSQL se mantiene en la red interna de Docker.

```text
Internet → EC2:80 → API:8080 → PostgreSQL:5432 → volumen postgres_data
```

Elige el tamaño según tu presupuesto; para construir la imagen en el servidor, se recomiendan al menos **2 GiB de RAM**. AWS puede cobrar por cómputo, almacenamiento y dirección IPv4; revisa los créditos y precios que aplican a tu cuenta antes de crear recursos. [Guía oficial de EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html).

## Opción con CloudShell, incluso sin VPC predeterminada

Desde CloudShell en la región **us-east-2**, clona el repositorio y ejecuta:

```bash
git clone https://github.com/sbarahona-m17/clinica-api.git clinica-despliegue
cd clinica-despliegue
bash scripts/deploy-aws.sh
```

El script usa [`infra/aws.yaml`](../infra/aws.yaml) para crear una red propia, subred pública, salida a internet, grupo de seguridad HTTP y una EC2 `t3.small` con disco EBS de 16 GiB. No requiere una VPC existente, claves SSH ni un rol IAM en la instancia. La cuenta que ejecuta CloudShell debe tener permisos de CloudFormation, EC2 y lectura del parámetro público SSM de Ubuntu.

La contraseña de PostgreSQL se genera dentro de EC2 y se guarda en `.env`; no aparece en la consola ni en GitHub. Solo se publica el puerto 80. El script espera una respuesta de salud `UP` antes de anunciar el enlace listo. Si falla, comparte los eventos mostrados antes de volver a ejecutar; no crees una segunda instancia para resolver el mismo error.

La instancia permanece activa hasta que la detengas. Cuenta las tres horas de revisión desde que hayas entregado el enlace al profesor; puedes cerrar CloudShell y apagar tu computador. Después de la revisión, usa el comando `aws ec2 stop-instances` que imprime el script. Detener EC2 conserva el disco, que puede seguir generando cargos. [Información de facturación EBS con instancias detenidas](https://repost.aws/knowledge-center/ebs-charge-stopped-instance).

Para eliminar definitivamente los recursos cuando ya no necesites los datos, desde la consola de CloudFormation selecciona el stack `clinica-api-sofia` y revisa la acción **Delete**: elimina también el servidor y su disco, incluidos los datos PostgreSQL. No lo elimines antes de la revisión. El despliegue manual alternativo se explica a continuación.

## 1. Publicar el código

El proyecto está publicado en [sbarahona-m17/clinica-api](https://github.com/sbarahona-m17/clinica-api). Los comandos de esta guía contienen `TU_IP_PUBLICA` y `tu-clave.pem`: sustitúyelos por tus valores reales. No subas `.env`, claves `.pem` ni credenciales AWS al repositorio.

## 2. Crear EC2

En la consola de AWS, abre **EC2 → Launch instance**:

1. Nombre: `clinica-api-sofia`.
2. AMI: **Ubuntu Server 24.04 LTS**, arquitectura **64-bit x86**.
3. Tipo: una instancia con al menos 2 GiB de memoria, por ejemplo `t3.small`; revisa su costo.
4. Crea o selecciona un par de claves y guarda el archivo `.pem`.
5. Selecciona una subred pública con salida a internet y habilita una IPv4 pública.
6. Configura el grupo de seguridad:

   | Puerto | Protocolo | Origen | Uso |
   | --- | --- | --- | --- |
   | 22 | TCP | Tu IP pública /32 | SSH |
   | 80 | TCP | `0.0.0.0/0` | API pública para la entrega |

7. Usa un disco EBS de al menos 16 GiB y lanza la instancia.

No abras los puertos 5432 ni 8080. Espera a que la instancia pase sus comprobaciones de estado y copia su IPv4 pública. [Referencia de creación y conexión EC2](https://docs.aws.amazon.com/AWSEC2/latest/UserGuide/EC2_GetStarted.html).

## 3. Conectarte

Desde PowerShell, en la carpeta donde guardaste la clave:

```powershell
ssh -i .\tu-clave.pem ubuntu@TU_IP_PUBLICA
```

Los comandos siguientes se ejecutan en la terminal **Linux de EC2**.

## 4. Instalar Docker

En una instancia Ubuntu nueva, instala Docker desde su repositorio oficial:

```bash
sudo apt update
sudo apt install -y ca-certificates curl git
sudo install -m 0755 -d /etc/apt/keyrings
sudo curl -fsSL https://download.docker.com/linux/ubuntu/gpg -o /etc/apt/keyrings/docker.asc
sudo chmod a+r /etc/apt/keyrings/docker.asc
```

```bash
sudo tee /etc/apt/sources.list.d/docker.sources <<EOF
Types: deb
URIs: https://download.docker.com/linux/ubuntu
Suites: noble
Components: stable
Architectures: amd64
Signed-By: /etc/apt/keyrings/docker.asc
EOF
sudo apt update
sudo apt install -y docker-ce docker-ce-cli containerd.io docker-buildx-plugin docker-compose-plugin
sudo systemctl enable --now docker
sudo docker compose version
```

Estos comandos corresponden a Ubuntu 24.04 x86_64, como se indicó al crear la instancia. [Instalación oficial de Docker en Ubuntu](https://docs.docker.com/engine/install/ubuntu/).

## 5. Configurar y desplegar

```bash
git clone https://github.com/sbarahona-m17/clinica-api.git
cd clinica-api
cp .env.example .env
chmod 600 .env
nano .env
```

En `.env`, cambia `DB_PASSWORD` por una contraseña propia. Una cadena aleatoria de letras y números de al menos 24 caracteres evita problemas de interpretación por el shell o Compose. Guarda con Ctrl+O, Enter y sal con Ctrl+X.

```bash
sudo docker compose -f compose.aws.yaml up --build -d
sudo docker compose -f compose.aws.yaml ps
sudo docker compose -f compose.aws.yaml logs -f api
```

La primera construcción descarga dependencias y ejecuta las pruebas antes de crear la imagen. Cuando veas `Started ClinicaApplication`, sal del seguimiento de logs con Ctrl+C; los contenedores siguen ejecutándose.

La variable `DB_PASSWORD` se lee del `.env` de la raíz. Al iniciar una base nueva se establece esa contraseña; editarla después no cambia automáticamente la contraseña de un usuario PostgreSQL ya creado.

## 6. Verificar el enlace público

En EC2:

```bash
curl --fail http://localhost/actuator/health
curl --fail http://localhost/api/pacientes
```

En tu computador, abre:

```text
http://TU_IP_PUBLICA/
http://TU_IP_PUBLICA/actuator/health
http://TU_IP_PUBLICA/api/pacientes
http://TU_IP_PUBLICA/api/citas
```

La salud debe devolver `{"status":"UP"}`. Para probar los CRUD desde PowerShell en tu computador:

```powershell
powershell -ExecutionPolicy Bypass -File scripts/smoke-test.ps1 -BaseUrl http://TU_IP_PUBLICA
```

Usa **http://**, ya que esta configuración no incluye un certificado TLS. Es una demostración académica sin autenticación: carga únicamente datos ficticios. Una aplicación clínica real requeriría autenticación, HTTPS y controles adicionales.

## 7. Persistencia y actualización

Los datos de PostgreSQL quedan en un volumen Docker dentro del disco EBS. Sobreviven a recrear los contenedores y reiniciar la instancia. **No son una copia de seguridad:** eliminar el volumen o el disco EBS elimina los datos. Conserva el disco y configura copias antes de guardar información que necesites mantener.

Para actualizar el código sin borrar el volumen:

```bash
git pull --ff-only
sudo docker compose -f compose.aws.yaml up --build -d
```

Para detener los contenedores:

```bash
sudo docker compose -f compose.aws.yaml down
```

No agregues `-v`, porque eliminaría el volumen de datos.

Al detener y volver a iniciar EC2, comprueba si cambió su IPv4 pública y actualiza el enlace de entrega si corresponde. Si necesitas una dirección estable, configura una Elastic IP y revisa su costo.

## 8. Entrega y problemas comunes

Completa en el README los enlaces reales de GitHub y AWS. Mantén la instancia encendida hasta la revisión y conserva evidencia de las pruebas y de `docker compose ps`.

| Problema | Qué revisar |
| --- | --- |
| Tiempo de espera al abrir la URL | Instancia encendida, IPv4 pública, subred pública y entrada TCP 80 en el grupo de seguridad |
| API reiniciándose | `sudo docker compose -f compose.aws.yaml logs --tail=100 api` |
| Error de conexión a PostgreSQL | Logs de `db`, `.env` y `DB_HOST=db` |
| Construcción detenida por memoria | Recursos de la instancia; usar al menos 2 GiB o construir en otro equipo |
| 409 al borrar un paciente | Eliminar primero sus citas |

Cuando termine la evaluación, revisa y elimina los recursos que ya no necesites para evitar cargos. Antes de eliminar una instancia o su almacenamiento, guarda los datos que quieras conservar.
