
Read Http/Https data via Http Proxy

Support Http1.x, Https1.x, Http2.0 support to be added.


## Https Traffics
Bazahe use mitm to capture https traffics, This need a self signed certificate.
This project already contains one default ca root certificate in certificates directory, you need install the ca certificate into you system.
However, it is strongly suggested to create your own ca root certificate.


## Create yourself CA ROOT Certificate

```sh
dname="CN=Love ISummer, OU=Qunar DZS, O=Qunar, L=Beijing, ST=Beijing, C=CN"

# Make CA Root Certificate, in p#12 format
keytool -genkeypair -storetype pkcs12 -keystore root_ca.p12 -storepass "${password_for_root_ca_key}" \
    -keyalg RSA -sigalg SHA256withRSA \
    -dname "$dname" \
    -validity 1000

# Convert CA Root Certificate to crt format
openssl pkcs12 -in root_ca.p12 -out root_ca.pem -passin "pass:${password_for_root_ca_key}" \
    -passout "pass:${password_for_root_ca_key}"
# Convert CA Root Certificate to pem format
openssl x509 -outform der -in root_ca.pem -out root_ca.crt -passin "pass:${password_for_root_ca_key}"
```