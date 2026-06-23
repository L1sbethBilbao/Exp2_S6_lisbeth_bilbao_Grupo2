# Guia de pruebas Postman y checklist de video

Base URL: `http://<IP-ELASTICA-EC2>:8080`

Reemplaza los valores de ejemplo segun tu entorno.

**Coleccion Postman:** `postman/Pruebas-Semana3.postman_collection.json`

**Importar:** Postman → Import → seleccionar el archivo JSON → editar variable `ec2_host` con tu IP.

La coleccion tiene dos carpetas secuenciales:
- **1. Negocio — Pedidos y Guias** — crear pedidos, generar PDF automatico (`POST /api/pedidos/{id}/generar-guia`)
- **2. Cloud — Ordenamiento EFS + S3** — listar bucket, consultar con filtros, descargar, PUT, DELETE (como el profesor)

---

## Cruce con actividad, pauta y apuntes

| Fuente | Requisito | Como se cumple en este proyecto |
|--------|-----------|----------------------------------|
| **actividad_S3.txt** | EFS temporal | `PedidoGuiaService` → `EfsService.saveBytes()` en `/app/efs/{fecha}/{transportista}/` |
| **actividad_S3.txt** | S3 por fecha/transportista | `AwsS3Service.uploadBytes()` con key `20250604/TransportesSur/guia-PED-001.pdf` |
| **actividad_S3.txt** | Crear guias de despacho | `POST /api/pedidos/{id}/generar-guia` genera PDF desde el pedido |
| **actividad_S3.txt** | Modificar, eliminar, consultar, descargar | Endpoints en `/s3/{bucket}/` (igual al profesor) |
| **actividad_S3.txt** | Sin validacion permisos descarga | No hay Spring Security (profesor: proximas clases) |
| **actividad_S3.txt** | Docker Hub + GitHub Actions EC2 | `deploy.yml` |
| **pauta 1** | EFS organizado | Misma key que S3; ver `ls` en EC2 y `docker exec` |
| **pauta 2** | S3 automatico ordenado | POST sube a bucket con prefijo fecha/transportista |
| **pauta 3** | Modificar en S3 | PUT actualiza mismo objeto |
| **pauta 4** | Descargar contenido | `GET /s3/{bucket}/object` devuelve bytes del PDF |
| **pauta 5** | Consultar con filtros | `GET /s3/{bucket}/consulta?fecha=&transportista=` filtra por prefijo |
| **pauta 6** | Pipeline CI/CD | Push a `main` dispara workflow |
| **pauta 7** | Video explicativo | Guion abajo + apuntes comandos EC2 |
| **apuntes** | `df -h`, `ls`, `docker exec` | Seccion 6 — carpeta `20250604/TransportesSur/` (no `pdfs/` del demo del profesor) |

**Nota:** El demo del profesor (`ms-administracion-archivos`) usa key `pdfs/testEFS1.pdf`. Tu actividad pide **fecha/transportista**, por eso en EFS veras carpetas `20250604/TransportesSur/` y no `pdfs/`.

---

## Orden para el VIDEO (apuntes + pauta)

### Carpeta 1 — Negocio
1. **POST** crear pedido 1 → **POST** generar guia (Pauta 1 y 2; PDF generado por el sistema)
2. **POST** crear pedido 2 → **POST** generar segunda guia (otra carpeta `20250605/TransportesNorte/`)
3. **EC2** — `df -h`, `ls -R /home/ec2-user/efs`, `docker exec` (apuntes 1-7)

### Carpeta 2 — Cloud
4. **GET list** — mostrar las 2 keys en el bucket
5. **GET consulta** — filtrar por fecha/transportista (`total: 1` en cada caso)
6. **GET download** — descargar PDF y abrirlo (Pauta 4)
7. **PUT** — modificar guia (Pauta 3)
8. **DELETE** — eliminar segunda guia
9. **Consola S3** — verificar cambios
10. **Git push** — pipeline en vivo (Pauta 6)
11. **Explicar con detalle** (Pauta 7)

---

## 1. Crear pedido y generar guia — Pauta 1 y 2

**Paso A — Crear pedido:**

```
POST http://<IP>:8080/api/pedidos
Content-Type: application/json
```

```json
{
  "cliente": "Maria Lopez",
  "direccion": "Av. Libertad 123, Santiago",
  "descripcion": "Caja mediana fragil",
  "transportista": "TransportesSur",
  "fecha": "20250604"
}
```

**Respuesta:** `201 Created` con `id: "PED-001"`.

**Paso B — Generar guia (sin subir PDF manual):**

```
POST http://<IP>:8080/api/pedidos/PED-001/generar-guia
```

**Respuesta:**

```json
{
  "key": "20250604/TransportesSur/guia-PED-001.pdf",
  "fecha": "20250604",
  "transportista": "TransportesSur",
  "nombreGuia": "guia-PED-001.pdf",
  "mensaje": "Guia generada y almacenada en EFS y S3"
}
```

**Verificar en EC2:**

```bash
ls -R /home/ec2-user/efs/
# 20250604/TransportesSur/guia-PED-001.pdf
```

**Verificar en consola AWS S3:** objeto con key `20250604/TransportesSur/guia-PED-001.pdf`

---

## 2. Listar todo el bucket (GET) — Carpeta 2, paso 1

```
GET http://<IP>:8080/s3/<BUCKET>/objects
```

**Respuesta esperada:** array con **todas** las keys del bucket (sin filtros):

```json
[
  {
    "key": "20250604/TransportesSur/guia-PED-001.pdf",
    "size": 1234,
    "lastModified": "..."
  },
  {
    "key": "20250605/TransportesNorte/guia-PED-002.pdf",
    "size": 1180,
    "lastModified": "..."
  }
]
```

Diferencia con consulta: **list** = inventario completo; **consulta** = filtrado por fecha/transportista con campo `total`.

---

## 3. Consultar guias (GET) — Pauta 5

```
GET http://<IP>:8080/s3/<BUCKET>/consulta?fecha=20250604&transportista=TransportesSur
```

**Respuesta esperada:**

```json
{
  "total": 1,
  "fecha": "20250604",
  "transportista": "TransportesSur",
  "guias": [
    {
      "key": "20250604/TransportesSur/guia-PED-001.pdf",
      "size": 1234,
      "lastModified": "..."
    }
  ]
}
```

Con `fecha=20250605&transportista=TransportesNorte` solo aparece la segunda guia (`total: 1`).

Importante: esto **cuenta y lista metadatos**. No descarga el PDF.

---

## 4. Descargar guia (GET) — Pauta 4

```
GET http://<IP>:8080/s3/<BUCKET>/object?fecha=20250604&transportista=TransportesSur&nombreGuia=guia-PED-001
```

**Respuesta esperada:** archivo PDF binario (Save Response → guardar y abrir el PDF).

No basta con listar; debes **obtener el contenido** del archivo.

---

## 5. Modificar guia (PUT) — Pauta 3

```
PUT http://<IP>:8080/s3/<BUCKET>/object
Content-Type: multipart/form-data
```

| Campo | Valor ejemplo |
|-------|---------------|
| file | PDF (puedes usar el descargado en paso 4) |
| fecha | 20250604 |
| transportista | TransportesSur |
| nombreGuia | guia-PED-001 |

**Verificar:** en consola S3, el archivo mantiene la misma key pero cambia tamano/fecha de modificacion.

---

## 6. Eliminar guia (DELETE)

```
DELETE http://<IP>:8080/s3/<BUCKET>/object?fecha=20250605&transportista=TransportesNorte&nombreGuia=guia-PED-002
```

Tambien funciona con `key` directa:

```
DELETE http://<IP>:8080/s3/<BUCKET>/object?key=20250605/TransportesNorte/guia-PED-002.pdf
```

**Respuesta esperada:** `204 No Content`

**Nota:** elimina solo de **S3** (igual que el profesor). El archivo puede seguir en EFS.

**Verificar:** el objeto ya no aparece en la consola S3.

---

## 7. Demostracion EFS en video — Pauta 1 maximo (apuntes clase)

En EC2, tras generar las 2 guias (carpeta 1):

```bash
df -h
ls -R /home/ec2-user/efs/
# Esperado:
# 20250604/TransportesSur/guia-PED-001.pdf
# 20250605/TransportesNorte/guia-PED-002.pdf

sudo docker exec -it empresa-transportista-efs bash
ls -R /app/efs/
exit
```

Explicar la cadena (apuntes): micro escribe en `/app/efs` → Docker mapea a `/home/ec2-user/efs` en Linux → Linux escribe en Amazon EFS. Misma organizacion que las keys en S3.

---

## 8. Pipeline CI/CD en vivo — Pauta 6

1. Hacer un cambio menor (ej. comentario en README)
2. `git add . && git commit -m "trigger deploy" && git push origin main`
3. Mostrar GitHub Actions ejecutandose
4. Verificar que la app responde en EC2 despues del deploy

---

## 9. Guion del video — Pauta 7

Orden sugerido:

1. Presentar el caso (empresa transportista, pedidos y guias de despacho)
2. Mostrar arquitectura: dos capas (negocio + cloud), EC2 + Docker + EFS + S3 + GitHub Actions
3. **Carpeta 1:** crear pedidos y generar guias (PDF automatico, sin subir archivo)
4. Mostrar EFS en EC2 (`ls -R`) — dos carpetas fecha/transportista
5. Mostrar objetos en consola S3 (mismas keys)
6. **Carpeta 2:** listar bucket completo vs consulta con filtros
7. Descargar PDF y abrirlo
8. Modificar guia (PUT) y ver cambio en S3
9. Eliminar segunda guia y verificar en S3
10. Push a main y pipeline en vivo
11. Explicar **por que** funciona cada parte (no solo mostrar pantallas)

---

## Pruebas solo en EC2 (sin entorno local)

Todas las pruebas se hacen contra la instancia desplegada:

```
http://<IP-ELASTICA-EC2>:8080/api/pedidos      ← negocio
http://<IP-ELASTICA-EC2>:8080/s3/<BUCKET>/     ← cloud
```

Orden recomendado:

1. Seguir [AWS_SETUP.md](AWS_SETUP.md) (S3, EFS, EC2, IAM, Docker)
2. Desplegar el contenedor con volumen EFS montado (push a `main` o manual)
3. Ejecutar **Carpeta 1** de Postman completa
4. Ejecutar **Carpeta 2** de Postman completa
5. Verificar EFS en SSH (`ls -R /home/ec2-user/efs`, `docker exec`, `df -h`)
6. Verificar objetos en la consola AWS S3
7. Grabar el video con ese flujo en vivo

---

## Variables de la coleccion Postman

| Variable | Valor ejemplo | Uso |
|----------|---------------|-----|
| `ec2_host` | `52.45.88.121` | IP elastica de EC2 |
| `bucket` | `tu-bucket-guias` | Nombre del bucket S3 (igual que `AWS_S3_BUCKET`) |
| `pedido_id_1` | `PED-001` | Se guarda al crear pedido 1 |
| `pedido_id_2` | `PED-002` | Se guarda al crear pedido 2 |
| `fecha` | `20250604` | Pedido 1 / consulta cloud |
| `transportista` | `TransportesSur` | Pedido 1 |
| `nombreGuia` | `guia-PED-001` | Sin .pdf |
| `fecha2` | `20250605` | Pedido 2 |
| `transportista2` | `TransportesNorte` | Pedido 2 |
| `nombreGuia2` | `guia-PED-002` | Sin .pdf |

El bucket va en la URL como `{bucket}` (igual al profesor). Debe coincidir con `AWS_S3_BUCKET` en EC2.
