package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.requests.BootstrapRequest;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.Utils;

import java.util.List;

@Sharable
public class BootstrapRequestHandler extends SimpleChannelInboundHandler<BootstrapRequest> {

	public BootstrapRequestHandler() {
		super(false);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, BootstrapRequest req) throws Exception {
		List<KeyValue> keyValues = req.execute();
		HttpResponseData response = new HttpResponseData(200, Gsons.def().toJson(keyValues));
		Utils.writeResponse(ctx, response);
	}

}
