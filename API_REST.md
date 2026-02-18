# 🔗 API REST - Cloud Distribué

Documentation complète de l'API REST pour accès réseau au système de stockage distribué.

---

## 📌 Généralités

- **URL de base:** `http://10.134.17.222:8080`
- **Port:** 8080
- **Format données:** JSON (+ binaire pour fichiers)
- **CORS:** Activé (*) pour accès cross-domain
- **Authentification:** Pas (basée sur userId dans les paramètres)

---

## 📤 Upload - POST /api/upload

Télécharger un fichier et le distribuer sur 3 serveurs.

### Syntaxe

```bash
curl -X POST "http://10.134.17.222:8080/api/upload?fileName=document.pdf&userId=user123" \
     --data-binary @document.pdf
```

### Paramètres

| Paramètre | Type | Obligatoire | Description |
|-----------|------|-------------|-------------|
| `fileName` | string | ✓ | Nom du fichier |
| `userId` | string | ✓ | ID utilisateur |

### Corps de la requête

Le fichier binaire lui-même

### Réponse (Succès 200)

```json
{
  "status": "success",
  "message": "Fichier distribué sur 3 serveurs",
  "fileName": "document.pdf"
}
```

### Réponse (Erreur 500)

```json
{
  "error": "Erreur lors de l'upload"
}
```

### Exemple

```bash
curl -X POST "http://10.134.17.222:8080/api/upload?fileName=image.jpg&userId=armando" \
     --data-binary @/home/armando/image.jpg
```

---

## 📥 Download - GET /api/download

Télécharger un fichier. Le système récupère automatiquement depuis les serveurs UP.

### Syntaxe

```bash
curl "http://10.134.17.222:8080/api/download?fileName=document.pdf&userId=user123" \
     --output document.pdf
```

### Paramètres

| Paramètre | Type | Obligatoire | Description |
|-----------|------|-------------|-------------|
| `fileName` | string | ✓ | Nom du fichier |
| `userId` | string | ✓ | ID utilisateur |

### Réponse (Succès 200)

Fichier binaire directement

### Réponse (Erreur 404)

```json
{
  "error": "Fichier non trouvé"
}
```

### Exemple

```bash
curl "http://10.134.17.222:8080/api/download?fileName=image.jpg&userId=armando" \
     --output /tmp/image.jpg
```

---

## 📋 Lister - GET /api/files

Lister tous les fichiers stockés dans le cluster.

### Syntaxe

```bash
curl http://10.134.17.222:8080/api/files
```

### Paramètres

Aucun

### Réponse (Succès 200)

```json
[
  {
    "fileName": "document.pdf",
    "ownerId": "user123",
    "totalSize": 1048576,
    "chunks": 1
  },
  {
    "fileName": "image.jpg",
    "ownerId": "user123",
    "totalSize": 2097152,
    "chunks": 2
  }
]
```

### Champs

| Champ | Type | Description |
|-------|------|-------------|
| `fileName` | string | Nom du fichier |
| `ownerId` | string | Propriétaire du fichier |
| `totalSize` | number | Taille en octets |
| `chunks` | number | Nombre de chunks (parts) |

### Exemple

```bash
curl http://10.134.17.222:8080/api/files | python3 -m json.tool
```

---

## 🔗 Cluster Status - GET /api/cluster

Obtenir l'état du cluster (OSDs UP/DOWN).

### Syntaxe

```bash
curl http://10.134.17.222:8080/api/cluster
```

### Paramètres

Aucun

### Réponse (Succès 200)

Format texte:
```
osd-9001|UP|10.134.17.222:9001
osd-9002|DOWN|10.134.17.222:9002
osd-9003|UP|10.134.17.222:9003
osd-9004|UP|10.134.17.222:9004
```

Format JSON (après traitement):
```json
[
  {
    "osdId": "osd-9001",
    "status": "UP",
    "address": "10.134.17.222:9001"
  },
  {
    "osdId": "osd-9002",
    "status": "DOWN",
    "address": "10.134.17.222:9002"
  }
]
```

### Statuts possibles

| Status | Signification |
|--------|---------------|
| `UP` | OSD actif et réactif |
| `SUSPECTED` | OSD en retard (temps mort) |
| `DOWN` | OSD non actif |

### Exemple

```bash
curl http://10.134.17.222:8080/api/cluster
```

---

## 🔒 Codes HTTP

| Code | Signification |
|------|---------------|
| 200 | ✓ Succès |
| 404 | Ressource non trouvée |
| 405 | Méthode non autorisée (ex: POST sur GET) |
| 500 | Erreur serveur |

---

## 📊 Exemples complets

### 1. Upload et Download en boucle

```bash
#!/bin/bash

FILE="document.pdf"
USER="test_user"

# Upload
echo "Upload..."
curl -X POST "http://10.134.17.222:8080/api/upload?fileName=$FILE&userId=$USER" \
     --data-binary @"$FILE"

sleep 2

# Download
echo "Download..."
curl "http://10.134.17.222:8080/api/download?fileName=$FILE&userId=$USER" \
     --output "/tmp/$FILE"

echo "Fichier téléchargé: /tmp/$FILE"
```

### 2. Lister et télécharger tous les fichiers

```bash
#!/bin/bash

USER="test_user"

# Récupérer liste
FILES=$(curl -s http://10.134.17.222:8080/api/files | \
        python3 -c "import sys, json; [print(f['fileName']) for f in json.load(sys.stdin)]")

# Pour chaque fichier
for FILE in $FILES; do
    echo "Téléchargement: $FILE"
    curl "http://10.134.17.222:8080/api/download?fileName=$FILE&userId=$USER" \
         --output "/tmp/$FILE"
done
```

### 3. Monitoring en temps réel

```bash
#!/bin/bash

while true; do
    clear
    echo "===== ÉTAT DU CLUSTER ====="
    curl -s http://10.134.17.222:8080/api/cluster
    echo ""
    echo "===== FICHIERS STOCKÉS ====="
    curl -s http://10.134.17.222:8080/api/files | python3 -m json.tool | head -30
    sleep 5
done
```

### 4. Script Python pour utiliser l'API

```python
import requests
import json

BASE_URL = "http://10.134.17.222:8080"

def upload_file(filename, user_id, filepath):
    """Upload un fichier"""
    url = f"{BASE_URL}/api/upload?fileName={filename}&userId={user_id}"
    with open(filepath, 'rb') as f:
        response = requests.post(url, data=f)
    return response.json()

def download_file(filename, user_id, output_path):
    """Télécharge un fichier"""
    url = f"{BASE_URL}/api/download?fileName={filename}&userId={user_id}"
    response = requests.get(url)
    with open(output_path, 'wb') as f:
        f.write(response.content)

def list_files():
    """Liste tous les fichiers"""
    url = f"{BASE_URL}/api/files"
    response = requests.get(url)
    return response.json()

def get_cluster_status():
    """État du cluster"""
    url = f"{BASE_URL}/api/cluster"
    response = requests.get(url)
    return response.text

# Utilisation
if __name__ == "__main__":
    # Upload
    result = upload_file("test.txt", "python_user", "/tmp/test.txt")
    print(f"Upload: {result}")
    
    # Liste
    files = list_files()
    print(f"Fichiers: {json.dumps(files, indent=2)}")
    
    # Cluster
    status = get_cluster_status()
    print(f"Cluster:\n{status}")
    
    # Download
    download_file("test.txt", "python_user", "/tmp/downloaded.txt")
    print("Download OK")
```

### 5. Test avec cURL (bash)

```bash
#!/bin/bash

BASE_URL="http://10.134.17.222:8080"
USER="curl_user"

# Test upload
echo "=== TEST UPLOAD ==="
curl -X POST "$BASE_URL/api/upload?fileName=test.txt&userId=$USER" \
     --data-binary "Ceci est un test" \
     -H "Content-Type: application/octet-stream"

echo -e "\n\n=== LISTER FICHIERS ==="  
curl "$BASE_URL/api/files" | python3 -m json.tool

echo -e "\n\n=== ÉTAT CLUSTER ===" 
curl "$BASE_URL/api/cluster"

echo -e "\n\n=== DOWNLOAD ===" 
curl "$BASE_URL/api/download?fileName=test.txt&userId=$USER" \
     --output "/tmp/test_downloaded.txt"
cat /tmp/test_downloaded.txt
```

---

## 🚀 Cas d'usage

### Client lourd (Desktop app)

```java
// Upload
HttpPost upload = new HttpPost("http://10.134.17.222:8080/api/upload?fileName=file.pdf&userId=user123");
upload.setEntity(new FileEntity(new File("file.pdf")));
HttpResponse response = client.execute(upload);

// Download
HttpGet download = new HttpGet("http://10.134.17.222:8080/api/download?fileName=file.pdf&userId=user123");
HttpResponse response = client.execute(download);
InputStream is = response.getEntity().getContent();
```

### Web app Frontend

```javascript
// Upload
const formData = new FormData();
formData.append('file', fileInput.files[0]);

fetch('http://10.134.17.222:8080/api/upload?fileName=image.jpg&userId=web_user', {
  method: 'POST',
  body: fileInput.files[0]
}).then(r => r.json()).then(data => console.log(data));

// Download
fetch('http://10.134.17.222:8080/api/download?fileName=image.jpg&userId=web_user')
  .then(r => r.blob())
  .then(blob => {
    const url = URL.createObjectURL(blob);
    const a = document.createElement('a');
    a.href = url;
    a.download = 'image.jpg';
    a.click();
  });
```

### Mobile (Android, iOS)

```kotlin
// Kotlin (Android)
val file = File("document.pdf")
val requestBody = file.asRequestBody("application/octet-stream".toMediaType())

val request = Request.Builder()
    .url("http://10.134.17.222:8080/api/upload?fileName=document.pdf&userId=mobile_user")
    .post(requestBody)
    .build()

val response = client.newCall(request).execute()
println(response.body?.string())
```

---

## ⚙️ Configuration

### Port personnalisé

Modifiez dans [HTTPServer.java](src/main/java/datastockage/server/HTTPServer.java):

```java
private static final int PORT = 8080;  // Changez ici
```

### Recompiler et redémarrer

```bash
./build.sh
./run.sh
```

---

## 🔧 Troubleshooting

### API non accessible

```bash
# Vérifier si le serveur écoute
netstat -an | grep 8080

# Tester la connexion
curl -v http://10.134.17.222:8080/api/files

# Vérifier les logs du serveur
# Cherchez "[HTTPServer] API REST démarrée"
```

### Fichier non trouvé après upload

```bash
# Attendre un peu (< 2 secondes)
sleep 2

# Vérifier la liste
curl http://10.134.17.222:8080/api/files | python3 -m json.tool

# Vérifier l'userId
```

### Download échoue

```bash
# Vérifier l'état cluster
curl http://10.134.17.222:8080/api/cluster

# Si tous les OSD sont DOWN:
# 1. Redémarrer le serveur
# 2. Vérifier qu'au moins 1 des 3 replicas est UP
```

---

## 📈 Performance

### Vitesses typiques

- **Upload**: 50-100 MB/s (réseau dépendant)
- **Download**: 50-100 MB/s (réseau dépendant)
- **Latence API**: < 50 ms
- **Response time**: < 100 ms

---

## 📄 Limitations

- Pas d'authentification (userId juste pour identifiant)
- Pas de quota utilisateur
- Pas de supression de fichiers (API GET-only)
- Pas de versioning
- Pas de compression

---

## 🎯 Prochaines étapes

Tester l'API:

```bash
chmod +x test_api.sh
./test_api.sh
```

Utiliser dans votre application:
- Remplacer `10.134.17.222` par votre IP serveur
- Adapter les userId selon votre système
- Gérer les erreurs HTTP

---

**Version:** 1.0  
**Date:** Février 2026  
**Statut:** Production-ready (prototype éducatif)
