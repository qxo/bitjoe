package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.tradle.joe.Config;
import io.tradle.joe.Joe;
import io.tradle.joe.exceptions.StorageException;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.Utils;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class SendToStorage extends SimpleChannelInboundHandler<StorageTransaction> {

	private final Logger logger = LoggerFactory.getLogger(SendToStorage.class);

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, StorageTransaction t) throws Exception {
		Config.AddressConfig keeper = Joe.JOE.config().keepers().get(0);
		String key = t.getHashString();
		String value = t.getEncryptedString();
		
		QueryStringEncoder url = new QueryStringEncoder(keeper.toString());
		url.addParam("key", key);
		url.addParam("value", value);
		
		System.out.println("Sending to storage");
		System.out.println("Key: " + key);
		System.out.println("Value: " + value);
		HttpResponseData response = Utils.get(url.toUri());
		if (response.code() > 399)
			throw new StorageException("transaction refused by keeper network: " + response.code() + " " + response.response());
		else
			ctx.fireChannelRead(t);
	}
}
