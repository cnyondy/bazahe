## 下载安装

### 安装Java
巴扎黑需要 Java8u40 及以上的版本运行，如果系统中没有满足条件的Java版本，首先需要下载最新的 JRE 或 JDK安装。

* Jar 包文件。 打成的Java jar包，需要使用命令java -jar bazahe.jar 来运行，适用于所有操作系统
* Mac 镜像文件。 适用于 macOS
* Linux/Windows 待添加

## 安装根证书

为了对 HTTPS 请求进行抓包，需要使用中间人劫持，这需要将一个自签名的根证书安装到系统中。

程序第一次启动时，会自动生成一个新的根证书。这样每个用户使用的都是不同的根证书，并且这个证书的私钥只在自己的电脑上，所以不用担心在导入了根证书后出现安全问题。

程序通过http的方式提供根证书下载。打开程序，点击 【Start】启动 Http 代理。

在浏览器中打开代理服务器的地址，如:

[[image/open_cert_index.png]]

其中提供了.cer/.crt, .pem格式的证书下载。通常.cer 的证书在各个操作系统中都可以使用。

### macOS 中安装根证书
打开下载的.cer 文件，mac 会打开 钥匙管理器程序 KeyChain，证书被安装到系统中。双击Love ISummer 标题的证书，选择信任:

[[image/mac_trust_cert.png]]

完成证书安装

### iOS 中安装根证书

打开下载的.cer 文件，iOS会提示是否安装，按照提示一步步进行，选择安装。
[[image/ios_install_cert.jpeg]]
[[image/ios_install_cert_2.jpeg]]

## 设置代理

### ios 设置代理

iOS 可以针对某个网络设置使用的 HTTP 代理。
打开 设置 - 无线局域网， 点击使用的网络，在 HTTP 代理下选择手动，填入电脑的本地IP和代理的端口(默认1024):

[[image/ios_set_proxy.jpeg]]
