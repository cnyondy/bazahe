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
public class TLSInputs {


    public static TLSPlaintextHeader readPlaintextHeader(InputStream input) throws IOException {
        int contentType = input.read();
        int majorVersion = input.read();
        int minorVersion = input.read();
        int length = (input.read() << 8) + input.read();
        return new TLSPlaintextHeader(contentType, majorVersion, minorVersion, length);
    }


    public static HandShakeMessage<?> readHandShakeMessage(InputStream input) throws IOException {
        int messageType = input.read();
        int length = (input.read() << 16) + (input.read() << 8) + input.read();
        if (messageType == HandShakeMessage.hello_request) {
            return new HandShakeMessage<>(messageType, length, new HelloRequest());
        } else if (messageType == HandShakeMessage.client_hello) {
            return new HandShakeMessage<>(messageType, length, readClientHello(input, length));
        } else if (messageType == HandShakeMessage.server_hello) {
            return new HandShakeMessage<>(messageType, length, null);
        }
        return null;
    }

    private static ClientHello readClientHello(InputStream input, int length) throws IOException {
        int majorVersion = input.read();
        int minorVersion = input.read();
        byte[] random = new byte[32];
        int read = InputOutputs.readExact(input, random);
        int sessionIdLen = input.read();
        byte[] sessionId = new byte[sessionIdLen];
        read = InputOutputs.readExact(input, sessionId);
        int cipherSuiteLen = (input.read() << 8) + input.read();
        byte[] cipherSuite = new byte[cipherSuiteLen];
        read = InputOutputs.readExact(input, cipherSuite);
        int compressionMethodsLen = input.read();
        byte[] compressionMethods = new byte[compressionMethodsLen];
        read = InputOutputs.readExact(input, compressionMethods);

        int readed = 2 + 32 + 1 + sessionIdLen + 2 + cipherSuiteLen + 1 + compressionMethodsLen;
        if (readed < length) {
            //read extensions
            int extensionsNum = (input.read() << 8) + input.read();
            for (int i = 0; i < extensionsNum; i++) {
                Extension extension = readExtension(input);
                if (extension.isALPN()) {
                    List<String> alpnNames = extension.ALPNNames();
                }
            }
        }
        return new ClientHello();
    }

    public static Extension readExtension(InputStream input) throws IOException {
        int extensionType = (input.read() << 8) + input.read();
        int extensionDataLen = (input.read() << 8) + input.read();
        byte[] data = new byte[extensionDataLen];
        int read = InputOutputs.readExact(input, data);
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
