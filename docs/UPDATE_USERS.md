# Actualizar roles de usuario B2C — Microsoft Graph

Guía para asignar `extension_UserRole` (`GESTOR_GUIAS` / `LECTOR_GUIAS`) cuando el portal Azure **no muestra** el campo **UserRole** al editar usuarios.

**Herramienta:** [Microsoft Graph Explorer](https://developer.microsoft.com/graph/graph-explorer)  
**Relacionado:** [GUIA_AZURE.md](GUIA_AZURE.md) §4.7 · [POSTMAN_PRUEBAS.md](../POSTMAN_PRUEBAS.md)

---

## ⚠ Si PATCH da 404 con URL y body correctos

**El PATCH está bien.** El error `Request_ResourceNotFound` significa: Graph busca el usuario en el **tenant donde iniciaste sesión**, y ahí **no existe** `e37ed2c8-...`.

| Lo que ves en portal Azure | Lo que usa Graph Explorer |
|----------------------------|---------------------------|
| Puedes abrir **Azure AD B2C** y ver usuarios | Usa el token del tenant de tu login (`@duocuc.cl` → **Duoc UC**) |
| Ves `b2c-extensions-app` en App registrations B2C | Graph consulta **Duoc UC**, no **EmpresaTransportistaEFS** |

**Prueba obligatoria** (ejecuta ANTES de cualquier PATCH):

```
GET https://graph.microsoft.com/v1.0/organization?$select=displayName,id
```

| Respuesta `displayName` | Conclusión |
|---------------------------|------------|
| **EmpresaTransportistaEFS** | Puedes usar Graph PATCH (§3) |
| **Fundacion Instituto Profesional Duoc UC** (u otro) | **Graph PATCH no funcionará** → usa **§6 Run user flow** (recomendado) |

**Cuenta `@duocuc.cl`:** suele poder **ver** el tenant B2C en portal, pero **no** es miembro admin del directorio B2C → Graph Explorer no puede modificar usuarios B2C.

**Solución más simple para la actividad:** **§6 Run user flow** (sin Graph).

---

## Valores del proyecto (copiar tal cual)

| Campo | Valor |
|-------|-------|
| Tenant B2C | **EmpresaTransportistaEFS** |
| Tenant ID | `972f25cf-cd03-4c70-84ea-285778b48398` |
| Dominio B2C | `empresatransportistaefs.onmicrosoft.com` |
| App OAuth Postman | `empresa-transportista-efs` → `49f4ab51-5e0e-4139-9cf4-15f566581b07` |
| App extensiones B2C | `b2c-extensions-app` → `5047db47-431a-46ad-af61-04ae3811d605` |
| App extensiones (sin guiones) | `5047db47431a46adaf6104ae3811d605` |
| Claim en JWT | `extension_UserRole` |
| Campo en Graph PATCH/GET | `extension_5047db47431a46adaf6104ae3811d605_UserRole` |

| Rol | Display name | Email | Object ID | UPN (referencia) |
|-----|--------------|-------|-----------|------------------|
| GESTOR (Usuario 1) | Usuario 1 | `lisbethbilbao1@gmail.com` | `eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975` | `eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975@EmpresaTransportistaEFS.onmicrosoft.com` |
| LECTOR (Usuario 2) | Usuario 2 | `lisbeth.bilbao.merino@gmail.com` | `e37ed2c8-025e-43c5-bde7-8f14dd04ffb1` | `e37ed2c8-025e-43c5-bde7-8f14dd04ffb1@EmpresaTransportistaEFS.onmicrosoft.com` |

**Validado en portal Azure AD B2C** (Users → Información básica). Los Object ID de la guía **coinciden** con Usuario 1 y Usuario 2.

Si el PATCH Graph da **404**, los IDs **no están mal** — Graph Explorer consulta otro tenant (Duoc UC). Ver §1 y §6.

## 1. Tenant correcto (obligatorio)

El GET que devuelve alumnos Duoc UC (`179151405 EDUARDO SERGIO…`, `206266082 Juan Pablo…`) indica que Graph Explorer está en **Fundacion Instituto Profesional Duoc UC**, **no** en tu B2C.

| Tenant | ¿Usuarios B2C? |
|--------|----------------|
| Fundacion Instituto Profesional Duoc UC | **No** — alumnos DUOC |
| **EmpresaTransportistaEFS** | **Sí** — emails `@gmail.com` del proyecto |

### Cambiar tenant en Graph Explorer

1. Icono de **perfil** (arriba derecha) → **Change tenant** / **Cambiar directorio**.
2. Selecciona **EmpresaTransportistaEFS** (`empresatransportistaefs.onmicrosoft.com`).
3. Si no aparece: cierra sesión e inicia con la cuenta admin del tenant B2C.

### Comprobar tenant (ANTES del PATCH)

**Paso A — ¿Qué tenant usa Graph?**

```
GET https://graph.microsoft.com/v1.0/organization?$select=displayName,id
```

Respuesta esperada:

```json
{
  "value": [
    {
      "displayName": "EmpresaTransportistaEFS",
      "id": "972f25cf-cd03-4c70-84ea-285778b48398"
    }
  ]
}
```

**Paso B — ¿Existen usuarios B2C?**

```
GET https://graph.microsoft.com/v1.0/users?$top=10&$select=id,displayName,identities
```

Debe listar **Usuario 1**, **Usuario 2**, o `@gmail.com`. Si ves alumnos DUOC → tenant incorrecto → PATCH dará **404**.

**Paso C — Object ID por email (lector)**

```
GET https://graph.microsoft.com/v1.0/users?$filter=identities/any(c:c/issuerAssignedId eq 'lisbeth.bilbao.merino@gmail.com')&$select=id,displayName,identities
```

**Paso C — Object ID por email (gestor)**

```
GET https://graph.microsoft.com/v1.0/users?$filter=identities/any(c:c/issuerAssignedId eq 'lisbethbilbao1@gmail.com')&$select=id,displayName,identities
```

---

## 2. GET — Buscar usuarios B2C

Permiso: **User.Read.All**

### 2.1 Listar usuarios

```
GET https://graph.microsoft.com/v1.0/users?$top=10&$select=id,displayName,identities
```

### 2.2 Buscar lector por email

```
GET https://graph.microsoft.com/v1.0/users?$filter=identities/any(c:c/issuerAssignedId eq 'lisbeth.bilbao.merino@gmail.com')&$select=id,displayName,identities
```

### 2.3 Buscar gestor por email

```
GET https://graph.microsoft.com/v1.0/users?$filter=identities/any(c:c/issuerAssignedId eq 'lisbethbilbao1@gmail.com')&$select=id,displayName,identities
```

### 2.4 LECTOR por Object ID

```
GET https://graph.microsoft.com/v1.0/users/e37ed2c8-025e-43c5-bde7-8f14dd04ffb1?$select=id,displayName,identities
```

### 2.5 GESTOR por Object ID

```
GET https://graph.microsoft.com/v1.0/users/eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975?$select=id,displayName,identities
```

| Resultado | Acción |
|-----------|--------|
| **200** con datos del usuario | Tenant correcto → continúa con PATCH |
| **404** `Request_ResourceNotFound` | Tenant incorrecto (Duoc UC) — cambia tenant §1 |

---

## 3. PATCH — Asignar rol (UserRole)

Permiso: **User.ReadWrite.All** (Modify permissions → Consent)

- Método: **PATCH**
- Body en pestaña **Request body**
- Tenant activo: **EmpresaTransportistaEFS** (`972f25cf-cd03-4c70-84ea-285778b48398`)

### 3.1 LECTOR — `lisbeth.bilbao.merino@gmail.com`

**URL:**

```
PATCH https://graph.microsoft.com/v1.0/users/e37ed2c8-025e-43c5-bde7-8f14dd04ffb1
```

**Request body:**

```json
{
  "extension_5047db47431a46adaf6104ae3811d605_UserRole": "LECTOR_GUIAS"
}
```

### 3.2 GESTOR — `lisbethbilbao1@gmail.com`

**URL:**

```
PATCH https://graph.microsoft.com/v1.0/users/eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975
```

**Request body:**

```json
{
  "extension_5047db47431a46adaf6104ae3811d605_UserRole": "GESTOR_GUIAS"
}
```

### Respuesta esperada

| Código | Significado |
|--------|-------------|
| **204 No Content** | Rol actualizado correctamente |
| **404** | Tenant incorrecto (Duoc UC) o usuario inexistente |
| **400 Empty Payload** | Body vacío en **Request body** |
| **400** propiedad desconocida | Usaste Client ID `49f4ab51...` en lugar de `5047db47...` |

---

## 4. GET — Verificar rol guardado

Permiso: **User.Read.All**

### 4.1 LECTOR

```
GET https://graph.microsoft.com/v1.0/users/e37ed2c8-025e-43c5-bde7-8f14dd04ffb1?$select=id,displayName,extension_5047db47431a46adaf6104ae3811d605_UserRole
```

Respuesta esperada:

```json
{
  "id": "e37ed2c8-025e-43c5-bde7-8f14dd04ffb1",
  "displayName": "Usuario 2",
  "extension_5047db47431a46adaf6104ae3811d605_UserRole": "LECTOR_GUIAS"
}
```

### 4.2 GESTOR

```
GET https://graph.microsoft.com/v1.0/users/eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975?$select=id,displayName,extension_5047db47431a46adaf6104ae3811d605_UserRole
```

Respuesta esperada:

```json
{
  "id": "eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975",
  "displayName": "Usuario 1",
  "extension_5047db47431a46adaf6104ae3811d605_UserRole": "GESTOR_GUIAS"
}
```

---

## 5. Verificar en Postman

1. **Manage Tokens** → eliminar tokens viejos.
2. **0.1** → login `lisbethbilbao1@gmail.com` → **Use Token** → **Send**.
3. **0.0 C** → consola: `extension_UserRole: GESTOR_GUIAS`.
4. Ventana privada → **0.2** → login `lisbeth.bilbao.merino@gmail.com`.
5. **0.0 C** → consola: `extension_UserRole: LECTOR_GUIAS`.
6. **Carpeta 2** → GET **200**, POST **403**, DELETE **403**.

---

## 6. Alternativa recomendada — Run user flow (sin Graph)

Usa esto si Graph Explorer sigue en tenant **Duoc UC** o PATCH da **404**.

### 6.1 Borrar usuarios viejos (opcional, evita duplicados)

**Azure AD B2C** → **Users** → elimina usuarios `unknown` / viejos si vas a re-registrar.

### 6.2 Registrar GESTOR con rol

1. **Azure AD B2C** → **User flows** → **`B2C_1_cdy2204-1`**
2. Botón **Run user flow** / **Ejecutar flujo de usuario**
3. Application: **`empresa-transportista-efs`**
4. Reply URL: `https://jwt.ms` (o la que ofrezca el portal)
5. **Run user flow** → se abre navegador
6. **Sign up now** / **Registrarse ahora**
7. Email: `lisbethbilbao1@gmail.com`
8. Contraseña: (la de tu archivo de claves local)
9. Si el formulario pide **UserRole** → escribe: **`GESTOR_GUIAS`**
10. Completar registro

### 6.3 Registrar LECTOR con rol

Repite §6.2 con:

| Campo | Valor |
|-------|-------|
| Email | `lisbeth.bilbao.merino@gmail.com` |
| UserRole | **`LECTOR_GUIAS` |

Usa ventana privada o cierra sesión B2C entre gestor y lector.

### 6.4 Si el formulario NO pide UserRole al registrarse

1. Confirma **User attributes** del flow: **UserRole** marcado (ya lo tienes).
2. Prueba **Edit profile** flow o vuelve a registrar con otro email de prueba.
3. Como último recurso: inicia sesión en Graph con la **cuenta Microsoft personal** que **creó** el tenant B2C (owner de la suscripción Azure), no `@duocuc.cl`.

### 6.5 Verificar en Postman

1. **0.1** → `lisbethbilbao1@gmail.com` → **0.0 C** → `extension_UserRole: GESTOR_GUIAS`
2. Ventana privada → **0.2** → `lisbeth.bilbao.merino@gmail.com` → **0.0 C** → `extension_UserRole: LECTOR_GUIAS`
3. **Carpeta 2** → GET **200**, POST **403**, DELETE **403**

---

## 7. Alternativa PowerShell (tenant B2C explícito)

Solo si tienes cuenta **admin del tenant B2C** (no solo Duoc UC).

```powershell
Install-Module Microsoft.Graph.Users -Scope CurrentUser
Connect-MgGraph -TenantId 972f25cf-cd03-4c70-84ea-285778b48398 -Scopes "User.ReadWrite.All"

# LECTOR
Update-MgUser -UserId e37ed2c8-025e-43c5-bde7-8f14dd04ffb1 -AdditionalProperties @{
  "extension_5047db47431a46adaf6104ae3811d605_UserRole" = "LECTOR_GUIAS"
}

# GESTOR
Update-MgUser -UserId eaa2d4a9-c5a7-46e6-a4eb-f6e9554d4975 -AdditionalProperties @{
  "extension_5047db47431a46adaf6104ae3811d605_UserRole" = "GESTOR_GUIAS"
}

# Verificar lector
Get-MgUser -UserId e37ed2c8-025e-43c5-bde7-8f14dd04ffb1 -Property "displayName,extension_5047db47431a46adaf6104ae3811d605_UserRole"
```

Si `Connect-MgGraph` falla o PATCH da 404 → usa **§6 Run user flow**.

---

## 8. Errores frecuentes

| Error | Causa | Solución |
|-------|-------|----------|
| GET lista alumnos DUOC | Tenant Duoc UC | GET `https://graph.microsoft.com/v1.0/organization?$select=displayName,id` → debe ser **EmpresaTransportistaEFS** |
| `Request_ResourceNotFound` en PATCH | Graph en tenant **Duoc UC** (cuenta `@duocuc.cl`) | **§6 Run user flow** — no insistir con Graph |
| `Empty Payload` | PATCH sin body | Pestaña **Request body** con JSON §3 |
| Claim vacío en token | Application claims sin UserRole | [GUIA_AZURE.md](GUIA_AZURE.md) §4.7 |
| POST Carpeta 2 da 201 | Token GESTOR en 0.2 | Login `lisbeth.bilbao.merino@gmail.com` en ventana privada |
