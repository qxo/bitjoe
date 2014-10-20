package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class RemoteIPFilter extends SimpleChannelInboundHandler<HttpRequest> {

	private final Logger logger = LoggerFactory.getLogger(RemoteIPFilter.class);
	
	public RemoteIPFilter() {
		super(false);
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (!"127.0.0.1".equals(Utils.getRemoteIPAddress(ctx)))
			ctx.close();
		else
			ctx.fireChannelRead(req);
	}
	
	@Override
	public void channelReadComplete(ChannelHandlerContext ctx) {
		ctx.flush();
	}
	
	@Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
		logger.error("exception in IP Filter", cause);
		ctx.close();
	}

}
