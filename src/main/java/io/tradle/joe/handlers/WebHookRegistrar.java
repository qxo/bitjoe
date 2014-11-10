package io.tradle.joe.handlers;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Joe;
import io.tradle.joe.extensions.WebHooksExtension;
import io.tradle.joe.requests.WebHookRequest;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.HttpResponseData;

import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class WebHookRegistrar extends SimpleChannelInboundHandler<WebHookRequest> {

	private final Logger logger = LoggerFactory.getLogger(WebHookRegistrar.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, WebHookRequest req) throws Exception {
		Wallet wallet = Joe.JOE.wallet();
		WebHooksExtension webHooks = (WebHooksExtension) wallet.getExtensions().get(WebHooksExtension.EXTENSION_ID);

//		switch (req.type()) {
//		case Clear:
//			webHooks.removeAll(req.url());
//			return;
//		case Create:
			webHooks.putWebHook(req.url(), req.event());
//			return;
//		case Destroy:
//			webHooks.remove(req.url(), req.event());
//			return;
//		}
		
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, req.httpRequest().getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(Gsons.pretty().toJson(new HttpResponseData(200, "OK")), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }
}
