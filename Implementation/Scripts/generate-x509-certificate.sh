#!/bin/bash
openssl req -x509 -newkey rsa:4096 -keyout key.pem -out cert.pem -days 36500
mv *.pem ./ssl