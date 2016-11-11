package bazahe.httpproxy;

import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;
import net.dongliu.commons.Strings;

import javax.annotation.Nullable;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaderNames.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Netty http handler, to proxy http request
 *
 * @author Liu Dong
 */
@Log4j2
public class HttpProxyHandler extends ChannelInboundHandlerAdapter {

    // -1 means not set
    private long contentLen;
    @Nullable
    private HttpHeaders headers;

    private volatile ChannelFuture cf;
    private List<HttpContent> buf;

    private HttpClient client = new HttpClient();

    private ChannelFuture lastWriteFuture;

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.channel().read();
        super.channelActive(ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelUnregistered(ChannelHandlerContext ctx) throws Exception {
        super.channelUnregistered(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // start of new http request
        if (msg instanceof HttpRequest) {
            onReceiveHttpRequest(ctx, (HttpRequest) msg);
        }

        // the http body chunk
        if (msg instanceof HttpContent) {
            onReceiveHttpBodyChunk(ctx, (HttpContent) msg);
        }
    }

    @SneakyThrows
    private void onReceiveHttpRequest(ChannelHandlerContext ctx, HttpRequest request) {
        log.debug("receive new http request: {}", request.uri());
        if (HttpUtil.isContentLengthSet(request)) {
            contentLen = HttpUtil.getContentLength(request);
        } else {
            contentLen = -1;
        }
        headers = request.headers();
        URL uri = new URL(request.uri());

        log.debug("connect to {}:{}", uri.getHost(), uri.getPort());
        ChannelFuture cf = client.connect("www.163.com", 80); // 8166
        cf.addListener(f -> {
            if (cf.isSuccess()) {
                lastWriteFuture = cf.channel().write(request);
                lastWriteFuture.addListener(wf -> {
                    if (wf.isSuccess()) {
                        ctx.channel().read();
                    } else {
                        sendErrorAndClose(ctx, wf.cause());
                    }
                });

            } else {
                // send errors
                sendErrorAndClose(ctx, f.cause());
            }
        });
        this.cf = cf;
    }

    private void onReceiveHttpBodyChunk(ChannelHandlerContext ctx, HttpContent httpContent) {
        if (lastWriteFuture == null) {
            buf = new ArrayList<>(1);
        }
        if (httpContent instanceof LastHttpContent) {
            log.debug("receive last http body chunk");
            LastHttpContent lastHttpContent = (LastHttpContent) httpContent;
            HttpHeaders trailingHeaders = lastHttpContent.trailingHeaders();
            contentLen = -1;
            headers = null;
        }
        cf.channel().write(httpContent).addListener(f -> {
            if (f.isSuccess()) {
                ctx.channel().read();
            } else {
                //TODO: should reset connection?
                log.warn("proxy request failed", f.cause());
                ctx.channel().close();
            }
        });
        if (httpContent instanceof LastHttpContent) {
            ctx.channel().flush();
            cf.channel().read();
        }

    }

    private void sendErrorAndClose(ChannelHandlerContext ctx, Throwable t) {
        log.debug("send error response", t);
        sendErrorAndClose(ctx, HttpResponseStatus.INTERNAL_SERVER_ERROR, Strings.nullToEmpty(t.getMessage()));
    }

    private void sendErrorAndClose(ChannelHandlerContext ctx, HttpResponseStatus status, String msg) {
        log.debug("send error response, {}", status);
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, status, Unpooled.wrappedBuffer(msg.getBytes
                ()));
        response.headers().set(CONTENT_TYPE, "text/plain");
        response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
        response.headers().set(CONNECTION, HttpHeaderValues.CLOSE);
        ctx.channel().write(response);
        ctx.channel().flush();
        ctx.channel().close();
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

}