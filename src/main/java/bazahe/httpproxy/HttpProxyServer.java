package bazahe.httpproxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.util.concurrent.GlobalEventExecutor;
import lombok.Setter;
import lombok.SneakyThrows;
import lombok.extern.log4j.Log4j2;

import java.util.Objects;

/**
 * Http server using netty
 *
 * @author Liu Dong
 */
@Log4j2
public class HttpProxyServer {

    private final int port;
    private final String host;

    private EventLoopGroup masterGroup;
    private EventLoopGroup workerGroup;
    private ServerBootstrap bootstrap;
    private final ChannelGroup channelGroup = new DefaultChannelGroup("HTTP-Proxy-Server", GlobalEventExecutor
            .INSTANCE);


    private int state;

    private ChannelFuture closeFuture;

    public HttpProxyServer(String host, int port) {
        this.host = host;
        this.port = port;
    }

    public HttpProxyServer(int port) {
        this("", port);
    }

    /**
     * Start Netty http server
     */
    @SneakyThrows
    public HttpProxyServer start() {
        if (this.state != 0) {
            throw new IllegalStateException();
        }

        masterGroup = new NioEventLoopGroup();
        workerGroup = new NioEventLoopGroup();
        bootstrap = new ServerBootstrap();
        bootstrap.group(masterGroup, workerGroup).channel(NioServerSocketChannel.class);
        bootstrap.childHandler(new ChannelInitializer<SocketChannel>() {
            @Override
            public void initChannel(SocketChannel ch) throws Exception {
                ch.pipeline().addLast(new HttpResponseEncoder());
                ch.pipeline().addLast(new HttpRequestDecoder());
                ch.pipeline().addLast(new HttpProxyHandler());
            }
        });
        bootstrap.option(ChannelOption.SO_BACKLOG, 128);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        // set auto read to false, to avoid client writes a lot of data before the target server can consume it
        bootstrap.childOption(ChannelOption.AUTO_READ, false);

        ChannelFuture bindFuture;
        if (host.isEmpty()) {
            bindFuture = bootstrap.bind(port).sync();
        } else {
            bindFuture = bootstrap.bind(host, port).sync();
        }
        log.info("start http proxy at :{}", port);
        closeFuture = bindFuture.channel().closeFuture();
        closeFuture.addListener(f -> {
            masterGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        });
        return this;
    }

    /**
     * Waiting for stop
     */
    @SneakyThrows
    public HttpProxyServer join() {
        closeFuture.sync();
        return this;
    }

    /**
     * Stop the http server
     */
    @SneakyThrows
    public void stop() {
        //TODO:
        channelGroup.close().sync();
    }

    public static void main(String[] args) {
        new HttpProxyServer(2048).start().join();
    }
}
