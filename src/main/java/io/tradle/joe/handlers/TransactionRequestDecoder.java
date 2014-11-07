package io.tradle.joe.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.tradle.joe.requests.TransactionRequest;

@Sharable
public class TransactionRequestDecoder extends SimpleChannelInboundHandler<HttpRequest> {
	
	public TransactionRequestDecoder() {
		super(false); // do not release msg object since it's passed through
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		ctx.fireChannelRead(new TransactionRequest(req));
	}
}
