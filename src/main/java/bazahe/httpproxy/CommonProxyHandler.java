package bazahe.httpproxy;

import bazahe.Context;
import bazahe.httpparse.*;
import bazahe.utils.ListUtils;
import com.google.common.collect.ImmutableSet;
import com.google.common.io.ByteStreams;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.Proxy;
import java.net.Socket;
import java.net.URL;
import java.util.*;

/**
 * Non-connect http handler
 *
 * @author Liu Dong
 */
@Log4j2
public class CommonProxyHandler implements Handler {

    @Override
    public void handle(Socket serverSocket, String rawRequestLine, @Nullable MessageListener messageListener)
            throws IOException {
        HttpInputStream input = new HttpInputStream(new BufferedInputStream(serverSocket.getInputStream()));
        input.putBackLine(rawRequestLine);
        HttpOutputStream output = new HttpOutputStream(serverSocket.getOutputStream());

        while (true) {
            boolean shouldBreak = handleOneRequest(input, output, messageListener);
            if (shouldBreak) {
                logger.debug("Server close connection");
                break;
            }
        }
    }

    //TODO: HttpUrlConnection always resolve dns before send request when using a proxy.
    //how ever, it get a connection pool
    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream input, HttpOutputStream output,
                                     @Nullable MessageListener messageListener) {
        @Nullable RequestHeaders requestHeaders = input.readRequestHeaders();
        // client close connection
        if (requestHeaders == null) {
            logger.debug("Client close connection");
            return true;
        }
        String rawRequestLine = requestHeaders.getRawRequestLine();
        logger.debug("Accept new request: {}", rawRequestLine);

        // expect-100. just tell client continue to send http body
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            output.writeLine("HTTP/1.1 100 Continue\r\n");
        }

        String messageId = MessageIdGenerator.getInstance().nextId();
        RequestLine requestLine = requestHeaders.getRequestLine();
        String method = requestLine.getMethod();
        List<Header> newRequestHeaders = ListUtils.filter(requestHeaders.getHeaders(),
                h -> !proxyRemoveHeaders.contains(h.getName()));
        String url = requestLine.getPath();

        @Nullable OutputStream requestOutput = null;
        if (messageListener != null) {
            requestOutput = messageListener.onHttpRequest(messageId, new URL(url).getHost(), url, requestHeaders);
        }

        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(input, requestHeaders, requestOutput);

        Proxy proxy = Context.getInstance().getProxy();
        int timeout = Context.getInstance().getMainSetting().getTimeout() * 1000;
        HttpURLConnection conn = (HttpURLConnection) new URL(url).openConnection(proxy);
        conn.setRequestMethod(method);
        conn.setReadTimeout(timeout);
        conn.setConnectTimeout(timeout);
        conn.setInstanceFollowRedirects(false);
        if (requestBody != null) {
            conn.setDoOutput(true);
        }

        for (Header header : newRequestHeaders) {
            conn.setRequestProperty(header.getName(), String.valueOf(header.getValue()));
        }
        if (shouldClose) {
            conn.setRequestProperty("Connection", "close");
        }
        conn.connect();
        if (requestBody != null) {
            ByteStreams.copy(requestBody, conn.getOutputStream());
        }
        int statusCode = conn.getResponseCode();
        String statusLine = null;
        // headers and cookies
        List<Header> headerList = new ArrayList<>();
        int index = 0;
        while (true) {
            String key = conn.getHeaderFieldKey(index);
            String value = conn.getHeaderField(index);
            if (value == null) {
                break;
            }
            index++;
            //status line
            if (key == null) {
                statusLine = value;
                continue;
            }
            headerList.add(new Header(key, value));
        }
        InputStream responseInput;
        try {
            responseInput = conn.getInputStream();
        } catch (IOException e) {
            responseInput = conn.getErrorStream();
        }

        Objects.requireNonNull(statusLine);
        output.writeLine(statusLine);
        ResponseHeaders responseHeaders = toResponseHeaders(statusLine, headerList);
        @Nullable OutputStream responseOutput = null;
        if (messageListener != null) {
            responseOutput = messageListener.onHttpResponse(messageId, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(responseOutput, responseInput);

        List<Header> newResponseHeaders = filterResponseHeaders(shouldClose, responseHeaders);
        output.writeHeaders(newResponseHeaders);
        long respLen = responseHeaders.contentLen();
        if (respHasBody(requestHeaders.getRequestLine().getMethod(), statusCode)) {
            output.writeBody(respLen, responseBody);
        }

        return shouldClose;
    }

    private InputStream getResponseBodyInput(@Nullable OutputStream responseOutput, InputStream responseBody) {
        if (responseOutput != null) {
            try {
                responseBody = new ObservableInputStream(responseBody, responseOutput);
            } catch (Throwable e) {
                logger.error("", e);
            }
        }
        return responseBody;
    }

    private List<Header> filterResponseHeaders(boolean shouldClose, ResponseHeaders responseHeaders) {
        List<Header> newResponseHeaders = new ArrayList<>(responseHeaders.getHeaders());
        long respLen = responseHeaders.contentLen();
        Set<String> removeHeaders = new HashSet<>();

        if (respLen == -1) {
            if (!responseHeaders.chunked()) {
                removeHeaders.add("Transfer-Encoding");
                newResponseHeaders.add(new Header("Transfer-Encoding", "chunked"));
            }
        }
        removeHeaders.add("Connection");
        newResponseHeaders.removeIf(h -> removeHeaders.contains(h.getName()));
        if (!shouldClose) {
            newResponseHeaders.add(new Header("Connection", "Keep-Alive"));
        } else {
            newResponseHeaders.add(new Header("Connection", "Close"));
        }
        return newResponseHeaders;
    }

    private Set<String> proxyRemoveHeaders = ImmutableSet.of("Connection", "Proxy-Authenticate", "Proxy-Connection",
            "Transfer-Encoding");

    @SneakyThrows
    @Nullable
    private InputStream getRequestBodyInputStream(HttpInputStream input, RequestHeaders requestHeaders,
                                                  @Nullable OutputStream requestOutput) {
        InputStream requestBody;
        if (requestHeaders.chunked()) {
            requestBody = input.getBody(-1);
        } else if (requestHeaders.contentLen() >= 0) {
            requestBody = input.getBody(requestHeaders.contentLen());
        } else if (!requestHeaders.hasBody()) {
            requestBody = null;
        } else {
            requestBody = null;
//            throw new HttpParserException("Where is body");
        }

        if (requestOutput != null) {
            if (requestBody == null) {
                requestOutput.close();
            } else {
                try {
                    requestBody = new ObservableInputStream(requestBody, requestOutput);
                } catch (Throwable e) {
                    logger.error("", e);
                }
            }
        }
        return requestBody;
    }

    private ResponseHeaders toResponseHeaders(String statusLine, List<Header> headers) {
        List<String> rawHeaders = ListUtils.convert(headers, h -> h.getKey() + ": " + h.getValue());
        return new ResponseHeaders(statusLine, rawHeaders);
    }

    private boolean respHasBody(String method, int statusCode) {
        if (method.equalsIgnoreCase("HEAD")) {
            return false;
        }
        if (statusCode >= 100 && statusCode < 200 || statusCode == 204 || statusCode == 304) {
            return false;
        }
        return true;
    }
}
