#!/usr/bin/env bash

password_for_root_ca_key=123456
password_for_app_keystore=123456
password_for_app_key=123456


function make_root_cert() {
    dname="CN=Love ISummer, OU=Qunar DZS, O=Qunar, L=Beijing, ST=Beijing, C=CN"

    # Make CA Root Certificate, in p#12 format
    keytool -genkeypair -storetype pkcs12 -keystore root_ca.p12 -storepass "${password_for_root_ca_key}" \
        -keyalg RSA -sigalg SHA256withRSA \
        -dname "$dname" \
        -validity 1000

    # Convert CA Root Certificate to pem format
    openssl pkcs12 -in root_ca.p12 -out root_ca.pem -passin "pass:${password_for_root_ca_key}" \
        -passout "pass:${password_for_root_ca_key}"
    openssl x509 -outform der -in root_ca.pem -out root_ca.crt -passin "pass:${password_for_root_ca_key}"
}

function generate_and_sign_cert() {
    dname="CN=Love ISummer, OU=Qunar DZS, O=Qunar, L=Beijing, ST=Beijing, C=CN"

    # Generate key store contains the key and certificate for my app
    keytool -genkeypair -keystore bazahe.jks -storepass ${password_for_app_keystore} \
        -alias bazahe -keypass ${password_for_app_key} \
        -keyalg RSA -sigalg SHA256withRSA \
        -dname "$dname" \
        -validity 365

    # Make Certificate Signing Request
    keytool -certreq -keystore bazahe.jks -storepass ${password_for_app_keystore} \
         -alias bazahe -keypass ${password_for_app_key} \
         -file bazahe.csr

    # Generate a signed certificate for CSR
    openssl x509 -req -sha256 -CA root_ca.pem -in bazahe.csr -out bazahe.cer -days 365 -CAcreateserial \
        -extensions v3_req  -extfile  "$(dirname $0)/openssl.cnf" \
        -passin "pass:${password_for_root_ca_key}"


    # Import root ca into app key store
    keytool -importcert -keystore bazahe.jks  -storepass "${password_for_app_keystore}" \
        -alias "root_ca" -keypass "${password_for_root_ca_key}" \
        -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
        -noprompt -file root_ca.crt

    # Import the signed key, overwrite the origin key
    keytool -importcert -keystore bazahe.jks -storepass "${password_for_app_keystore}" \
        -alias bazahe -keypass "${password_for_app_key}" \
        -keyalg RSA -keysize 2048 -sigalg SHA256withRSA \
        -file bazahe.cer
}

#make_root_cert
generate_and_sign_cert

