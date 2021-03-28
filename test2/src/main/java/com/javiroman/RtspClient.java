package com.javiroman;

import java.io.UnsupportedEncodingException;

import org.apache.commons.codec.binary.Base64;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.rtsp.RtspDecoder;
import io.netty.handler.codec.rtsp.RtspEncoder;
import io.netty.handler.codec.rtsp.RtspHeaderNames;
import io.netty.handler.codec.rtsp.RtspMethods;
import io.netty.handler.codec.rtsp.RtspVersions;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


public class RtspClient {

    static int seq = 0;
    static final String userAgent = "JAVA_Client";
    private static final String USERNAME = "user";
    private static final String PASSWORD = "pass";
    static String authStringEnc;
    private final static Logger LOGGER = LoggerFactory.getLogger(RtspClient.class);


    public static void setBasicAuth() {
        String authString = USERNAME + ":" + PASSWORD;
        System.out.println("auth string: " + authString);
        byte[] authEncBytes = Base64.encodeBase64(authString.getBytes());
        authStringEnc = new String(authEncBytes);
        System.out.println("Base64 encoded auth string: " + authStringEnc);
    }

    public static void main(String[] args) throws InterruptedException, UnsupportedEncodingException {
        setBasicAuth();
        sendReq("rtsp://10.11.0.152:8554/archive/1e170451117241", false);
    }

    public static void sendReq(String url, boolean isRedirect) throws InterruptedException {
        Channel channel;
        if (!isRedirect)
            channel = start("10.11.0.152", 8554);
        else
            channel = start("10.11.0.152", 9554);

        DefaultHttpRequest request = new DefaultHttpRequest(RtspVersions.RTSP_1_0, RtspMethods.DESCRIBE, url);
        request.headers().add(RtspHeaderNames.CSEQ, getSeqNumber());
        request.headers().add(RtspHeaderNames.USER_AGENT, userAgent);
        request.headers().add(RtspHeaderNames.AUTHORIZATION, "Basic " + authStringEnc);
        request.headers().add(RtspHeaderNames.ACCEPT, "application/sdp");
        LOGGER.info("MIERDA: " + request.toString());
        channel.writeAndFlush(request).sync();
    }

    public static synchronized int getSeqNumber() {
        return ++seq;
    }


    public static Channel start(String ip, int port) {
        Bootstrap rtspClient = new Bootstrap();
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        final ClientHandler handler = new ClientHandler();
        Channel ch = null;

        rtspClient.group(workerGroup).channel(NioSocketChannel.class);
        //rtspClient.option(ChannelOption.SO_KEEPALIVE, true);
        rtspClient.remoteAddress(ip, port);
        rtspClient.handler(new ChannelInitializer<SocketChannel>() {
            protected void initChannel(SocketChannel ch1) {
                ChannelPipeline p = ch1.pipeline();
                p.addLast("encoder", new RtspEncoder());
                p.addLast("decoder", new RtspDecoder());
                p.addLast(handler);
            }
        });

        try {
            ch = rtspClient.connect(ip, port).sync().channel();
        } catch (Exception e) {
            System.out.println("Error " + e);
        }
        return ch;
    }

}
