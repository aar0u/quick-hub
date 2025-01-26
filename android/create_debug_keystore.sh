#!/bin/bash

# Define a temporary keystore file
TEMP_KEYSTORE=$(mktemp -u)

# Generate the debug keystore
keytool -genkey -v -keystore $TEMP_KEYSTORE -alias debug_alias -keyalg RSA -keysize 2048 -validity 10000 -noprompt \
  -dname "CN=Android Debug,O=Android,C=US" \
  -storepass "android" \
  -keypass "android"

# Output the Base64 encoding of the keystore
BASE64_KEYSTORE=$(base64 < $TEMP_KEYSTORE)
echo "Base64 Encoded Keystore:"
echo $BASE64_KEYSTORE

# Clean up the temporary file
rm $TEMP_KEYSTORE
