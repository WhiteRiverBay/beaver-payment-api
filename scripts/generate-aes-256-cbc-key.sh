#!/bin/bash

KEY=$(openssl rand -hex 32)

echo "AES-256 Private key in HEX: "
echo "$KEY"
