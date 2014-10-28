package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.exceptions.InvalidTransactionRequestException;
import io.tradle.joe.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class RequestFilter extends SimpleChannelInboundHandler<HttpRequest> {

	private final Logger logger = LoggerFactory.getLogger(RequestFilter.class);

	public RequestFilter() {
		super(false); // do not release msg object since it's passed through
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (!"127.0.0.1".equals(Utils.getRemoteIPAddress(ctx)))
			ctx.close();
//		else if (!req.getMethod().equals(HttpMethod.POST))
//			throw new InvalidTransactionRequestException("Transaction request must be a POST request");
		else
			ctx.fireChannelRead(req);
	}
}
