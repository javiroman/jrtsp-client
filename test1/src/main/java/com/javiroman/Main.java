package com.javiroman;

import com.javiroman.rtsp.RTSPHandler;
import com.javiroman.rtsp.RtspInfo;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;

public class Main {

    private final static Logger LOGGER = LoggerFactory.getLogger(Main.class);

    public static void main(String[] args) {
        if (args.length < 1) {
            System.out.println("params is null");
            return;
        }

        String rtspUrl = args[0];
        RtspInfo info = parseUrl(rtspUrl);
        if (info == null) {
            return;
        }

        LOGGER.info(info.toString());

        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap clientBootstrap = new Bootstrap();
            clientBootstrap.group(group)
                    .option(ChannelOption.TCP_NODELAY, true)
                    .channel(NioSocketChannel.class)
                    .remoteAddress(new InetSocketAddress(info.getIp(), info.getPort()))
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel socketChannel) {
                            socketChannel.pipeline()
                                    .addLast(new RtspEncoder())
                                    .addLast(new RtspDecoder())
                                    .addLast(new RTSPHandler(info.getUrl(),
                                            info.getUsername(),
                                            info.getPassword()));
                        }
                    });

            // The connection to the server peer, blocks until connection is established or error.
            //ChannelFuture f = clientBootstrap.connect().sync();

            // https://stackoverflow.com/questions/15006303/netty-connection-retries
            ChannelFuture f = null;
            while (true)
            {
                LOGGER.info("Waiting for server connection");
                f = clientBootstrap.connect();
                f.awaitUninterruptibly();
                if (f.isSuccess())
                {
                    LOGGER.info("RTSP Connection success!");
                    break;
                }
                Thread.sleep(1000);
            }

            // Wait for the server to close the connection.
            f.channel().closeFuture().sync();

        } catch (Exception e) {
            LOGGER.error("Error ->", e);
            LOGGER.error("<- Error");
        } finally {
            // Shut down executor threads to exit.Ahkk
            try {
                LOGGER.info("ShutdownGracefully the connection group");
                group.shutdownGracefully().sync();
            } catch (InterruptedException e) {
                LOGGER.error("", e);
            }
        }
    }

    private static RtspInfo parseUrl(String rtspUrl) {
        RtspInfo rtspInfo = new RtspInfo();
        try {
            int len = rtspUrl.length();
            int mark = rtspUrl.lastIndexOf("@");
            String first = rtspUrl.substring(0, mark);
            String second = rtspUrl.substring(mark + 1, len);
            String uri = "rtsp://" + second;
            rtspInfo.setUrl(uri);
            String ipInfo = second.substring(0, second.indexOf("/"));
            if (ipInfo.contains(":")) {
                int ipInfoLen = ipInfo.length();
                String ip = ipInfo.substring(0, ipInfo.indexOf(":"));
                String port = ipInfo.substring(ipInfo.indexOf(":") + 1, ipInfoLen);
                rtspInfo.setIp(ip);
                rtspInfo.setPort(Integer.parseInt(port));
            } else {
                rtspInfo.setIp(ipInfo);
            }
            String[] arrays = first.split("//");
            String authSrc = arrays[1];
            String[] arrays2 = authSrc.split(":");
            String username = arrays2[0];
            String password = arrays2[1];
            rtspInfo.setUsername(username);
            rtspInfo.setPassword(password);
            return rtspInfo;
        } catch (Exception e) {
            LOGGER.error("rtspUrl must be 'rtsp://username:password@ip:port/xxx'");
        }
        return null;
    }
}
