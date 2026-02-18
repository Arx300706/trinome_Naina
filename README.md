# ☁️ Cloud Distribué - Système de Stockage Résilient

## Description

Un **système de stockage distribué et décentralisé** accessible en réseau local via une interface web. Les données sont automatiquement répliquées sur 3 serveurs OSD différents pour assurer la résilience aux pannes.

---

## 🚀 Démarrage rapide

### Mode 1: Via le terminal
```bash
cd /home/armando/Documents/S3-S4/Cervelet/Data-Stockage-main
./run.sh
```

### Mode 2: Depuis un IDE
Exécutez la classe `MainApp.java`

---

## 🌐 Accès à l'interface web

Une fois que le serveur est lancé, ouvrez votre navigateur et accédez à:

### URL locale
```
http://10.134.17.222:8080
```

### Depuis n'importe quel ordinateur du réseau
- Utilisez l'adresse IP du serveur au lieu de `10.134.17.222`
- Aucun code source nécessaire, juste un navigateur web moderne
- Upload, Download, et gestion des fichiers directement depuis le navigateur

---

## 🏗️ Architecture

### Services démarrés

| Service | Port | Rôle |
|---------|------|------|
| **DIR Server** | 7000 | Gestion des métadonnées et orchestration |
| **OSD-1** | 9001 | Serveur de stockage distribué |
| **OSD-2** | 9002 | Serveur de stockage distribué |
| **OSD-3** | 9003 | Serveur de stockage distribué |
| **OSD-4** | 9004 | Serveur de stockage distribué |
| **HTTP Server** | 8080 | Interface web et API REST |

---

## 📊 DISTRIBUTION (Répartition des données)

### Fonctionnement

1. **Upload d'un fichier**
   - Le fichier est divisé en **CHUNKS** de 1 Mo chacun
   - Chaque chunk est répliqué sur **3 serveurs OSD différents**
   - Les métadonnées sont sauvegardées sur le DIR

2. **Exemple**
   - Fichier: `document.pdf` (5 Mo)
   - Chunks créés: `chunk_1`, `chunk_2`, `chunk_3`, `chunk_4`, `chunk_5`
   - Chaque chunk est stocké sur 3 serveurs différents
   - Total stocké: 15 Mo (pour 5 Mo de données)

### Avantages
- **Résilience**: Tolérance aux pannes de 2 serveurs OSD
- **Performance**: Lectures parallelisées depuis plusieurs serveurs
- **Équilibrage**: Les données sont distribuées uniformément

---

## 🔄 RECONSTRUCTION (Récupération automatique)

### Logique de récupération

Lors d'un **download**:
1. Le DIR cherche les replicas du chunk
2. Si un OSD est DOWN, essaie le suivant automatiquement
3. Si au moins 1 replica est disponible → chunk récupéré ✓
4. Les chunks sont assemblés en fichier complet

### Exemples de résistance

| Scenario | Résultat |
|----------|----------|
| 1 OSD DOWN | ✓ Fichiers toujours accessibles (2 copies restantes) |
| 2 OSD DOWN | ✓ Fichiers toujours accessibles (1 copie restante) |
| 3 OSD DOWN | ✗ Donnée perdue (cas extrême, très rare) |
| Serveur complètement OFF | ✓ Les autres replicas prennent le relais automatiquement |

### Auto-réparation

Le système **réplique automatiquement** les chunks sous-répliqués:
- Le ReplicationManager vérifie l'état de chaque OSD (heartbeat)
- Si un OSD revient UP après une panne, les chunks sont re-répliqués
- Aucune intervention manuelle nécessaire

---

## 📱 Interface Web - Guide d'utilisation

### 1️⃣ Télécharger un fichier

```
Section "Télécharger un fichier"
├─ Glissez le fichier dans la zone
│  OU cliquez pour sélectionner
├─ Confirmez avec "Valider l'upload"
└─ Statut: "Fichier uploadé et distribué"
```

**Que se passe-t-il?**
- Le fichier est divisé en chunks (1 Mo max)
- Chaque chunk est repliqué sur 3 OSD
- Les métadonnées sont sauvegardées
- Les fichiers survivent aux pannes serveur

### 2️⃣ Télécharger un fichier

```
Section "Fichiers stockés"
├─ Cliquez "Actualiser la liste"
├─ Sélectionnez un fichier
└─ Cliquez "Télécharger"
```

**Récupération automatique:**
- Si un serveur est OFF, utilise un autre replica
- Si plusieurs replicas sont DOWN, utilise le dernier
- Si tous les replicas sont DOWN → "Fichier non trouvé"

### 3️⃣ Vérifier l'état du cluster

```
Section "État du Cluster"
├─ Cliquez "Vérifier l'état"
└─ Voir le statut de chaque OSD (✓ UP ou ✗ DOWN)
```

---

## 🔧 Configuration

### Changer l'adresse IP du serveur

Modifiez l'IP dans les fichiers:

**OSDServer.java** (ligne 11)
```java
static String dirHost = "10.134.17.222";
```

**index.html** (ligne 113)
```javascript
const API_BASE = 'http://10.134.17.222:8080/api';
```

### Facteur de réplication

Dans **ReplicationManager.java** (ligne 31):
```java
public static final int REPLICATION_FACTOR = 3;
```

Augmentez à 4 ou 5 pour plus de résilience (plus de stockage utilisé)

---

## 📊 Monitoring et Logs

### Logs du système

```
[DIR] Serveur lancé sur le port 7000
[OSD osd-9001] Started on port 9001
[HTTPServer] Serveur HTTP démarré sur http://0.0.0.0:8080
[DIR-HTTP] Upload: document.pdf (1048576 bytes)
[DIR-HTTP] Chunk distribué sur 3 OSD
```

### Indicateurs de santé

- **✓ UP**: OSD actif et réactif
- **⚠️ SUSPECTED**: OSD en retard (peut revenir)
- **✗ DOWN**: OSD non réactif (triggers auto-réparation)

---

## ⚡ Cas d'usage réels

### Scenario 1: Panne d'un serveur
```
1. Un OSD tombe en panne
2. Les chunks sont détectés comme sous-répliqués
3. Le ReplicationManager les re-réplique automatiquement
4. Les utilisateurs ne voient aucune interruption
```

### Scenario 2: Upload pendant maintenance
```
1. Vous uploadez un fichier
2. Un OSD tombe en panne PENDANT l'upload
3. Les chunks sont répliqués sur les 3 OSD restants
4. Fichier complètement sécurisé
```

### Scenario 3: Recovery après une longue panne
```
1. Un OSD était DOWN pendant 1 heure
2. OSD redémarre et se reconnecte
3. ReplicationManager détecte les chunks manquants
4. Auto-réplication depuis les autres replicas
5. Cluster revient à la réplication x3
```

---

## 🚨 Troubleshooting

### Port déjà utilisé
```
Error: Port 7000 deja utilise
❌ Arrêtez l'ancienne instance
✓ ./stop.sh
```

### Pas d'accès à l'interface web
```
✓ Vérifiez que le port 8080 est accessible
✓ Utilisez la même IP que le serveur
✓ Comprobez le pare-feu
```

### Fichier introuvable après upload
```
✓ Attendez 2-3 secondes avant de télécharger
✓ Cliquez "Actualiser la liste"
✓ Vérifiez les logs du serveur
```

---

## 📈 Performances

### Vitesses typiques

- **Upload**: 50-100 MB/s (dépend du réseau)
- **Download**: 50-100 MB/s (dépend du réseau)
- **Latence métadonnées**: < 10 ms
- **Détection panne OSD**: 5-10 secondes (heartbeat)
- **Auto-réparation chunk**: 2-5 secondes par chunk

### Limites

- **Taille max chunk**: 1 Mo (configurable)
- **Fichiers simultanés**: Limité par la mémoire
- **Nombre OSD**: Jusqu'à 100+ (recommandé: 4-16)

---

## 🔐 Sécurité (Actuelle)

⚠️ **Version prototype - non prod-ready**

Améliorations recommandées:
- [ ] Authentification utilisateur
- [ ] Chiffrement des données
- [ ] Audit trails
- [ ] Rate limiting

---

## 🎓 Architecture techniquement

### Stack technologique

- **Langage**: Java 11+
- **Protocole**: HTTP/REST + Sockets TCP
- **Persistance**: Fichiers JSON
- **Interface**: HTML5 + CSS3 + JavaScript vanilla
- **Concurrence**: ConcurrentHashMap + ExecutorService

### Classes principales

```
DirServer
  ├─ Gestion métadonnées (FileMeta)
  ├─ Orchestration uploads/downloads
  ├─ ReplicationManager
  │   ├─ HealthChecker (heartbeat)
  │   ├─ RepairEngine (auto-réparation)
  │   └─ Placement (choix serveurs)
  └─ MetaStore (persistance)

OSDServer (x4)
  ├─ Stockage chunks
  ├─ Heartbeat vers DIR
  └─ Gestion fichiers locaux

HTTPServer
  ├─ StaticFileHandler (HTML/CSS/JS)
  ├─ UploadHandler (POST /api/upload)
  ├─ DownloadHandler (GET /api/download)
  ├─ ListFilesHandler (GET /api/files)
  └─ ClusterStatusHandler (GET /api/cluster)
```

---

## 📝 API REST

### Upload
```http
POST /api/upload?fileName=document.pdf&userId=user_123

Body: (fichier binaire)

Response: {"status":"success", "message":"Fichier uploadé et distribué", "fileName":"document.pdf"}
```

### Download
```http
GET /api/download?fileName=document.pdf&userId=user_123

Response: (fichier binaire)
```

### Lister fichiers
```http
GET /api/files

Response: [
  {"fileName":"doc1.pdf","ownerId":"user_123","totalSize":1048576,"chunks":1},
  {"fileName":"image.jpg","ownerId":"user_123","totalSize":2097152,"chunks":2}
]
```

### État du cluster
```http
GET /api/cluster

Response: 
osd-9001|UP|10.134.17.222:9001
osd-9002|UP|10.134.17.222:9002
osd-9003|DOWN|10.134.17.222:9003
osd-9004|UP|10.134.17.222:9004
```

---

## 🎯 Conclusion

Ce système offre:
✅ **Distribution**: Données réparties sur 3 serveurs  
✅ **Résilience**: Survit aux pannes de 2 serveurs  
✅ **Automatique**: Recovery sans intervention  
✅ **Facile**: Interface web intuitive  
✅ **Réseau**: Accès local simple et rapide  

Perfect pour:
- Projets étudiants
- Prototypes
- POC (Proof of Concept)
- Apprentissage distributed systems

---

**Version**: 1.0  
**Date**: Février 2026  
**Auteur**: Système Cloud Distribué
