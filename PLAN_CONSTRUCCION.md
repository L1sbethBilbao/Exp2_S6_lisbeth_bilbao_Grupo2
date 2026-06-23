# Plan de construccion: Microservicio empresa-transportista-efs (Semana 3)

> **Estado:** Capa de negocio + capa cloud completadas. Pendiente: push/deploy, pruebas en EC2 y grabacion del video.

Documentos relacionados:
- [AWS_SETUP.md](AWS_SETUP.md) — Infraestructura cloud paso a paso
- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — Pruebas y guion del video
- [HELP.md](HELP.md) — Resumen rapido del proyecto

---

## Contexto

Microservicio Spring Boot para **gestion de pedidos** y **generacion automatica de guias PDF**, usando **EFS** (temporal) + **S3** (persistente), desplegado en **EC2** via **Docker** y **GitHub Actions**.

- **Capa negocio:** pedidos en memoria + PDF generado por el sistema (patron S2 `ResumenArchivoService`)
- **Capa cloud:** igual al profesor (`AwsS3Controller`, `AwsS3Service`, `EfsService`)

Referencia del profesor: `ms-administracion-archivos` — misma capa S3/EFS, adaptada al dominio transportista con keys `{fecha}/{transportista}/guia-{id}.pdf`.

Repositorio Git: `https://github.com/L1sbethBilbao/Exp1_S3_lisbeth_bilbao-Grupo2.git`

---

## Paso 0 — Spring Initializr (referencia)

| Campo | Valor correcto |
|-------|----------------|
| Project | Maven |
| Language | Java |
| Spring Boot | **3.3.13** (no 4.0.6) |
| Group | com.duoc |
| Artifact | empresa-transportista-efs |
| Package | com.duoc.empresa_transportista_efs |
| Packaging | Jar |
| Configuration | Properties |
| Java | 21 |

**Dependencias Initializr:** Spring Web, Lombok.

**Agregar manualmente en pom.xml:**
- `spring-cloud-aws-starter-s3` + BOM 3.3.1
- `spring-boot-starter-validation`
- `openpdf` 2.0.3 (generacion de PDF)

**No usar:** JPA, Security, Actuator.

---

## Paso 1 — Estructura implementada

```
src/main/java/com/duoc/empresa_transportista_efs/
├── controller/
│   ├── PedidoController.java          ← CRUD pedidos + generar-guia
│   └── AwsS3Controller.java           ← capa cloud (igual al profesor)
├── model/
│   └── Pedido.java
├── service/
│   ├── PedidoService.java             ← pedidos en memoria
│   ├── PedidoGuiaService.java         ← orquesta PDF → EFS → S3
│   ├── GuiaGeneradorService.java      ← genera PDF con OpenPDF
│   ├── GuiaDespachoService.java       ← keys y consulta por prefijo
│   ├── AwsS3Service.java              ← upload, uploadBytes, download, delete, list, move
│   └── EfsService.java                ← saveToEfs, saveBytes
├── dto/
│   ├── PedidoRequest.java / PedidoResponse.java
│   ├── GuiaConsultaResponse.java / GuiaCreadaResponse.java
│   ├── GuiaMetadataDto.java / S3ObjectDto.java / ErrorResponse.java
└── exception/
    ├── GlobalExceptionHandler.java
    ├── ResourceNotFoundException.java / GuiaYaGeneradaException.java
    └── S3*Exception.java, InvalidFileException.java
```

---

## Paso 2 — Configuracion

Archivo: `src/main/resources/application.properties`

- `server.port=8080`
- `efs.path=/app/efs`
- `aws.s3.bucket=${AWS_S3_BUCKET:tu-bucket-guias}`
- Logs AWS en DEBUG

Perfil local: no aplica — las pruebas se realizan directamente en EC2 con Postman.

---

## Paso 3 — Regla de negocio: key S3/EFS

```text
{fecha}/{transportista}/guia-{pedidoId}.pdf
```

Ejemplos:
- `20250604/TransportesSur/guia-PED-001.pdf`
- `20250605/TransportesNorte/guia-PED-002.pdf`

Flujo `generar-guia`: generar PDF → EFS → S3.

---

## Paso 4 — API REST

### Capa negocio (`/api/pedidos`)

| Metodo | Ruta | Accion |
|--------|------|--------|
| POST | `/api/pedidos` | Crear pedido |
| GET | `/api/pedidos` | Listar pedidos |
| GET | `/api/pedidos/{id}` | Obtener pedido |
| PUT | `/api/pedidos/{id}` | Actualizar pedido |
| DELETE | `/api/pedidos/{id}` | Eliminar pedido |
| POST | `/api/pedidos/{id}/generar-guia` | Generar PDF y guardar en EFS + S3 |

Campos del pedido: `cliente`, `direccion`, `descripcion`, `transportista`, `fecha`.

### Capa cloud (`/s3/{bucket}` — igual al profesor)

| Metodo | Ruta | Accion |
|--------|------|--------|
| GET | `/s3/{bucket}/objects` | Listar todos los objetos del bucket |
| GET | `/s3/{bucket}/consulta` | Consultar por fecha/transportista |
| GET | `/s3/{bucket}/object` | Descargar PDF |
| POST | `/s3/{bucket}/object` | Subir guia manual (EFS + S3) |
| PUT | `/s3/{bucket}/object` | Actualizar guia |
| POST | `/s3/{bucket}/move` | Mover objeto |
| DELETE | `/s3/{bucket}/object` | Eliminar guia (solo S3) |

Sin Spring Security esta semana.

---

## Paso 5 — Servicios

| Servicio | Responsabilidad |
|----------|-----------------|
| **PedidoService** | CRUD en `ConcurrentHashMap`; IDs `PED-001`, `PED-002`, ... |
| **GuiaGeneradorService** | Crea PDF simple con datos del pedido (OpenPDF) |
| **PedidoGuiaService** | `generarGuia(id)` → PDF → `EfsService.saveBytes` → `AwsS3Service.uploadBytes` |
| **GuiaDespachoService** | `buildKey`, `resolveKey`, `consultarGuias` por prefijo |
| **EfsService** | `saveToEfs` (multipart) y `saveBytes` (PDF generado) |
| **AwsS3Service** | Operaciones S3; bucket como parametro en URL |

---

## Paso 6 — Dockerfile

Multi-stage con Java 21. JAR: `empresa-transportista-efs-1.0.0.jar`. Carpeta `/app/efs`.

```bash
docker run -d --name empresa-transportista-efs \
  -p 8080:8080 \
  -v /home/ec2-user/efs:/app/efs \
  -e EFS_PATH=/app/efs \
  -e AWS_S3_BUCKET=tu-bucket \
  -e AWS_REGION=us-east-1 \
  TU_USUARIO/empresa-transportista-efs:latest
```

---

## Paso 7 — Infraestructura AWS

Ver [AWS_SETUP.md](AWS_SETUP.md).

Cadena EFS para el video:

```
Microservicio → /app/efs → EC2 /home/ec2-user/efs → Amazon EFS
```

Estructura esperada tras generar 2 guias:

```
/home/ec2-user/efs/
├── 20250604/TransportesSur/guia-PED-001.pdf
└── 20250605/TransportesNorte/guia-PED-002.pdf
```

En S3: mismas keys (sin carpetas fisicas; la consola las muestra como prefijos).

---

## Paso 8 — CI/CD

Workflow: `.github/workflows/deploy.yml`

Push a `main` → Maven build → Docker Hub → SSH deploy EC2.

Secrets: `DOCKERHUB_USERNAME`, `DOCKERHUB_TOKEN`, `EC2_HOST`, `USER_SERVER`, `EC2_SSH_KEY`, `AWS_ACCESS_KEY_ID`, `AWS_SECRET_ACCESS_KEY`, `AWS_SESSION_TOKEN`, `AWS_REGION`, `AWS_S3_BUCKET`, `EFS_MOUNT_PATH`, `EFS_PATH`.

---

## Paso 9 — Pruebas y video

Coleccion Postman: `postman/Pruebas-Semana3.postman_collection.json`

Dos carpetas secuenciales:
1. **Negocio** — pedidos + generar guia (sin subir PDF manual)
2. **Cloud** — listar bucket, consultar, descargar, PUT, DELETE

Ver [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) para checklist completo por criterio de pauta.

---

## Orden de implementacion (estado)

| # | Tarea | Estado |
|---|-------|--------|
| 1 | Corregir pom.xml | Completado |
| 2 | application.properties | Completado |
| 3 | EfsService + AwsS3Service | Completado |
| 4 | GuiaDespachoService + AwsS3Controller | Completado |
| 5 | Excepciones globales | Completado |
| 6 | Capa negocio (Pedido, PDF, generar-guia) | Completado |
| 7 | Postman 2 carpetas + docs | Completado |
| 8 | Dockerfile + GitHub Actions | Completado |
| 9 | AWS (S3, EFS, EC2, IAM) | Manual — en EC2 |
| 10 | Pruebas Postman en EC2 | Pendiente |
| 11 | Video demostracion | Pendiente |

---

## Que NO implementar

- Base de datos (pedidos en memoria)
- Spring Security / permisos
- Microservicios adicionales
- Frontend
