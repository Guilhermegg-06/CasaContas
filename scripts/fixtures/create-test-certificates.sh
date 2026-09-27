#!/bin/sh
set -eu
# Certificados efêmeros, exclusivamente para PostgreSQL sintético do CI.
openssl req -x509 -newkey rsa:2048 -nodes -days 1 -subj '/CN=CasaContas Test CA' \
  -keyout /certificates/ca.key -out /certificates/ca.crt
openssl req -newkey rsa:2048 -nodes -subj '/CN=postgres' \
  -keyout /certificates/server.key -out /certificates/server.csr
printf 'subjectAltName=DNS:postgres\nbasicConstraints=CA:FALSE\n' > /certificates/extensions.cnf
openssl x509 -req -in /certificates/server.csr -CA /certificates/ca.crt \
  -CAkey /certificates/ca.key -CAcreateserial -days 1 \
  -extfile /certificates/extensions.cnf -out /certificates/server.crt
keytool -importcert -noprompt -alias casacontas-test-ca -file /certificates/ca.crt \
  -keystore /certificates/truststore.p12 -storetype PKCS12 -storepass synthetic-trust-password
chmod 644 /certificates/server.crt /certificates/truststore.p12
