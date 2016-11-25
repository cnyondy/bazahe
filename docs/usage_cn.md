## 下载安装

### 下载安装Java
巴扎黑需要 Java8u40 及以上的版本运行，如果系统中没有满足条件的Java版本，首先需要下载最新的 JRE 或 JDK安装，如果只是运行不做开发的话下载 JRE 即可。

在 macOS 上，打开终端输入 java，则会引导去下载最新的 JRE。

[到这里下载](http://www.oracle.com/technetwork/java/javase/downloads/jre8-downloads-2133155.html) JRE，
[到这里下载](http://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html) JDK.

### 下载巴扎黑
当前提供了 jar 包文件 和 macOS 镜像文件下载，[点击这里去下载](https://github.com/clearthesky/bazahe/releases)。

* bazahe_jar.zip。 Jar 包文件，解压缩后需要使用命令java -jar app/bazahe.jar 来运行，适用于所有操作系统
* bazahe_mac.zip。 Mac 镜像文件。 适用于 macOS

运营程序，点击【Start】启动 HTTP 代理，点击【Setting】设置代理的端口。程序界面如下所示:

![mac_trust_cert](https://github.com/clearthesky/bazahe/blob/master/docs/image/mac_trust_cert.png)

## 安装根证书

为了对 HTTPS 请求进行抓包，需要使用中间人劫持，这需要将一个自签名的根证书安装到系统中。

程序第一次启动时，会自动生成一个新的根证书。这样每个用户使用的都是不同的根证书，并且这个证书的私钥只在自己的电脑上，所以不用担心在导入了根证书后出现安全问题。

程序通过 http 的方式提供根证书下载。打开程序，点击 【Start】启动 Http 代理。

在浏览器中打开代理服务器的地址，如:

![open_cert_index](https://github.com/clearthesky/bazahe/blob/master/docs/image/open_cert_index.png)

其中提供了.cer/.crt, .pem格式的证书下载。通常.cer 的证书在各个操作系统中都可以使用。

### macOS 中安装根证书
打开下载的.cer 文件，mac 会打开 钥匙管理器程序 KeyChain，证书被安装到系统中。双击Love ISummer 标题的证书，选择信任:

![mac_trust_cert](https://github.com/clearthesky/bazahe/blob/master/docs/image/mac_trust_cert.png)


完成证书安装

### iOS 中安装根证书

打开下载的.cer 文件，iOS会提示是否安装，按照提示一步步进行，选择安装。

![ios_install_cert](https://github.com/clearthesky/bazahe/blob/master/docs/image/ios_install_cert.jpeg)

点击安装

![ios_install_cert](https://github.com/clearthesky/bazahe/blob/master/docs/image/ios_install_cert_2.jpeg)

会出现警告，继续点击安装

## 抓包, 设置代理
讲需要抓包的浏览器，或者程序，或者系统代理设置为程序所启动的 HTTP 代理，即可抓包。代理的默认端口是1024, IP则使用电脑分配的本地IP。
如果是抓移动设备上的数据，需要移动设备和电脑在同一个内网网段。 

### ios 设置代理

iOS 可以针对某个网络设置使用的 HTTP 代理。
打开 设置 - 无线局域网， 点击使用的网络，在 HTTP 代理下选择手动，填入电脑的本地IP和代理的端口:

![ios_set_proxy](https://github.com/clearthesky/bazahe/blob/master/docs/image/ios_set_proxy.jpeg)

即可。
