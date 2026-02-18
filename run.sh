#!/bin/bash

# ════════════════════════════════════════════════════════════════════════════
# Cloud Distribué - Système de Stockage Résilient
# Script de démarrage
# ════════════════════════════════════════════════════════════════════════════

BIN_DIR=bin

echo "╔═════════════════════════════════════════════════════════════════╗"
echo "║        ☁️  CLOUD DISTRIBUÉ - DÉMARRAGE                         ║"
echo "║        Système de Stockage Résilient et Décentralisé           ║"
echo "╚═════════════════════════════════════════════════════════════════╝"
echo ""
echo "📋 Démarrage des services..."
echo "   → DIR Server (port 7000) - Métadonnées"
echo "   → OSD Servers (ports 9001-9004) - Stockage distribué" 
echo "   → API REST (port 8080) - Accès réseau"
echo ""
echo "🔗 API REST disponible sur:"
echo "   http://10.134.17.222:8080"
echo ""

# Compiler si nécessaire
if [ ! -f "$BIN_DIR/MainApp.class" ]; then
    echo "🔧 Compilation requise..."
    ./build.sh
    if [ $? -ne 0 ]; then
        echo "✗ Erreur de compilation"
        exit 1
    fi
fi

# Lancer l'application
echo "▶️  Lancement..."
java -cp $BIN_DIR MainApp
