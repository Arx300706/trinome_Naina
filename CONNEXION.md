# Comment se connecter au Cloud Distribué depuis les autres ordinateurs

**Bienvenue!** Votre cloud distribué est maintenant **accessible sur le réseau** à l'adresse:

## 🌍 Accédez au cloud avec n'importe quel navigateur

```
http://10.134.17.222:8080
```

Vous pouvez:
- **Upload** des fichiers (drag-and-drop automatique)
- **Download** vos fichiers stockés
- **Voir l'état** du système

---

## 3 façons de vous connecter

### 1️⃣ **Interface Web (Navigateur)**
La plus simple! Juste ouvrir:
```
Firefox → http://10.134.17.222:8080
```

### 2️⃣ **Client JavaScript Autonome**
Téléchargez `client-standalone.html` et ouvrez-le dans votre navigateur.
Vous pouvez **changer l'adresse IP** dans le menu si besoin.

### 3️⃣ **API REST (Pour programmeurs)**
Intégrez dans votre code:

**Upload un fichier:**
```bash
curl -X POST --data-binary @monfile.pdf \
  'http://10.134.17.222:8080/api/upload?fileName=monfile.pdf&userId=user1'
```

**Download un fichier:**
```bash
curl 'http://10.134.17.222:8080/api/download?fileName=monfile.pdf' -O
```

**Voir tous les fichiers:**
```bash
curl 'http://10.134.17.222:8080/api/files'
```

---

## 📍 Adresses importantes

| Service | Adresse |
|---------|---------|
| **Interface Web** | http://10.134.17.222:8080 |
| **Upload API** | http://10.134.17.222:8080/api/upload |
| **Download API** | http://10.134.17.222:8080/api/download |
| **Fichiers liste** | http://10.134.17.222:8080/api/files |
| **État du cluster** | http://10.134.17.222:8080/api/cluster |

---

## ⚡ Les fichiers sont répliqués 3x automatiquement

Chaque fichier que vous uploadez est:
- Découpés en **chunks de 1MB**
- Distribués sur **3 serveurs différents**
- **Automatiquement auto-réparé** si un serveur tombe

**Cela signifie:** Vous pouvez perdre jusqu'à **2 serveurs** sans rien perdre!

---

## 🔧 Fichiers utiles

- `GUIDE_CONNEXION.md` - Guide complet avec exemples (Python, Java, Node.js)
- `client-example.js` - Classe JavaScript à réutiliser dans vos apps
- `client-standalone.html` - Client web autonome

---

## ❓ Besoin d'aide?

1. Vérifier que le serveur fonctionne: `./run.sh`
2. Tester la connection: `curl 'http://10.134.17.222:8080/api/files'`
3. Vérifier que vous êtes sur le **même réseau**
4. Lire `GUIDE_CONNEXION.md` pour plus d'exemples

---

**C'est tout!** Vous pouvez maintenant accéder au cloud distribué depuis n'importe quel ordinateur du réseau. 🎉
