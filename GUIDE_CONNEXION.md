# Guide de Connexion - Client Cloud Distribué

## Pour les autres ordinateurs du réseau

Les autres ordinateurs connectés au **même réseau** (192.168.1.x ou 10.134.17.x) peuvent accéder au cloud de stockage distribué de 3 façons:

---

## 1️⃣ Interface Web Simple (Recommandée)

**Le plus facile pour commencer!**

### Option A: Via Firefox ou Chrome

Ouvrir le navigateur et aller à:
```
http://10.134.17.222:8080
```

Une page d'accueil s'affiche avec:
- **Interface Web**: Drag-and-drop de fichiers, liste des fichiers
- **Documentation API**: Pour intégration programmée

### Option B: Télécharger le client standalone

1. Télécharger le fichier `client-standalone.html`
2. Ouvrir le fichier dans Firefox/Chrome 
3. Modifier l'adresse IP si nécessaire (menu "Serveur")
4. Upload/Download directement!

**Avantage**: Fonctionne aussi si le serveur change d'IP

---

## 2️⃣ Classe JavaScript Réutilisable

Pour intégrer dans **votre propre application web**.

### Installation

```html
<script src="http://10.134.17.222/client-example.js"></script>
```

Ou copier le fichier `client-example.js` localement.

### Utilisation Basic

```javascript
// Créer un client
const cloud = new CloudClient('http://10.134.17.222:8080');

// Upload un fichier
const file = document.querySelector('input[type="file"]').files[0];
await cloud.upload(file, 'mon-fichier.pdf');

// Lister les fichiers
const result = await cloud.listFiles();
result.files.forEach(f => console.log(f.fileName));

// Download un fichier
await cloud.download('mon-fichier.pdf', true);  // true = auto-download

// Vérifier le cluster
await cloud.getClusterStatus();
```

### Autres méthodes disponibles

```javascript
cloud.upload(file, fileName)           // Upload File/Blob
  → { success: true, data: {...} }

cloud.download(fileName, autoDownload) // Download
  → { success: true, blob: Blob }

cloud.listFiles()                      // Liste des fichiers
  → { success: true, files: [...] }

cloud.getClusterStatus()               // État OSD
  → { success: true, status: "..." }
```

---

## 3️⃣ API REST Direct (Pour Code/Scripts)

Pour intégrer avec **n'importe quel langage**.

### Endpoints disponibles

#### Upload
```bash
POST http://10.134.17.222:8080/api/upload?fileName=test.pdf&userId=user123
Content-Type: application/octet-stream

[données binaires du fichier]
```

**Réponse:**
```json
{
  "status": "success",
  "message": "Fichier distribué sur 3 serveurs",
  "fileName": "test.pdf"
}
```

#### Download
```bash
GET http://10.134.17.222:8080/api/download?fileName=test.pdf&userId=user123
```

**Réponse:** Fichier binaire

#### Liste des fichiers
```bash
GET http://10.134.17.222:8080/api/files
```

**Réponse:**
```json
[
  {
    "fileName": "test.pdf",
    "ownerId": "user123",
    "totalSize": 102400,
    "chunks": 1
  },
  ...
]
```

#### État du cluster
```bash
GET http://10.134.17.222:8080/api/cluster
```

**Réponse:**
```
osd-9001|UP|10.134.17.222:9001
osd-9002|UP|10.134.17.222:9002
osd-9003|UP|10.134.17.222:9003
osd-9004|UP|10.134.17.222:9004
```

---

## 📋 Exemples par Langage

### Python
```python
import requests

# Upload
with open('mon-fichier.pdf', 'rb') as f:
    files = {'file': f}
    r = requests.post(
        'http://10.134.17.222:8080/api/upload?fileName=test.pdf&userId=user1',
        data=f.read()
    )
    print(r.json())

# Download
r = requests.get('http://10.134.17.222:8080/api/download?fileName=test.pdf&userId=user1')
with open('downloaded.pdf', 'wb') as f:
    f.write(r.content)

# Lister
r = requests.get('http://10.134.17.222:8080/api/files')
for file in r.json():
    print(f"{file['fileName']} ({file['totalSize']} bytes)")
```

### JavaScript (Node.js + Fetch)
```javascript
const fetch = require('node-fetch');
const fs = require('fs');

// Upload
const fileData = fs.readFileSync('mon-fichier.pdf');
const response = await fetch(
  'http://10.134.17.222:8080/api/upload?fileName=test.pdf&userId=user1',
  { method: 'POST', body: fileData }
);
console.log(await response.json());

// Download
const r = await fetch('http://10.134.17.222:8080/api/download?fileName=test.pdf');
const buffer = await r.buffer();
fs.writeFileSync('downloaded.pdf', buffer);

// Lister
const r = await fetch('http://10.134.17.222:8080/api/files');
const files = await r.json();
files.forEach(f => console.log(f.fileName));
```

### CURL (Terminal/Scripts Bash)
```bash
# Upload
curl -X POST \
  --data-binary @mon-fichier.pdf \
  'http://10.134.17.222:8080/api/upload?fileName=test.pdf&userId=user1'

# Download
curl -O 'http://10.134.17.222:8080/api/download?fileName=test.pdf'

# Lister les fichiers
curl 'http://10.134.17.222:8080/api/files' | python -m json.tool

# État du cluster
curl 'http://10.134.17.222:8080/api/cluster'
```

### Java
```java
import java.net.http.*;
import java.net.URI;

HttpClient client = HttpClient.newHttpClient();

// Upload
byte[] fileData = Files.readAllBytes(Paths.get("mon-fichier.pdf"));
HttpRequest request = HttpRequest.newBuilder()
    .uri(URI.create("http://10.134.17.222:8080/api/upload?fileName=test.pdf&userId=user1"))
    .POST(HttpRequest.BodyPublishers.ofByteArray(fileData))
    .build();
    
HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
System.out.println(response.body());

// Download
request = HttpRequest.newBuilder()
    .uri(URI.create("http://10.134.17.222:8080/api/download?fileName=test.pdf"))
    .GET()
    .build();
    
HttpResponse<byte[]> response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
Files.write(Paths.get("downloaded.pdf"), response.body());
```

---

## 🔧 Paramètres

### fileName
Le nom du fichier à uploader/downloader. Peut contenir des espaces et caractères spéciaux.

**Exemples valides:**
- `document.pdf`
- `my file 2025.xlsx`
- `photo_été.PNG`
- `archive (2).zip`

### userId
Identifiant utilisateur (texte libre, pas de validation).

**Exemples:**
- `user123`
- `alice@company.com`
- `00001`

Le système ne distingue pas les fichiers par userId (tous les fichiers sont partagés).

---

## 🌐 Teste de Connectivité

Avant de coder, vérifier que le serveur répond:

```bash
# Test simple
curl 'http://10.134.17.222:8080/api/files'

# Doit afficher: []  (liste vide ou fichiers existants)
```

Si ça ne marche pas:
- ✓ Est-ce que vous êtes sur le **même réseau**?
- ✓ L'IP 10.134.17.222 est correcte? (vérifier avec `ipconfig` ou `hostname -I`)
- ✓ Le **port 8080** n'est pas bloqué par un firewall?
- ✓ Le serveur est démarré? (vérifier avec `./run.sh`)

---

## ✅ Récapitulatif

| Cas d'usage | Recommandation |
|------------|---|
| Juste uploader/telecharger | Interface Web (http://10.134.17.222:8080) |
| Intégrer dans site web | client-example.js |
| Script de sauvegarde | API REST + curl/wget |
| Intégration programmée | CloudClient classe ou API REST direct |

---

## 📞 Support

**Interface Web:** Ouvrir le navigateur
**API REST:** Documentation complète dans README.md
**Code JavaScript:** Voir comments dans client-example.js
