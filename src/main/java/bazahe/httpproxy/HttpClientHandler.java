package bazahe.httpproxy;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.handler.codec.http.*;
import lombok.extern.log4j.Log4j2;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.annotation.Nullable;

/**
 * Netty http handler, to proxy http request
 *
 * @author Liu Dong
 */
@Log4j2
public class HttpClientHandler extends ChannelInboundHandlerAdapter {

    // -1 means not set
    private long contentLen;
    @Nullable
    private HttpHeaders headers;

    private final Channel proxyChannel;

    public HttpClientHandler(Channel channel) {
        this.proxyChannel = channel;
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        super.channelActive(ctx);
        ctx.channel().read();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        super.channelInactive(ctx);
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        // start of new http request
        if (msg instanceof HttpResponse) {
            onReceiveHttpResponse(ctx, (HttpResponse) msg);
        }

        // the http body chunk
        if (msg instanceof HttpContent) {
            onReceiveHttpBodyChunk(ctx, (HttpContent) msg);
        }
    }

    private void onReceiveHttpResponse(ChannelHandlerContext ctx, HttpResponse response) {
        log.debug("receive new http response: {}", response.status());
        if (HttpUtil.isContentLengthSet(response)) {
            contentLen = HttpUtil.getContentLength(response);
        } else {
            contentLen = -1;
        }
        headers = response.headers();

        proxyChannel.write(response).addListener(f -> {
            if (f.isSuccess()) {
                ctx.channel().read();
            } else {
                ctx.channel().close();
            }
        });
    }

    private void onReceiveHttpBodyChunk(ChannelHandlerContext ctx, HttpContent httpContent) {

        if (httpContent instanceof LastHttpContent) {
            log.debug("receive last http response body chunk");
            LastHttpContent lastHttpContent = (LastHttpContent) httpContent;
            HttpHeaders trailingHeaders = lastHttpContent.trailingHeaders();
            contentLen = -1;
            headers = null;
        }

        if (httpContent instanceof HttpResponse) {
            return;
        }
        proxyChannel.write(httpContent).addListener(f -> {
            if (f.isSuccess()) {
                ctx.channel().read();
            } else {
                ctx.channel().close();
            }
        });
    }


    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) throws Exception {
        ctx.flush();
    }

}