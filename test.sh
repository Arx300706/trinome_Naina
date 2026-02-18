#!/bin/bash

# ════════════════════════════════════════════════════════════════════════════
# Test du Cloud Distribué
# Script pour vérifier le fonctionnement du système
# ════════════════════════════════════════════════════════════════════════════

echo "╔═════════════════════════════════════════════════════════════════╗"
echo "║        🧪 TEST DU CLOUD DISTRIBUÉ                              ║"
echo "╚═════════════════════════════════════════════════════════════════╝"
echo ""

# Couleurs
GREEN='\033[0;32m'
RED='\033[0;31m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Configuration
TIMEOUT=5
BASE_URL="http://10.134.17.222:8080"

echo -e "${YELLOW}1️⃣  Vérification du serveur HTTP...${NC}"
response=$(curl -s -o /dev/null -w "%{http_code}" --connect-timeout $TIMEOUT "$BASE_URL")
if [ "$response" = "200" ]; then
    echo -e "${GREEN}✓ Serveur HTTP accessible${NC}"
else
    echo -e "${RED}✗ Serveur HTTP non accessible (code: $response)${NC}"
    echo "  Assurez-vous que le serveur est démarré avec ./run.sh"
    exit 1
fi

echo -e "\n${YELLOW}2️⃣  Vérification de l'API...${NC}"

# Test listage fichiers
echo -n "  • GET /api/files: "
response=$(curl -s -X GET "$BASE_URL/api/files" --connect-timeout $TIMEOUT -H "Content-Type: application/json")
if echo "$response" | grep -q "\["; then
    echo -e "${GREEN}✓${NC}"
else
    echo -e "${RED}✗${NC}"
fi

# Test état cluster
echo -n "  • GET /api/cluster: "
response=$(curl -s -X GET "$BASE_URL/api/cluster" --connect-timeout $TIMEOUT)
if echo "$response" | grep -q "osd"; then
    echo -e "${GREEN}✓${NC}"
    echo ""
    echo "  État du cluster:"
    echo "$response" | sed 's/^/    /' | head -10
else
    echo -e "${RED}✗${NC}"
fi

echo ""
echo -e "${YELLOW}3️⃣  Test d'upload...${NC}"

# Créer un fichier test
TEST_FILE="/tmp/test_file_$(date +%s).txt"
TEST_CONTENT="Ceci est un fichier de test pour le cloud distribué - $(date)"
echo "$TEST_CONTENT" > "$TEST_FILE"
TEST_FILENAME=$(basename "$TEST_FILE")

echo "  • Fichier de test: $TEST_FILENAME"
echo "  • Taille: $(stat -f%z "$TEST_FILE" 2>/dev/null || stat -c%s "$TEST_FILE") octets"

response=$(curl -s -X POST "$BASE_URL/api/upload?fileName=$TEST_FILENAME&userId=test_user" \
    --connect-timeout $TIMEOUT \
    --data-binary @"$TEST_FILE" \
    -H "Content-Type: application/octet-stream")

if echo "$response" | grep -q "success"; then
    echo -e "  • Upload: ${GREEN}✓${NC}"
else
    echo -e "  • Upload: ${RED}✗${NC}"
    echo "    Réponse: $response"
fi

echo ""
echo -e "${YELLOW}4️⃣  Vérification du fichier uploadé...${NC}"
sleep 2 # Attendre que le fichier soit indexé

response=$(curl -s -X GET "$BASE_URL/api/files" --connect-timeout $TIMEOUT)
if echo "$response" | grep -q "$TEST_FILENAME"; then
    echo -e "  • Fichier trouvé dans le cluster: ${GREEN}✓${NC}"
else
    echo -e "  • Fichier trouvé dans le cluster: ${RED}✗${NC}"
fi

echo ""
echo -e "${YELLOW}5️⃣  Test de téléchargement...${NC}"

DOWNLOAD_FILE="/tmp/downloaded_$TEST_FILENAME"
curl -s -X GET "$BASE_URL/api/download?fileName=$TEST_FILENAME&userId=test_user" \
    --connect-timeout $TIMEOUT \
    --output "$DOWNLOAD_FILE"

if [ -f "$DOWNLOAD_FILE" ] && [ -s "$DOWNLOAD_FILE" ]; then
    echo -e "  • Téléchargement: ${GREEN}✓${NC}"
    
    # Vérifier le contenu
    if grep -q "cloud distribué" "$DOWNLOAD_FILE"; then
        echo -e "  • Intégrité du fichier: ${GREEN}✓${NC}"
    else
        echo -e "  • Intégrité du fichier: ${RED}✗${NC}"
    fi
else
    echo -e "  • Téléchargement: ${RED}✗${NC}"
fi

echo ""
echo "╔═════════════════════════════════════════════════════════════════╗"
echo "║                      ✅ TESTS TERMINÉS                         ║"
echo "║                                                                 ║"
echo "║  🌐 Accès web: http://10.134.17.222:8080                       ║"
echo "╚═════════════════════════════════════════════════════════════════╝"

# Cleanup
rm -f "$TEST_FILE" "$DOWNLOAD_FILE"
