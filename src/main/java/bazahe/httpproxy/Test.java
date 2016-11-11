package bazahe.httpproxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelOption;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;

/**
 * @author Liu Dong
 */
public class Test {
    public static void main(String[] args) {
        Bootstrap bootstrap = new Bootstrap();
        bootstrap.group(new NioEventLoopGroup())
                .channel(NioSocketChannel.class)
                .handler(new HttpClientHandler(null))
                .option(ChannelOption.AUTO_READ, false);

        ChannelFuture cf = bootstrap.connect("www.163.com", 80);
        cf.addListener(f -> {
            if (cf.isSuccess()) {
//                cf.channel().write(request).addListener(wf -> {
//                    if (wf.isSuccess()) {
//                        ctx.channel().read();
//                    } else {
//                        sendErrorAndClose(ctx, f.cause());
//                    }
//                });
                System.out.println("bbbb");

            } else {
                // send errors
                System.out.println("aaaa");
            }
        });
    }
}
