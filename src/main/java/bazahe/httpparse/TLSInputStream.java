package bazahe.httpparse;

import lombok.Getter;
import net.dongliu.commons.io.InputOutputs;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

/**
 * Utils to read ssl handshake messages
 *
 * @author Liu Dong
 */
public class TLSInputStream extends DataInputStream {

    public TLSInputStream(InputStream in) {
        super(in);
    }

    /**
     * Read TLSPlaintext Header
     */
    public TLSPlaintextHeader readPlaintextHeader() throws IOException {
        int contentType = in.read();
        int majorVersion = in.read();
        int minorVersion = in.read();
        int length = readUnsigned2();
        return new TLSPlaintextHeader(contentType, majorVersion, minorVersion, length);
    }


    /**
     * Read one TLS handshake message. Now only support helloRequest, clientHello, serverHello
     *
     * @throws IOException
     */
    public HandShakeMessage<?> readHandShakeMessage() throws IOException {
        int messageType = in.read();
        int length = readUnsigned3();
        if (messageType == HandShakeMessage.hello_request) {
            return new HandShakeMessage<>(messageType, length, readHelloRequest(length));
        } else if (messageType == HandShakeMessage.client_hello) {
            return new HandShakeMessage<>(messageType, length, readClientHello(length));
        } else if (messageType == HandShakeMessage.server_hello) {
            return new HandShakeMessage<>(messageType, length, readServerHello(length));
        }
        throw new UnsupportedOperationException();
    }

    private ServerHello readServerHello(int length) {
        return new ServerHello();
    }

    private HelloRequest readHelloRequest(int length) {
        return new HelloRequest();
    }

    private ClientHello readClientHello(int length) throws IOException {
        int majorVersion = in.read();
        int minorVersion = in.read();
        byte[] random = readExact(32);
        int sessionIdLen = in.read();
        byte[] sessionId = readExact(sessionIdLen);
        int cipherSuiteLen = readUnsigned2();
        byte[] cipherSuite = readExact(cipherSuiteLen);
        int compressionMethodsLen = in.read();
        byte[] compressionMethods = readExact(compressionMethodsLen);

        int readed = 2 + 32 + 1 + sessionIdLen + 2 + cipherSuiteLen + 1 + compressionMethodsLen;
        if (readed < length) {
            //read extensions
            int extensionsNum = readUnsigned2();
            for (int i = 0; i < extensionsNum; i++) {
                Extension extension = readExtension();
                if (extension.isALPN()) {
                    List<String> alpnNames = extension.ALPNNames();
                }
            }
        }
        return new ClientHello();
    }

    private Extension readExtension() throws IOException {
        int extensionType = readUnsigned2();
        int extensionDataLen = readUnsigned2();
        byte[] data = readExact(extensionDataLen);
        return new Extension(extensionType, data);
    }

    public static class TLSPlaintextHeader {
        private static final int change_cipher_spec = 20;
        private static final int alert = 21;
        private static final int handshake = 22;
        private static final int application_data = 23;

        @Getter
        private final int contentType;
        @Getter
        private final int majorVersion;
        @Getter
        private final int minorVersion;
        @Getter
        private final int length;

        TLSPlaintextHeader(int contentType, int majorVersion, int minorVersion, int length) {
            this.contentType = contentType;
            this.majorVersion = majorVersion;
            this.minorVersion = minorVersion;
            this.length = length;
        }

        public boolean isValidHandShake() {
            return contentType == handshake && majorVersion <= 3 && minorVersion <= 3;
        }
    }

    public static class HelloRequest {

    }

    public static class HandShakeMessage<T> {
        private static final int hello_request = 0;
        private static final int client_hello = 1;
        private static final int server_hello = 2;
        private static final int certificate = 11;
        private static final int server_key_exchange = 12;
        private static final int certificate_request = 13;
        private static final int server_hello_done = 14;
        private static final int certificate_verify = 15;
        private static final int client_key_exchange = 16;
        private static final int finished = 20;

        @Getter
        private final int type;
        @Getter
        private final int length;
        @Getter
        private final T message;

        public HandShakeMessage(int type, int length, T message) {
            this.type = type;
            this.length = length;
            this.message = message;
        }
    }

    public static class ClientHello {


    }

    public static class ServerHello {

    }

    public static class Extension {
        private static final int application_layer_protocol_negotiation = 16;

        private final int type;
        private final byte[] data;

        public Extension(int type, byte[] data) {
            this.type = type;
            this.data = data;
        }

        public boolean isALPN() {
            return type == application_layer_protocol_negotiation;
        }

        public List<String> ALPNNames() throws IOException {
            InputStream input = new ByteArrayInputStream(data);
            int protocolNamesLen = (input.read() << 8) + input.read();
            List<String> protocolNameList = new ArrayList<>(protocolNamesLen);
            for (int i = 0; i < protocolNamesLen; i++) {
                int protocolNameLen = input.read();
                byte[] protocolName = new byte[protocolNameLen];
                InputOutputs.readExact(input, protocolName);
                protocolNameList.add(new String(protocolName));
            }
            return protocolNameList;
        }
    }
}
