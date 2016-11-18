#!/usr/bin/env bash

password_for_root_ca_key=123456
password_for_app_keystore=123456
password_for_app_key=123456

# Make CA Root Certificate, in p#12 format
keytool -genkeypair -storetype pkcs12 -keystore root_ca.p12 -storepass "${password_for_root_ca_key}" \
    -keyalg RSA -keysize 2048 -sigalg SHA1withRSA \
    -dname "cn=MyCompany Bulgaria, ou=Office No 5, o=MyCompany, L=Sofia, S=Sofia, c=BG" \
    -validity 3650

# Convert CA Root Certificate to pem format
openssl pkcs12 -in root_ca.p12 -out root_ca.pem -passin "pass:${password_for_root_ca_key}" \
    -passout "pass:${password_for_root_ca_key}"

# Generate key store contains the key and certificate for my app
keytool -genkeypair -keystore bazahe.jks -storepass ${password_for_app_keystore} \
    -alias bazahe -keypass ${password_for_app_key} \
    -keysize 2048 -keyalg RSA -sigalg sha1withrsa \
    -dname "cn=Mihail Stoynov, ou=MyCompany Bulgaria, o=MyCompany, L=Sofia, S=Sofia, c=BG" \
    -validity 3650

# Export public certificate to pem format
keytool -exportcert -keystore bazahe.jks -storepass ${password_for_app_keystore} \
    -alias bazahe -keypass ${password_for_app_key} \
    -file bazahe.cer

# Make Certificate Signing Request
keytool -certreq -keystore bazahe.jks -storepass ${password_for_app_keystore} \
     -alias bazahe -keypass ${password_for_app_key} \
     -keyalg rsa -file bazahe.csr

# Generate a signed certificate for CSR
openssl x509 -req -CA root_ca.pem -in bazahe.csr -out bazahe_signed.cer -days 3650 -CAcreateserial \
    -passin "pass:${password_for_root_ca_key}"


# Import root ca into app key store
openssl x509 -outform der -in root_ca.pem -out root_ca.crt -passin "pass:${password_for_root_ca_key}"
keytool -importcert -keystore bazahe.jks  -storepass "${password_for_app_keystore}" \
    -alias "root_ca" -keypass "${password_for_root_ca_key}" \
    -noprompt -file root_ca.crt

# Import the signed key, overwrite the origin key
keytool -importcert -keystore bazahe.jks -storepass "${password_for_app_keystore}" \
    -alias bazahe -keypass "${password_for_app_key}" \
    -file bazahe_signed.cer