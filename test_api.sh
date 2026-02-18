#!/bin/bash

# ════════════════════════════════════════════════════════════════════════════
# Test de l'API REST - Cloud Distribué
# ════════════════════════════════════════════════════════════════════════════

echo "╔═════════════════════════════════════════════════════════════════╗"
echo "║        🧪 TEST API REST - CLOUD DISTRIBUÉ                      ║"
echo "╚═════════════════════════════════════════════════════════════════╝"
echo ""

BASE_URL="http://10.134.17.222:8080"
TIMEOUT=5

# Test 1: Vérifier que le serveur est accessible
echo "1️⃣  Vérification du serveur API REST..."
response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout $TIMEOUT "$BASE_URL/api/files")
if [ "$response" = "200" ]; then
    echo "   ✓ Serveur réponse OK (code $response)"
else
    echo "   ✗ Serveur non accessible (code: $response)"
    echo "   Assurez-vous que le serveur est démarré avec ./run.sh"
    exit 1
fi

# Test 2: Lister les fichiers
echo ""
echo "2️⃣  API GET /api/files - Lister fichiers..."
response=$(curl -s -X GET "$BASE_URL/api/files" --connect-timeout $TIMEOUT)
echo "   Réponse:"
echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"

# Test 3: État du cluster
echo ""
echo "3️⃣  API GET /api/cluster - État du cluster..."
response=$(curl -s -X GET "$BASE_URL/api/cluster" --connect-timeout $TIMEOUT)
echo "   Réponse:"
echo "$response"

# Test 4: Upload un fichier de test
echo ""
echo "4️⃣  API POST /api/upload - Tester upload..."

# Créer un fichier test
TEST_FILE="/tmp/test_distributed_$(date +%s).txt"
echo "Voici un fichier de test pour le cloud distribué" > "$TEST_FILE"
TEST_FILENAME=$(basename "$TEST_FILE")

echo "   Fichier test: $TEST_FILENAME"

response=$(curl -s -X POST "$BASE_URL/api/upload?fileName=$TEST_FILENAME&userId=test_user" \
    --connect-timeout $TIMEOUT \
    --data-binary @"$TEST_FILE")

echo "   Réponse:"
echo "$response" | python3 -m json.tool 2>/dev/null || echo "$response"

# Attendre un peu
sleep 2

# Test 5: Chercher le fichier uploadé
echo ""
echo "5️⃣  Vérifier que le fichier est maintenant dans la liste..."
response=$(curl -s -X GET "$BASE_URL/api/files" --connect-timeout $TIMEOUT)
if echo "$response" | grep -q "$TEST_FILENAME"; then
    echo "   ✓ Fichier trouvé dans le cluster!"
else
    echo "   ✗ Fichier non trouvé"
fi

# Test 6: Télécharger le fichier
echo ""
echo "6️⃣  API GET /api/download - Tester download..."
DOWNLOAD_FILE="/tmp/downloaded_$TEST_FILENAME"

curl -s -X GET "$BASE_URL/api/download?fileName=$TEST_FILENAME&userId=test_user" \
    --connect-timeout $TIMEOUT \
    --output "$DOWNLOAD_FILE"

if [ -f "$DOWNLOAD_FILE" ] && [ -s "$DOWNLOAD_FILE" ]; then
    echo "   ✓ Fichier téléchargé avec succès"
    if diff "$TEST_FILE" "$DOWNLOAD_FILE" > /dev/null 2>&1; then
        echo "   ✓ Intégrité vérifiée (fichier identique)"
    else
        echo "   ✗ Intégrité incorrecte (fichiers différents)"
    fi
else
    echo "   ✗ Échec du téléchargement"
fi

# Cleanup
rm -f "$TEST_FILE" "$DOWNLOAD_FILE"

echo ""
echo "╔═════════════════════════════════════════════════════════════════╗"
echo "║                     ✅ TESTS COMPLÉTÉS                         ║"
echo "║                                                                 ║"
echo "║  API REST prête à l'usage!                                     ║"
echo "║  URL: http://10.134.17.222:8080                                ║"
echo "╚═════════════════════════════════════════════════════════════════╝"
