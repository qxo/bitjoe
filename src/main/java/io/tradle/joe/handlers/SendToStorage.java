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
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Joe;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.extensions.WebHooksExtension;
import io.tradle.joe.protocols.WebHookProtos.Event;
import io.tradle.joe.requests.TransactionRequest;
import io.tradle.joe.responses.TransactionResponse;
import io.tradle.joe.sharing.Permission;
import io.tradle.joe.sharing.ShareRequest;
import io.tradle.joe.sharing.ShareResult;
import io.tradle.joe.utils.Gsons;

import java.util.Map;

import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

@Sharable
public class SendToStorage extends SimpleChannelInboundHandler<TransactionRequest> {

	private final Logger logger = LoggerFactory.getLogger(SendToStorage.class);

	public SendToStorage() {
		super(false); // do not release msg object since it's passed through
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TransactionRequest req) throws Exception {
		JsonObject jResp = new JsonObject();
		JsonObject jResults = new JsonObject();
		
		byte[] data = req.data().toString().getBytes(CharsetUtil.UTF_8);
		
		WebHooksExtension webHooks = Joe.JOE.webHooks();
		if (webHooks != null)
			webHooks.notifyHooks(Event.KeyValue, new KeyValue(req.key(), req.data()));
		
		Wallet wallet = Joe.JOE.wallet();
		ShareResult share = new ShareRequest(wallet)
											 .store(data)
											 .shareWith(req.to())
											 .cleartext(req.cleartext())
											 .execute();
		
		jResp.add("file", Gsons.ugly().toJsonTree(new TransactionResponse(null, share.fileKey())));		
		
		Map<String, Permission> results = share.results();
		if (!results.isEmpty()) {
			jResp.add("permissions", jResults);
			for (String pubKey: results.keySet()) {
				Permission result = results.get(pubKey);
				TransactionResponse resp = new TransactionResponse(result.sendResult(), result.keyInStorage());
				JsonElement oneResp = Gsons.ugly().toJsonTree(resp, TransactionResponse.class);
				jResults.add(pubKey, oneResp);
			}
		}
		
		writeResponse(ctx, req.httpRequest(), jResp);
	}
	
	private boolean writeResponse(ChannelHandlerContext ctx, HttpRequest req, JsonElement resp) {
		// Build the response object.
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, 
				req.getDecoderResult().isSuccess() ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(Gsons.pretty().toJson(resp) + "\n", CharsetUtil.UTF_8));

		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
		return false;
	}
}
