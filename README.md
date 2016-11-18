## Generate self-assigned certificate

1. Make CA Root Certificate, in P#12 format

```sh
keytool -genkeypair -storetype pkcs12 \
    -keystore root_ca.p12 -storepass "${password_for_root_ca_keystore}" \
    -alias "root_ca" -keypass "${password_for_root_ca_key}" \
    -keyalg RSA -keysize 2048 -sigalg SHA1withRSA \
    -dname "cn=MyCompany Bulgaria, ou=Office No 5, o=MyCompany, L=Sofia, S=Sofia, c=BG" \
    -validity 3650
```

2. Convert CA Root Certificate to pem format

```sh
openssl pkcs12 -in root_ca.p12 -out root_ca.pem
# input ${password_for_root_ca_key}
# input ${password_root_ca.pem}
# input ${password_root_ca.pem} again
```


3. Generate key store contains the key and certificate

```sh
keytool -genkeypair -keystore bazahe.jks -storepass ${password_for_my_keystore} \
    -alias bazahe -keypass ${password_for_my_key} \
    -keysize 2048 -keyalg RSA -sigalg sha1withrsa \
    -dname "cn=Mihail Stoynov, ou=MyCompany Bulgaria, o=MyCompany, L=Sofia, S=Sofia, c=BG" \
    -validity 3650
```


4. Export public certificate to pem format

```sh
keytool -exportcert -keystore bazahe.jks -storepass ${password_for_my_keystore} \
    -alias bazahe -keypass ${password_for_my_key} \
    -file bazahe.cer
```


5. Make Certificate Signing Request

```sh
keytool -certreq -keystore bazahe.jks -storepass ${password_for_my_keystore} \
     -alias bazahe -keypass ${password_for_my_key} \
     -keyalg rsa -file bazahe.csr
```

6. Generate a signed certificate for CSR
```sh
openssl x509  -req  -CA root_ca.pem -in bazahe.csr -out bazahe_signed.cer -days 3650 -CAcreateserial
# input ${password_root_ca.pem}
```

7. import root ca into key store:

```sh
keytool -importcert -keystore bazahe.jks  -storepass "${password_for_my_keystore}" \
    -alias "root_ca" -keypass "${password_for_root_ca_key}" \
    -file root_ca.pem
```

8. import the signed key:
```sh
keytool –import –keystore bazahe.jks –file bazahe_signed.cer –alias bazahe
```