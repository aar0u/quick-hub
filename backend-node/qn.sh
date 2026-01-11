#!/bin/bash

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    SCRIPT_DIR="D:/dev/quick-hub/backend-node"
else
    SCRIPT_DIR="/Volumes/Storage/dev/quick-hub/backend-node"
fi

echo -e "\nOSTYPE: $OSTYPE\nUsing directory: $SCRIPT_DIR"

if ! command -v pnpm &> /dev/null; then
    echo "Error: pnpm is not installed. Please install pnpm first:"
    echo "  npm install -g pnpm"
    echo "Or visit: https://pnpm.io/installation"
    exit 1
fi

DIR="${1:-.}"
# Convert relative path to absolute path
if [[ "$DIR" == ./* || "$DIR" == ../* || "$DIR" == "." ]]; then
    DIR="$(pwd)/$DIR"
fi

export HTTP_PORT="${2:-3006}"
export HTTPS_PORT="${3:-$((HTTP_PORT + 363))}"

cd "$SCRIPT_DIR"
if [ ! -d "node_modules" ]; then
    echo "Installing dependencies with pnpm..."
    pnpm install
fi

pnpm start "$DIR"