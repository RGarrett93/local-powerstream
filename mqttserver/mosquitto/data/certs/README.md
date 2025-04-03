Create a self-signed certificate here:

openssl req -newkey rsa:2048 -nodes -keyout key.pem -x509 -days 3650 -out certificate.pem
Country Name (2 letter code) [XX]:US
State or Province Name (full name) []:
Locality Name (eg, city) [Default City]:
Organization Name (eg, company) [Default Company Ltd]:Let's Encrypt
Organizational Unit Name (eg, section) []:
Common Name (eg, your name or your server's hostname) []:mqtt-e.ecoflow.com
Email Address []: