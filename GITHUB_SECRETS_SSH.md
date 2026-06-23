# Como configurar EC2_SSH_KEY en GitHub (IMPORTANTE)

## El problema que tienes

El error `Permission denied (publickey)` significa que la clave en GitHub **no coincide** con la instancia EC2.

Tu instancia `dsy2204-1` usa el archivo:

```
C:\Users\lisbe\Downloads\dsy2204-1.pem
```

**NO uses** `labsuser.pem` — esa es otra clave y no abre tu EC2.

## Verificar en tu PC (obligatorio)

```powershell
ssh -i "C:\Users\lisbe\Downloads\dsy2204-1.pem" ec2-user@52.45.88.121
```

Si entras, esa es la clave correcta para GitHub.

## Fingerprints (para ver en GitHub Actions)

| Archivo | Fingerprint |
|---------|-------------|
| **dsy2204-1.pem** (CORRECTO) | `SHA256:LMpmI/thDtRMsbFMMYywOeJQuOh1Om2Tm9jZditomF4` |
| labsuser.pem (INCORRECTO) | `SHA256:xKNW22BVngda+uCxv2Dt3N4iH26A8l1eTlWPVVr3dj8` |

## Pasos en GitHub

1. Repo → **Settings** → **Secrets and variables** → **Actions**
2. **Eliminar** el secret `EC2_SSH_KEY`
3. Abrir `dsy2204-1.pem` con **Notepad**
4. Copiar TODO desde `-----BEGIN RSA PRIVATE KEY-----` hasta `-----END RSA PRIVATE KEY-----`
5. **New repository secret** → Name: `EC2_SSH_KEY` → pegar → Save
6. **Actions** → **Re-run all jobs**

## Otros secrets SSH

| Secret | Valor |
|--------|-------|
| `EC2_HOST` | `52.45.88.121` |
| `USER_SERVER` | `ec2-user` |

## El workflow NO es el problema

Maven, Docker y AWS ya pasan. Solo falla SSH porque el secret tiene la clave equivocada.
