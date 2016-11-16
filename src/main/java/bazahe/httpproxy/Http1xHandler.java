package bazahe.httpproxy;

import bazahe.exception.HttpParserException;
import bazahe.httpparse.*;
import lombok.Cleanup;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;
import net.dongliu.commons.codec.Digests;
import net.dongliu.commons.collection.Lists;
import net.dongliu.requests.RawResponse;
import net.dongliu.requests.RequestBuilder;
import net.dongliu.requests.Requests;

import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Http1.x handler
 *
 * @author Liu Dong
 */
@Log4j2
public abstract class Http1xHandler implements ProxyHandler {

    @Override
    public void handle(Socket socket, HttpInputStream input, HttpOutputStream output,
                       @Nullable HttpMessageListener httpMessageListener) throws IOException {
        // set tcp timeout(also keep-alive) to 60 secs.
        // TODO: this should be configured
        socket.setSoTimeout(60000);
        while (true) {
            boolean shouldBreak = handleOneRequest(input, output, httpMessageListener);
            if (shouldBreak) {
                log.debug("Server close connection");
                break;
            }
        }
    }

    @SneakyThrows
    private boolean handleOneRequest(HttpInputStream input, HttpOutputStream output,
                                     @Nullable HttpMessageListener httpMessageListener) {
        @Nullable RequestHeaders requestHeaders = input.readRequestHeaders();
        // client close connection
        if (requestHeaders == null) {
            log.debug("Client close connection");
            return true;
        }
        String rawRequestLine = requestHeaders.getRawRequestLine();
        log.debug("Accept new request: {}", rawRequestLine);

        // expect-100
        // TODO: should forward "100-continue" if target support http 1.1
        if ("100-continue".equalsIgnoreCase(requestHeaders.getFirst("Expect"))) {
            output.writeLine("HTTP/1.1 100 Continue\r\n");
        }

        String id = Digests.md5().update(rawRequestLine).toHexLower() + System.nanoTime();
        @Nullable OutputStream requestOutput = null;
        if (httpMessageListener != null) {
            requestOutput = httpMessageListener.onRequest(id, requestHeaders);
        }

        boolean shouldClose = requestHeaders.shouldClose();
        @Cleanup @Nullable InputStream requestBody = getRequestBodyInputStream(input, requestHeaders, requestOutput);

        RequestLine requestLine = requestHeaders.getRequestLine();
        String method = requestLine.getMethod();
        List<Header> newRequestHeaders = filterRequestHeaders(requestHeaders);
        RequestBuilder rb = Requests.newRequest(method, getUrl(requestLine.getUrl()))
                .compress(false).followRedirect(false)
                .verify(false)
                .headers(newRequestHeaders);
        if (requestBody != null) {
            rb.body(requestBody);
        }
        @Cleanup RawResponse rawResponse = rb.send();

        int statusCode = rawResponse.getStatusCode();
        output.writeLine(rawResponse.getStatusLine());
        ResponseHeaders responseHeaders = toResponseHeaders(rawResponse.getStatusLine(), rawResponse.getHeaders());
        @Nullable OutputStream responseOutput = null;
        if (httpMessageListener != null) {
            responseOutput = httpMessageListener.onResponse(id, responseHeaders);
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

    protected abstract String getUrl(String path);

    private InputStream getResponseBodyInput(@Nullable OutputStream responseOutput, InputStream responseBody) {
        if (responseOutput != null) {
            try {
                responseBody = new ObservableInputStream(responseBody, responseOutput);
            } catch (Throwable e) {
                log.error("", e);
            }
        }
        return responseBody;
    }

    private List<Header> filterResponseHeaders(boolean shouldClose, ResponseHeaders responseHeaders) {
        List<Header> newResponseHeaders = new ArrayList<>(responseHeaders.getHeaders());
        long respLen = responseHeaders.contentLen();
        if (respLen == -1) {
            if (!responseHeaders.chunked()) {
                removeHeaders(newResponseHeaders, "Transfer-Encoding");
                newResponseHeaders.add(new Header("Transfer-Encoding", "chunked"));
            }
        }
        removeHeaders(newResponseHeaders, "Connection");
        if (!shouldClose) {
            newResponseHeaders.add(new Header("Connection", "Keep-Alive"));
        } else {
            newResponseHeaders.add(new Header("Connection", "Close"));
        }
        return newResponseHeaders;
    }

    private List<Header> filterRequestHeaders(RequestHeaders requestHeaders) {
        List<Header> newRequestHeaders = new ArrayList<>(requestHeaders.getHeaders());
        newRequestHeaders.removeIf(h -> h.getName().equalsIgnoreCase("Connection"));
        removeHeaders(newRequestHeaders, "Connection", "Proxy-Authenticate", "Proxy-Connection", "Transfer-Encoding");
        return newRequestHeaders;
    }

    @SneakyThrows
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
            throw new HttpParserException("Where is body");
        }

        if (requestOutput != null) {
            if (requestBody == null) {
                requestOutput.close();
            } else {
                try {
                    requestBody = new ObservableInputStream(requestBody, requestOutput);
                } catch (Throwable e) {
                    log.error("", e);
                }
            }
        }
        return requestBody;
    }

    private void removeHeaders(List<Header> headers, String... names) {
        headers.removeIf(h -> Strings.equalsAny(h.getName(), names));
    }

    private ResponseHeaders toResponseHeaders(String statusLine, List<Map.Entry<String, String>> headers) {
        List<String> rawHeaders = Lists.map(headers, h -> h.getKey() + ": " + h.getValue());
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
