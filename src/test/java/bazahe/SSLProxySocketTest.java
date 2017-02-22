package bazahe;

import bazahe.httpproxy.SSLUtils;
import org.junit.Ignore;
import org.junit.Test;

import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocketFactory;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.Proxy;
import java.net.Socket;

/**
 * @author Liu Dong
 */
//http://www.javaworld.com/article/2077475/core-java/java-tip-111--implement-https-tunneling-with-jsse.html
public class SSLProxySocketTest {

    @Test
    @Ignore
    public void testSSLSocketProxy() throws IOException {
        String host = "www.facebook.com";
        String proxyHost = "127.0.0.1";
        Proxy proxy = new Proxy(Proxy.Type.SOCKS, new InetSocketAddress(proxyHost, 1080));
        Socket socket = new Socket(proxy);
        socket.connect(InetSocketAddress.createUnresolved(host, 443));
        SSLContext clientSSlContext = SSLUtils.createClientSSlContext();
        SSLSocketFactory factory = clientSSlContext.getSocketFactory();
        socket = factory.createSocket(socket, proxyHost, 1080, true);

        OutputStream outputStream = socket.getOutputStream();
        outputStream.write("GET / HTTP/1.1\n".getBytes());
        outputStream.write(("Host: " + host + "\n").getBytes());
        outputStream.write("\n".getBytes());
        outputStream.flush();
        InputStream inputStream = socket.getInputStream();
        int read = inputStream.read();
        System.out.println((char)read);
    }

}