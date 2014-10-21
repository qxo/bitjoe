package io.tradle.joe.handlers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.util.CharsetUtil;
import io.tradle.joe.TransactionRequest;

@Sharable
public class TransactionEncrypter extends SimpleChannelInboundHandler<TransactionRequest> {
	
	private final Logger logger = LoggerFactory.getLogger(TransactionEncrypter.class);
	
	public TransactionEncrypter() {
		super(false); // do not release msg object since it's passed through
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TransactionRequest req) throws Exception {
    	String data = req.data().toString();
    	byte[] unencryptedBytes = data.getBytes(CharsetUtil.UTF_8);    	
    	ctx.fireChannelRead(new StorageTransaction(req, unencryptedBytes));
	}
}
