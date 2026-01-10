#!/bin/bash

if [[ "$OSTYPE" == "msys" || "$OSTYPE" == "cygwin" ]]; then
    export JAVA_TOOL_OPTIONS="-Dfile.encoding=GBK -Dconsole.encoding=GBK"
    JAR_PATH="D:/dev/quick-hub/backend-kotlin/build/libs/quick-hub.jar"
else
    JAR_PATH="/Volumes/Storage/dev/quick-hub/backend-kotlin/build/libs/quick-hub.jar"
fi

echo -e "\nOSTYPE: $OSTYPE\nUsing JAR path: $JAR_PATH\n"

if [ ! -f "$JAR_PATH" ]; then
    echo "Error: JAR file not found at $JAR_PATH"
    echo "Please build the project first"
    exit 1
fi

export HTTP_PORT="${2:-3006}"
export HTTPS_PORT="${3:-$((HTTP_PORT + 363))}"

if [ $# -eq 0 ]; then
    java -jar "$JAR_PATH"
else
    java -jar "$JAR_PATH" "$1"
fi
