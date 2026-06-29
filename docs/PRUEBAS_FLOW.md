# Guía rápida — Flujo de pruebas Postman (Semana 6)

Guía corta para ejecutar las pruebas más tarde. Detalle completo: [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md).

**Colección:** `postman/Pruebas-Semana6.postman_collection.json`  
**Environment:** `Semana 6` (local — ver `postman/Semana6.postman_environment.example.json`)

---

## Usuarios y roles (dos cuentas distintas)

| Usuario Azure | Email | Rol en token | Postman |
|---------------|-------|--------------|---------|
| Usuario 1 | `lisbethbilbao1@gmail.com` | `GESTOR_GUIAS` | **0.1** + **Carpeta 1** |
| Usuario 2 | `lisbeth.bilbao.merino@gmail.com` | `LECTOR_GUIAS` | **0.2** + **Carpeta 2** |

Los roles ya vienen en el JWT (`extension_UserRole`) desde el registro en B2C. **No hace falta** Graph API ni re-registrar usuarios si los tokens muestran el rol correcto.

---

## Orden de ejecución (obligatorio)

```
0.1 GESTOR  →  Carpeta 1 (crear + generar guía)  →  0.2 LECTOR  →  Carpeta 2
```

### Paso a paso

| # | Acción | Resultado esperado |
|---|--------|-------------------|
| 1 | Seleccionar environment **Semana 6** | — |
| 2 | **0.1** → Get New Access Token → login **gestor** → **Access Token** → Use Token → Send | Tests verde: `GESTOR_GUIAS` · **200** |
| 3 | **0.0 C** (opcional) | Consola: `extension_UserRole: GESTOR_GUIAS` |
| 4 | **Carpeta 1** → `1. POST Crear pedido 1` | **201** |
| 5 | **Carpeta 1** → `2. POST Generar guía pedido 1` | **201** · setea `s3_key_pedido_1` |
| 6 | (Opcional) **Carpeta 1** → GET Consulta o Descargar guía | **200** |
| 7 | Borrar tokens / **ventana privada** | — |
| 8 | **0.2** → Get New Access Token → login **lector** → **Access Token** → Use Token → Send | Tests verde: `LECTOR_GUIAS` · **200** (`{{b2c_openid_config_url}}`) |
| 9 | **Carpeta 2** → GET descargar | **200** PDF |
| 10 | **Carpeta 2** → POST crear pedido | **403** |
| 11 | **Carpeta 2** → DELETE eliminar guía | **403** |
| 12 | **Carpeta 3** (sin token) | **401** |

---

## Roles de cada request (0.2 vs Carpeta 2)

| Request | Qué hace | ¿Necesita PDF en S3? |
|---------|----------|---------------------|
| **0.1** GESTOR | `GET /api/pedidos` vía Gateway | **No** |
| **0.2** LECTOR | `{{b2c_openid_config_url}}` (solo OAuth, **200**) | **No** |
| **Carpeta 2 GET** | `GET /s3/.../object` — descargar guía | **Sí** |

**404 Object Not Found** en **Carpeta 2 GET** (no en 0.2) **no es error de rol**. Significa:

- Token LECTOR válido
- Spring autorizó la descarga
- **Falta el archivo en S3** → ejecuta **Carpeta 1** pasos 4 y 5 antes de **Carpeta 2 GET**

### 404 por `nombreGuia` desincronizado (Cloud pasos 6–7)

Si el 404 menciona `guia-PED-001-actualizado-actualizado.pdf` (u otro nombre que no está en S3):

1. **6. PUT Actualizar guía** acumulaba `-actualizado` en `nombreGuia`
2. **7. POST Mover objeto** movía el PDF a `guia-movida-demo.pdf` sin actualizar variables

| Variable | Fuente de verdad |
|----------|------------------|
| `s3_key_pedido_1` | **2. POST Generar guía**; PUT/move la actualizan |
| `nombreGuia` sola | Puede quedar obsoleta tras Cloud 6–7 |

**Carpeta 2 GET** sincroniza `nombreGuia` desde `s3_key_pedido_1` antes de Send (reimporta colección).

**Workaround:** resetear `nombreGuia` al PDF que existe en S3 (`guia-PED-001` o `guia-movida-demo`) o re-ejecutar **2. POST Generar guía**.

### Códigos HTTP — qué significan

| Código | Significado | Qué hacer |
|--------|-------------|-----------|
| **401** / `Unauthorized` | Token o Gateway | OAuth · revisar Authorizer |
| **403** | Rol incorrecto | LECTOR en POST/DELETE debe dar 403; si da 201 usaste token GESTOR en 0.2 |
| **404** Object Not Found | Guía no está en S3 | **Carpeta 2 GET** — ejecutar Carpeta 1 antes |

---

## 401 en 0.1 (login OAuth OK, pero el request da 401)

**Síntoma:** OAuth abre, login Azure funciona y se obtiene Access Token, pero **0.1 Send** devuelve `401`.

**Clave: identificar quién emite el 401** (revisar el header `WWW-Authenticate` de la respuesta):

| Origen | Señal en la respuesta 401 |
|--------|---------------------------|
| **API Gateway** (JWT Authorizer `azureidaas`) | body `{"message":"Unauthorized"}` (JSON) |
| **Spring Boot** (Resource Server en EC2) | header `WWW-Authenticate: Bearer error="invalid_token", error_description="...decode the Jwt..."` y body vacío |

**Causa real de este proyecto:** el `iss` del token B2C es la forma **tfp**:

`https://empresatransportistaefs.b2clogin.com/tfp/972f25cf-cd03-4c70-84ea-285778b48398/b2c_1_cdy2204-1/v2.0/`

El **Authorizer del Gateway** ya estaba en `tfp` (aceptaba el token), pero el **Spring desplegado** tenía `issuer-uri` **sin `tfp`** (`.../972f25cf-.../v2.0/`), así que Spring rechazaba el JWT. El `iss` debe ser **idéntico** (`tfp`, `b2c_1` en minúscula) en los **tres** lugares:

| Lugar | Valor |
|-------|-------|
| Claim `iss` del token (0.0 C) | `.../tfp/972f25cf-.../b2c_1_cdy2204-1/v2.0/` |
| `application.properties` → `spring.security.oauth2.resourceserver.jwt.issuer-uri` | igual |
| AWS Authorizer `azureidaas` → Issuer | igual |

**Diagnóstico con 0.0 A / 0.0 B:**

- **0.0 A** EC2 **401** (header Spring `decode the Jwt`) → corregir `issuer-uri` en `application.properties` (a `tfp`) y **redeployar** la imagen.
- **0.0 A** EC2 **200** + **0.0 B** Gateway **401** `{"message":"Unauthorized"}` → corregir Issuer/Audience del Authorizer en AWS y **Deploy DEV**.

El environment **Semana 6** ya tiene `b2c_issuer_canonical` y `b2c_issuer_gateway` en forma `tfp` (`b2c_1` minúscula).

---

## Variables que debe dejar Carpeta 1

Tras `2. POST Generar guía pedido 1` (**201**):

| Variable | Valor esperado |
|----------|----------------|
| `fecha` | `20250604` |
| `transportista` | `TransportesSur` |
| `nombreGuia` | `guia-PED-001` |
| `s3_key_pedido_1` | `20250604/TransportesSur/guia-PED-001.pdf` |

**Fuente de verdad para Carpeta 2 GET:** `s3_key_pedido_1` (PUT paso 6 y move paso 7 la actualizan).

Comprobar guía en S3 (con token GESTOR):

```
GET {{api_gateway_url}}/s3/{{bucket}}/consulta?fecha=20250604&transportista=TransportesSur
```

---

## Validar UserRole en Postman

En pestaña **Tests** después de Send:

| Request | Test automático |
|---------|-----------------|
| **0.1** | `Access Token con extension_UserRole GESTOR` |
| **0.2** | `Access Token LECTOR_GUIAS` |

Si el test de rol pasa **en verde** en **0.2** pero **Carpeta 2 GET** da **404**, el rol está bien — falta generar la guía (Carpeta 1).

---

## Forzar login LECTOR (Postman no pide contraseña)

Postman reutiliza sesión del GESTOR en su navegador integrado. Para obligar login del Usuario 2:

### A) En Postman (hazlo una vez)

1. **Settings** (engranaje) → **General**
2. Desactiva **"Authorize using browser"** / usa navegador del sistema (no el popup embebido de Postman)
3. En versiones recientes: desactiva OAuth con ventana interna si aparece esa opción

### B) Antes de 0.2 (cada vez)

1. **0.2** → Authorization → **Manage Tokens** → borrar `lector-access-token` y `gestor-access-token`
2. Environment → vaciar `access_token_lector` y `access_token_gestor`
3. **Get New Access Token** (no solo Send)

La request **0.2** usa las **mismas variables OAuth que 0.1** (`b2c_auth_url`, `b2c_token_url`, etc.). Solo cambia el nombre del token guardado: `lector-access-token`.

4. Login: `lisbeth.bilbao.merino@gmail.com`
5. **Use Token Type = Access Token** → **Use Token** → **Send**
6. Test verde: `LECTOR_GUIAS` (si dice `GESTOR_GUIAS`, repite desde paso 1)

### Si Get New Access Token no abre en 0.2

Comparar con **0.1** (que sí funciona):

| Campo Authorization | Debe ser |
|---------------------|----------|
| Type | **OAuth 2.0** (no Bearer / no Inherit) |
| Auth URL | `{{b2c_auth_url}}` con valor (no vacío) |
| Token URL | `{{b2c_token_url}}` |
| Client ID | `{{b2c_client_id}}` |

Si **Auth URL** está vacío → reimporta `postman/Semana6.postman_environment.json` o completa `b2c_auth_url` en environment **Semana 6**.

---

### C) Si aún no pide login (sesión GESTOR)

Abre en Chrome/Edge **incógnito** esta URL y cierra sesión B2C, luego vuelve a **Get New Access Token**:

```
https://empresatransportistaefs.b2clogin.com/empresatransportistaefs.onmicrosoft.com/B2C_1_cdy2204-1/oauth2/v2.0/logout
```

O borra cookies de `b2clogin.com` en el navegador que use Postman para OAuth.

---

## Errores frecuentes

| Problema | Solución |
|----------|----------|
| **Carpeta 2 GET** da 404 | Ejecutar Carpeta 1 pasos 1 y 2; reimportar colección (sync `s3_key_pedido_1`); o resetear `nombreGuia` al PDF que existe en S3 |
| **404** con `-actualizado-actualizado` | Pasos Cloud 6–7 desincronizaron variables — usar `s3_key_pedido_1` o regenerar guía |
| POST Carpeta 2 da **201** (no 403) | Token de GESTOR en 0.2 → ventana privada + login lector |
| Access token grisado en Postman | Azure no devolvió `access_token` → [GUIA_AZURE.md](GUIA_AZURE.md) |
| `s3_key_pedido_1 vacio` (pre-request Carpeta 2) | Carpeta 1 no ejecutada — la colección avisa antes de Send |
| **generar-guia** da 403 IAM | Rol EC2 para S3 — ver [AWS_GATEWAY_SETUP.md](docs/AWS_GATEWAY_SETUP.md) |

---

## Checklist express (antes de grabar video)

- [ ] Environment **Semana 6** activo
- [ ] **0.1** → Tests `GESTOR_GUIAS` OK
- [ ] Carpeta 1 → requests **1** y **2** → **201**
- [ ] `s3_key_pedido_1` tiene valor en environment
- [ ] **0.2** → Tests `LECTOR_GUIAS` OK + **200** (no uses `/api/pedidos` en 0.2)
- [ ] Carpeta 2 → GET **200** PDF · POST **403** · DELETE **403**

---

## Referencias

- [POSTMAN_PRUEBAS.md](POSTMAN_PRUEBAS.md) — guía completa
- [docs/GUIA_AZURE.md](docs/GUIA_AZURE.md) — Azure B2C y Access Token
- [docs/UPDATE_USERS.md](docs/UPDATE_USERS.md) — usuarios B2C (Graph no necesario si roles OK)
