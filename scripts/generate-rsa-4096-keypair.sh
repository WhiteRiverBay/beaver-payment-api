#!/bin/bash

# Generate a RSA keypair
openssl genpkey -algorithm RSA -out private.pem -pkeyopt rsa_keygen_bits:4096

# Extract the public key
openssl rsa -in private.pem -outform PEM -pubout -out public.pem

# Display the public key
echo "Public key:"
cat public.pem

# Display the private key
echo "Private key:"
cat private.pem