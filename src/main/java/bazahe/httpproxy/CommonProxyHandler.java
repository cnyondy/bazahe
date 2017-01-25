package bazahe.httpproxy;

import bazahe.httpparse.*;
import com.google.common.collect.ImmutableSet;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.RequestBuilder;
import net.dongliu.requests.Requests;

import javax.annotation.Nullable;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.*;

import static java.util.stream.Collectors.*;

/**
 * Non-connect http handler
 *
 * @author Liu Dong
 */
@Log4j2
public class CommonProxyHandler implements ProxyHandler {
    
    private static MessageIdGenerator messageIdGenerator = new MessageIdGenerator();

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
        List<Header> newRequestHeaders = requestHeaders.getHeaders().stream()
                .filter(h -> !proxyRemoveHeaders.contains(h.getName()))
                .collect(toList());
        String url = requestLine.getPath();

        @Nullable OutputStream requestOutput = null;
        if (messageListener != null) {
            requestOutput = messageListener.onHttpRequest(messageId, new URL(url).getHost(), url, requestHeaders);
        }

        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(input, requestHeaders, requestOutput);

        RequestBuilder requestBuilder = Requests.newRequest(method, url)
                .compress(false).followRedirect(false)
                .verify(false)
                .headers(newRequestHeaders);
        if (requestBody != null) {
            requestBuilder.body(requestBody);
        }
        @Cleanup RawResponse rawResponse = requestBuilder.send();

        int statusCode = rawResponse.getStatusCode();
        output.writeLine(rawResponse.getStatusLine());
        ResponseHeaders responseHeaders = toResponseHeaders(rawResponse.getStatusLine(), rawResponse.getHeaders());
        @Nullable OutputStream responseOutput = null;
        if (messageListener != null) {
            responseOutput = messageListener.onHttpResponse(messageId, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(responseOutput, rawResponse.getInput());

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

    private ResponseHeaders toResponseHeaders(String statusLine, List<Map.Entry<String, String>> headers) {
        List<String> rawHeaders = headers.stream().map(h -> h.getKey() + ": " + h.getValue()).collect(toList());
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
