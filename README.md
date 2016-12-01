
Watch Http/Https/WebSocket data via Http Proxy


[中文使用说明文档](https://github.com/clearthesky/bazahe/blob/master/docs/usage_cn.md)

## Build
Java8u40+ required To build this project.

Bazahe use [javafx maven plugin](https://github.com/javafx-maven-plugin/javafx-maven-plugin) to distribute.

Create executable java jar:

```sh
mvn jfx:jar
```

The jar file can be found at target/app/bazahe.jar. Use

```sh
java -jar target/app/bazahe.jar
```
to run the program.

Also, you can use `mvn jfx:native` to create platform-dependent native routine(and installer), you will find them under path target/native.

## Https Traffics
Bazahe use mitm to capture https traffics, This need a self signed certificate installed.
When Bazahe start at the first time,it will create a new CA Root Certificate and private key, save to $HOME/.bazahe/bazahe.p12.

Then you need to import the CA Root Certificate into you operation system.
Open you browser, and enter the address you proxy listened on, you will see a certificate export page. Download the certificate and import.

Different system may required different certificate format, some os accept them all.
Usually,  use crt for macOS/iOS, pem for Linux/Android.
