#!/bin/bash

# copy hub.sh $HOME/.local/bin/

if [ $# -eq 0 ]; then
    echo "Usage: hub.sh [folder] [http_port]"
    echo "Example: hub.sh /path/to/folder 8080"
    exit 1
fi

folder="$1"
http_port="${2:-3006}"

# Calculate HTTPS port by adding 363 to HTTP port
https_port=$((http_port + 363))

java -jar /Volumes/Storage/dev/quick-hub/backend-kotlin/build/libs/quick-hub.jar "$folder"