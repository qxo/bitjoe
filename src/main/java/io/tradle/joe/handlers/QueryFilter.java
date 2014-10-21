package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class QueryFilter extends SimpleChannelInboundHandler<HttpRequest> {

	private static final String[] REQ_PARAMS = { "data" };//, "to" };
	
	private final Logger logger = LoggerFactory.getLogger(QueryFilter.class);
	
	public QueryFilter() {
		super(false); // do not release msg object since it's passed through
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		QueryStringDecoder qsd = new QueryStringDecoder(req.getUri());
		Map<String, List<String>> params = qsd.parameters();
		for (String p: REQ_PARAMS) {
			if (!params.containsKey(p)) {
				ctx.close();
				return;
			}
		}
		
		ctx.fireChannelRead(req);
	}
}
