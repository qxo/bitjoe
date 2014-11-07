package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.tradle.joe.requests.WebHookRequest;
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
		if (!"127.0.0.1".equals(Utils.getRemoteIPAddress(ctx))) {
			ctx.close();
			return;
		}
		
		QueryStringDecoder qsd = new QueryStringDecoder(req.getUri());
		String path = qsd.path();
		if (path.startsWith("hooks")) {
			ctx.fireChannelRead(new WebHookRequest(req));
			return;
		}
		
		ctx.fireChannelRead(req);
	}
}
