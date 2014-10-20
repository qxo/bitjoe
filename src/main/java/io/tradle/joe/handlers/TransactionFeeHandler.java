package io.tradle.joe.handlers;

import static com.google.common.base.Preconditions.checkNotNull;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.util.CharsetUtil;
import io.tradle.joe.Config;
import io.tradle.joe.Joe;
import io.tradle.joe.TransactionRequest;
import io.tradle.joe.exceptions.InvalidTransactionRequestException;
import io.tradle.joe.utils.TransactionUtils;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendRequest;
import org.bitcoinj.kits.WalletAppKit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class TransactionFeeHandler extends SimpleChannelInboundHandler<StorageTransaction> {
	
	private final Logger logger = LoggerFactory.getLogger(TransactionFeeHandler.class);
    
    /** Buffer that stores the response content */
    private final StringBuilder buf = new StringBuilder();
    
    @Override
    protected void channelRead0(ChannelHandlerContext ctx, StorageTransaction t) {        
    	Joe joe = Joe.JOE;
    	Config config = joe.config();
    	TransactionRequest req = t.httpRequest();
    	
    	byte[] data = t.getHash();
    	byte[] prefixBytes = joe.getDataPrefix();
    	int totalLength = prefixBytes.length + data.length;
    	if (totalLength > config.maxDataBytes())
    		throw new InvalidTransactionRequestException("Data too long by " + (config.maxDataBytes() - prefixBytes.length - data.length) + " bytes");
    	
    	WalletAppKit kit = joe.kit();
    	Address to = joe.appAddress();
    	byte[] appAndData = new byte[Math.min(config.maxDataBytes(), totalLength)];
    	System.arraycopy(prefixBytes, 0, appAndData, 0, prefixBytes.length);
    	System.arraycopy(data, 0, appAndData, prefixBytes.length, data.length);
    	
    	Wallet.SendRequest sendReq = SendRequest.to(to, joe.appFee());
    	TransactionUtils.addDataToTransaction(sendReq.tx, appAndData);
    	
    	Wallet.SendResult result;
		try {
			result = kit.wallet().sendCoins(kit.peerGroup(), sendReq);
			checkNotNull(result);
			System.out.println("Sent coins to: " + to + ", transaction hash: " + result.tx.toString());
		} catch (InsufficientMoneyException e) {
			fail(ctx, req, e.getMessage());
			return;
		}
		
		ok(ctx, req);
	}
	
//	private static void send100Continue(ChannelHandlerContext ctx) {
//        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
//        ctx.write(response);
//    }

	private void ok(ChannelHandlerContext ctx, TransactionRequest req) {
		buf.setLength(0);
		buf.append("OK");
		writeResponse(ctx, req.httpRequest());
	}
	
	private void fail(ChannelHandlerContext ctx, TransactionRequest req, String msg) {
		req.httpRequest().setDecoderResult(DecoderResult.failure(new Exception(msg)));
    	buf.setLength(0);
//    	buf.append(e.getMessage());
		writeResponse(ctx, req.httpRequest());
	}

    private boolean writeResponse(ChannelHandlerContext ctx, HttpRequest req) {
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, req.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
        ctx.write(response);
        return false;
    }

}
