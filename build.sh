#!/bin/bash

SRC_DIR=src/main/java
BIN_DIR=bin

echo "🔧 Nettoyage des anciens fichiers .class..."
rm -rf $BIN_DIR
mkdir -p $BIN_DIR

echo "🔧 Compilation des sources..."
find $SRC_DIR -name "*.java" > sources.txt
javac -d $BIN_DIR @sources.txt
if [ $? -ne 0 ]; then
    echo "❌ Erreur pendant la compilation"
    exit 1
fi

rm sources.txt
echo "✅ Compilation terminée. Fichiers .class dans $BIN_DIR"
