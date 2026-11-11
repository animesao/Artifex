#!/bin/bash
set -euo pipefail

# Версия читается из файла VERSION — единый источник правды (используется и CI).
VERSION=$(tr -d '[:space:]' < VERSION)
JAR="target/Artifex-${VERSION}.jar"

echo "==========================================="
echo "  Artifex v${VERSION} Build Script"
echo "==========================================="
echo ""

# Проверяем наличие Maven
if ! command -v mvn &> /dev/null; then
    echo "❌ Maven не найден! Установите Maven для сборки плагина."
    exit 1
fi

echo "🔧 Очистка предыдущих сборок..."
mvn clean

echo ""
echo "📦 Сборка плагина (включая тесты)..."
mvn package

if [ -f "$JAR" ]; then
    echo ""
    echo "✅ Плагин успешно собран!"
    echo "📁 Файл находится в: ${JAR}"
    echo ""
    echo "🚀 Установка на сервер:"
    echo "   cp ${JAR} /path/to/server/plugins/"
    echo ""
    echo "📚 Что нового — см. CHANGELOG.md (секция v${VERSION})"
else
    echo ""
    echo "❌ Ошибка при сборке плагина! Ожидаемый файл не найден: ${JAR}"
    ls -la target/ || true
    exit 1
fi
