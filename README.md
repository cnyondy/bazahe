
Watch Http/Https/WebSocket data via Http Proxy

This project now is still under development, it lack features, and may have unexpected bugs.

## Build
Bazahe use [javafx maven plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) to build.
To build this project, Java8u40+ required.

Create executable java jar:

```sh
mvn jfx:jar
```

The jar file can be found at target/app/bazahe.jar. Use

```sh
java -jar target/app/bazahe.jar
```
to run the program.

Also, you can use `mvn jfx:native` to create platform-dependent native routine(and installer), in target/native.

## Https Traffics
Bazahe use mitm to capture https traffics, This need a self signed certificate installed.

You can select one if you already have a ca root key store, or you could generate new one.

To generate a new keystore file, ist, you need go to Configure-Generate New KeyStore File, to generate new ca keyStore and certificate files.
After doing this, three files are generated:

* root_ca.p12  the keyStore file, used by bazehe
* root_ca.crt  the root certificate in der format, used to install into macOS/iOS/Windows(For Windows may need to rename the file name to root_ca.cer)
* root_ca.pem  the root certificate in pem format, used to install into Linux/Android(For some Android may need to rename the file name to root_ca.cer)

Then install the ca certificate into you operation system.