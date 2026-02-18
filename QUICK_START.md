# ⚡ Démarrage rapide - Cloud Distribué

## 30 secondes pour commencer

### 1️⃣ Démarrer le serveur
```bash
cd /home/armando/Documents/S3-S4/Cervelet/Data-Stockage-main
./run.sh
```

### 2️⃣ Ouvrir Firefox
```
Accédez à: http://10.134.17.222:8080
```

### 3️⃣ Upload et téléchargement
- Glissez un fichier dans la zone d'upload
- Cliquez "Valider l'upload"
- Cliquez "Actualiser la liste"
- Téléchargez le fichier

✅ **C'est tout!**

---

## 🌐 Accès depuis autre ordinateur

### Même réseau local
Dans n'importe quel navigateur:
```
http://10.134.17.222:8080
```

### Déterminer l'IP du serveur
```bash
# Sur le serveur
hostname -I

# Ou
ifconfig | grep "inet "
```

Utilisez l'IP affichée à la place de `10.134.17.222`

---

## 🔍 Vérifier que ça marche

```bash
# Optionnel: Lancer les tests
chmod +x test.sh
./test.sh
```

Vous devriez voir:
```
✓ Serveur HTTP accessible
✓ GET /api/files: ✓
✓ GET /api/cluster: ✓
✓ Upload: ✓
✓ Fichier trouvé: ✓
✓ Téléchargement: ✓
✓ Intégrité: ✓
```

---

## 🛑 Arrêter le serveur

```bash
# Pressez Ctrl+C dans le terminal
# OU
./stop.sh
```

---

## 📊 Qu'est-ce qui se passe?

### Architecture active

```
5 serveurs + 1 interface web:
├─ 1 DIR Server (port 7000) - coordination
├─ 4 OSD Servers (ports 9001-9004) - stockage
└─ 1 HTTP Server (port 8080) - interface web
```

### Vos fichiers

```
Quand vous uploadez "document.pdf":
├─ Divisé en chunks (1 MB max)
├─ Chaque chunk copié sur 3 serveurs
└─ Survivent aux pannes serveur
```

### Téléchargement intelligent

```
Si un serveur tombe:
├─ Le système le détecte
├─ Récupère depuis une autre copie
└─ Vous ne voyez rien!
```

---

## 📁 Structure des fichiers

```
Ce que vous êtes supposé savoir:

Project root/
├─ src/
│  └─ main/
│     ├─ java/
│     │  └─ datastockage/
│     │     ├─ server/HTTPServer.java ← 🆕 Serveur web
│     │     ├─ dir/DirServer.java ← ✏️ Modifié
│     │     ├─ osd/OSDServer.java
│     │     └─ ...
│     └─ resources/
│        ├─ web/
│        │  └─ index.html ← 🆕 Interface web
│        └─ data/
├─ build.sh
├─ run.sh ← ✏️ Modifié
├─ stop.sh
└─ README.md ← 📖 Documentation complète
```

---

## 🎯 Cas d'usage

### Étudiant
```
1. Lancez ./run.sh
2. Uploadez vos documents
3. Accédez depuis le lab
4. Un serveur tombe? Pas grave, vos données restent!
```

### Projet scolaire
```
1. Installez sur une machine du réseau
2. Tous les étudiants accèdent via Firefox
3. Portfolio distribué et résilient
4. Pas de serveur cloud externalisé
```

### Demo
```
1. Montrez l'interface web
2. Uploadez un fichier
3. Dites "Regardez, c'est répliqué 3x"
4. Arrêtez un OSD
5. Montrez qu'on peut toujours télécharger
```

---

## ❓ FAQ

**Q: Ça marche sur Windows?**
A: Oui, si Java est installé. Utilisez `run.bat` ou adaptez les chemins.

**Q: Ça stocke les fichiers où?**
A: Dans le répertoire `storage/`

**Q: Ça utilise quelle base de données?**
A: Des fichiers JSON dans `dir-meta/` (pas de DB externe)

**Q: Combien de fichiers je peux stocker?**
A: Autant que la place disque des OSD

**Q: Les fichiers survont-ils vraiment aux pannes?**
A: Oui! 2 servers peuvent tomber, les données restent.

---

## 🚀 Prochaines étapes

### Basique
- [ ] Uploadez un fichier
- [ ] Téléchargez-le
- [ ] Vérifiez l'intégrité

### Intermédiaire
- [ ] Arrêtez un OSD
- [ ] Essayez de télécharger un fichier (marche toujours!)
- [ ] Vérifiez le cluster status

### Avancé
- [ ] Lisez IMPLEMENTATION.md
- [ ] Modifiez le facteur de réplication
- [ ] Explorez le code ReplicationManager
- [ ] Tweakez les timeouts heartbeat

---

## 📞 Aide

### Logs du serveur
```bash
# Le serveur affiche les logs dans le terminal
# Cherchez les ✓ et ✗
# Les erreurs sont affichées aussi
```

### Vérifier les connections
```bash
# En live
netstat -an | grep 8080
netstat -an | grep 7000
netstat -an | grep 900
```

### Diagnostics
```bash
# Test basique
curl http://10.134.17.222:8080

# Test API
curl http://10.134.17.222:8080/api/files

# Test upload
curl -X POST http://10.134.17.222:8080/api/upload \
  --data-binary @myfile.txt \
  -H "Content-Type: application/octet-stream"
```

---

**Version**: 1.0  
**Dernière update**: Février 2026  
**Statut**: Prêt pour production éducative
