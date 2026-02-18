#!/bin/bash

echo "🛑 Arrêt du cluster..."

pkill -f dir.DirServer
pkill -f osd.OSDServer

echo "✅ Cluster arrêté."
