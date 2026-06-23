# Guia de aprovisionamiento AWS

Pasos para configurar la infraestructura requerida por la actividad Semana 3.

## 1. Bucket S3

1. En AWS Console → S3 → **Create bucket**
2. Nombre unico (ej. `guias-transportista-lisbeth-2026`)
3. Region: la misma que usaras para EC2 y EFS
4. Bloquear acceso publico (recomendado)
5. Anota el nombre y configuralo en:
   - `application.properties` → `aws.s3.bucket`
   - Secret de GitHub → `AWS_S3_BUCKET`

## 2. Amazon EFS

1. AWS Console → EFS → **Create file system**
2. VPC: la misma de tu instancia EC2
3. Crear **mount targets** en las subnets de EC2
4. Security group de EFS: permitir NFS (puerto 2049) desde el security group de EC2

## 3. Instancia EC2

1. Lanzar instancia Amazon Linux 2023 o Ubuntu
2. Asignar **Elastic IP**
3. Security group: abrir puerto **8080** (HTTP del microservicio) y **22** (SSH)
4. Asociar **IAM Role** con permisos S3 (ver seccion 4)
5. Misma VPC/subnet que EFS

### Instalar Docker en EC2

```bash
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user
```

## 4. IAM Role para EC2

Crear rol con policy (ajusta el nombre del bucket):

```json
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Action": [
        "s3:PutObject",
        "s3:GetObject",
        "s3:DeleteObject",
        "s3:ListBucket"
      ],
      "Resource": [
        "arn:aws:s3:::tu-bucket-guias",
        "arn:aws:s3:::tu-bucket-guias/*"
      ]
    }
  ]
}
```

Asociar el rol a la instancia EC2. **No** hardcodear access keys en el codigo.

## 5. Montar EFS en EC2

```bash
# Amazon Linux - instalar cliente NFS
sudo yum install -y amazon-efs-utils

# Crear punto de montaje
sudo mkdir -p /home/ec2-user/efs

# Montar (reemplaza fs-xxxxx con tu ID de EFS)
sudo mount -t efs fs-XXXXXXXX:/ /home/ec2-user/efs

# Verificar
df -h
```

Para montaje persistente al reiniciar, agregar a `/etc/fstab`:

```
fs-XXXXXXXX:/ /home/ec2-user/efs efs defaults,_netdev 0 0
```

## 6. Ejecutar contenedor Docker en EC2

```bash
docker pull TU_USUARIO/empresa-transportista-efs:latest

docker run -d \
  --name empresa-transportista-efs \
  --restart unless-stopped \
  -p 8080:8080 \
  -v /home/ec2-user/efs:/app/efs \
  -e AWS_S3_BUCKET=tu-bucket-guias \
  -e AWS_REGION=us-east-1 \
  TU_USUARIO/empresa-transportista-efs:latest
```

## 7. Verificar cadena EFS (para el video)

```bash
# En EC2
df -h
ls -R /home/ec2-user/efs

# Dentro del contenedor
sudo docker exec -it empresa-transportista-efs bash
df -h
ls -R /app/efs
```

Flujo a explicar:

```
Microservicio → /app/efs → Linux EC2 /home/ec2-user/efs → Amazon EFS
```

## 8. Coleccion Postman

Importar en Postman:

```
postman/Pruebas-Semana3.postman_collection.json
```

Editar variables `ec2_host` y `bucket`. Ejecutar en orden:

1. **Carpeta 1 — Negocio:** crear pedidos y generar guias (PDF automatico)
2. **Carpeta 2 — Cloud:** listar bucket, consultar, descargar, PUT, DELETE

Ver [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) para el flujo completo y el guion del video.

## 9. Secrets de GitHub Actions

Configurar en el repositorio → Settings → Secrets:

| Secret | Descripcion |
|--------|-------------|
| `DOCKERHUB_USERNAME` | Usuario Docker Hub |
| `DOCKERHUB_TOKEN` | Token de acceso Docker Hub |
| `EC2_HOST` | IP elastica de EC2 (ej. 52.45.88.121) |
| `USER_SERVER` | `ec2-user` (Amazon Linux) |
| `EC2_SSH_KEY` | Clave privada **dsy2204-1.pem** completa |
| `AWS_ACCESS_KEY_ID` | Credencial temporal AWS Academy |
| `AWS_SECRET_ACCESS_KEY` | Credencial temporal AWS Academy |
| `AWS_SESSION_TOKEN` | Token de sesion AWS Academy |
| `AWS_REGION` | Region AWS (ej. us-east-1) |
| `AWS_S3_BUCKET` | Nombre del bucket S3 (ej. cdy2204-1) |
| `EFS_MOUNT_PATH` | Ruta EFS en EC2 (default: `/home/ec2-user/efs`) |
| `EFS_PATH` | Ruta EFS en contenedor (default: `/app/efs`) |

**EFS:** montar en EC2 con `sudo mount -t efs fs-XXXXXXXX:/ /home/ec2-user/efs`. El deploy mapea `-v EFS_MOUNT_PATH:EFS_PATH`.

**Nota:** La conexion SSH a EC2 usa `USER_SERVER` + `EC2_SSH_KEY`. Las credenciales AWS se pasan al contenedor Docker para S3.

**Importante:** Las credenciales AWS Academy **expiran**. Cuando caduquen, actualiza los 3 secrets en GitHub y vuelve a ejecutar el workflow.

## Arquitectura del codigo (referencia)

```
PedidoController (/api/pedidos)
    └── PedidoGuiaService
            ├── GuiaGeneradorService  (PDF OpenPDF)
            ├── EfsService.saveBytes
            └── AwsS3Service.uploadBytes

AwsS3Controller (/s3/{bucket}/...)
    ├── AwsS3Service (igual al profesor)
    ├── EfsService (igual al profesor)
    └── GuiaDespachoService (resolveKey, consultarGuias)
```

El bucket va en la URL como path variable `/{bucket}/`, igual que el proyecto del profesor.

Tras generar 2 guias (Postman carpeta 1), la estructura en EFS y S3 es:

```
20250604/TransportesSur/guia-PED-001.pdf
20250605/TransportesNorte/guia-PED-002.pdf
```

## Checklist antes de grabar el video

- [ ] EFS montado en `/home/ec2-user/efs` (`df -h` lo muestra)
- [ ] Contenedor corriendo en puerto 8080
- [ ] Bucket S3 creado y accesible desde EC2
- [ ] `POST /api/pedidos` + `POST .../generar-guia` crea PDF en EFS y S3
- [ ] `GET /s3/{bucket}/objects` muestra las keys ordenadas por fecha/transportista
- [ ] GitHub Actions despliega al hacer push a `main`
