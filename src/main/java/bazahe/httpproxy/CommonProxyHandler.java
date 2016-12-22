package bazahe.httpproxy;

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
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.Socket;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Non-connect http handler
 *
 * @author Liu Dong
 */
@Log4j2
public class CommonProxyHandler implements ProxyHandler {

    @Override
    public void handle(Socket serverSocket, String rawRequestLine,
                       @Nullable MessageListener messageListener) throws IOException {
        HttpInputStream inputStream = new HttpInputStream(new BufferedInputStream(serverSocket.getInputStream()));
        inputStream.putBackLine(rawRequestLine);
        HttpOutputStream outputStream = new HttpOutputStream(serverSocket.getOutputStream());

        while (true) {
            boolean shouldBreak = handleOneRequest(inputStream, outputStream, messageListener);
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

        String id = Digests.md5().update(rawRequestLine).toHexLower() + System.nanoTime();
        RequestLine requestLine = requestHeaders.getRequestLine();
        String method = requestLine.getMethod();
        List<Header> newRequestHeaders = filterRequestHeaders(requestHeaders);
        String url = requestLine.getPath();

        @Nullable OutputStream requestOutput = null;
        if (messageListener != null) {
            requestOutput = messageListener.onHttpRequest(id, new URL(url).getHost(), url, requestHeaders);
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
            responseOutput = messageListener.onHttpResponse(id, responseHeaders);
        }
        @Cleanup InputStream responseBody = getResponseBodyInput(responseOutput, rawResponse.getInput());

        List<Header> newResponseHeaders = filterResponseHeaders(shouldClose, responseHeaders);
        output.writeHeaders(newResponseHeaders);
        long respLen = responseHeaders.contentLen();
        if (HttpUtils.respHasBody(requestHeaders.getRequestLine().getMethod(), statusCode)) {
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

    private void removeHeaders(List<Header> headers, String... names) {
        headers.removeIf(h -> Strings.equalsAny(h.getName(), names));
    }

    private ResponseHeaders toResponseHeaders(String statusLine, List<Map.Entry<String, String>> headers) {
        List<String> rawHeaders = Lists.map(headers, h -> h.getKey() + ": " + h.getValue());
        return new ResponseHeaders(statusLine, rawHeaders);
    }

}
