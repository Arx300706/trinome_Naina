/**
 * Exemple de client JavaScript pour le Cloud Distribué
 * À utiliser dans un navigateur ou Node.js avec fetch
 * 
 * Utilisation:
 * 1. Dans un HTML: <script src="client-example.js"></script>
 * 2. Puis utiliser: const client = new CloudClient('http://10.134.17.222:8080')
 */

class CloudClient {
  constructor(serverUrl) {
    this.serverUrl = serverUrl || 'http://10.134.17.222:8080';
    this.userId = 'user_' + Math.random().toString(36).substr(2, 9);
    console.log(`Client Cloud initialisé (${this.userId}) → ${this.serverUrl}`);
  }

  /**
   * Upload un fichier sur le cloud
   * @param {File|Blob} file - Fichier à uploader
   * @param {string} fileName - Nom du fichier (optionnel, utilise file.name par défaut)
   */
  async upload(file, fileName) {
    fileName = fileName || file.name || 'file_' + Date.now();
    console.log(`[UPLOAD] ${fileName} (${this.formatSize(file.size)})...`);
    
    try {
      const url = `${this.serverUrl}/api/upload?fileName=${encodeURIComponent(fileName)}&userId=${this.userId}`;
      const response = await fetch(url, {
        method: 'POST',
        body: file
      });
      
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      const data = await response.json();
      console.log(`✓ Upload réussi:`, data.message);
      return { success: true, data };
    } catch (err) {
      console.error(`✗ Erreur upload:`, err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Download un fichier du cloud
   * @param {string} fileName - Nom du fichier
   * @param {boolean} autoDownload - Si true, déclenche le téléchargement navigateur
   */
  async download(fileName, autoDownload = false) {
    console.log(`[DOWNLOAD] ${fileName}...`);
    
    try {
      const url = `${this.serverUrl}/api/download?fileName=${encodeURIComponent(fileName)}&userId=${this.userId}`;
      const response = await fetch(url);
      
      if (!response.ok) throw new Error(`HTTP ${response.status} - Fichier non trouvé`);
      const blob = await response.blob();
      
      if (autoDownload) {
        const a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = fileName;
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
      }
      
      console.log(`✓ Download réussi (${this.formatSize(blob.size)})`);
      return { success: true, blob, size: blob.size };
    } catch (err) {
      console.error(`✗ Erreur download:`, err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Liste tous les fichiers du cloud
   */
  async listFiles() {
    console.log(`[LIST] Récupération des fichiers...`);
    
    try {
      const response = await fetch(`${this.serverUrl}/api/files`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      
      const files = await response.json();
      console.log(`✓ ${files.length} fichier(s) trouvé(s)`);
      
      files.forEach(f => {
        console.log(`  - ${f.fileName} (${this.formatSize(f.totalSize)}) [${f.chunks} chunks]`);
      });
      
      return { success: true, files };
    } catch (err) {
      console.error(`✗ Erreur listage:`, err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Obtient l'état du cluster (status OSD)
   */
  async getClusterStatus() {
    console.log(`[CLUSTER] Vérification de l'état...`);
    
    try {
      const response = await fetch(`${this.serverUrl}/api/cluster`);
      if (!response.ok) throw new Error(`HTTP ${response.status}`);
      
      const status = await response.text();
      console.log(`✓ État du cluster:\n${status}`);
      
      return { success: true, status };
    } catch (err) {
      console.error(`✗ Erreur cluster:`, err.message);
      return { success: false, error: err.message };
    }
  }

  /**
   * Utilitaire: Formate une taille en bytes
   */
  formatSize(bytes) {
    if (bytes === 0) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
}

// ═══════════════════════════════════════════════════════════════════════════
// EXEMPLES D'UTILISATION
// ═══════════════════════════════════════════════════════════════════════════

// Exemple 1: Usage dans un navigateur avec interface
/*
const cloud = new CloudClient('http://10.134.17.222:8080');

// Upload avec un input file
document.getElementById('fileInput').addEventListener('change', async (e) => {
  const file = e.target.files[0];
  const result = await cloud.upload(file);
  console.log(result);
});

// Afficher les fichiers
async function refreshFiles() {
  const result = await cloud.listFiles();
  if (result.success) {
    result.files.forEach(f => {
      console.log(f.fileName);
    });
  }
}
*/

// Exemple 2: Usage dans Node.js (avec node-fetch)
/*
const fetch = require('node-fetch');
const fs = require('fs');

async function demo() {
  const cloud = new CloudClient('http://10.134.17.222:8080');
  
  // Upload d'un fichier
  const fileBuffer = fs.readFileSync('mon-fichier.txt');
  const blob = new Blob([fileBuffer]);
  await cloud.upload(blob, 'mon-fichier.txt');
  
  // Lister les fichiers
  await cloud.listFiles();
  
  // Vérifier le cluster
  await cloud.getClusterStatus();
}

demo().catch(console.error);
*/

// Export pour module systems
if (typeof module !== 'undefined' && module.exports) {
  module.exports = CloudClient;
}
