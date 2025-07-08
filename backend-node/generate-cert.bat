@echo off

set CERT_FILE=cert\cert.pem
set KEY_FILE=cert\key.pem

REM Generate key and cert
openssl req -x509 -newkey rsa:2048 -nodes -keyout %KEY_FILE% -out %CERT_FILE% -days 365 -subj "/CN=localhost"

echo generated with self-signed key and cert.
