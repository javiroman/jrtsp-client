package com.javiroman;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultHttpResponse;

public class ClientHandler extends SimpleChannelInboundHandler<DefaultHttpResponse> {
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultHttpResponse msg) throws Exception {
        System.out.println("Response -" + msg.toString());
    }
}
