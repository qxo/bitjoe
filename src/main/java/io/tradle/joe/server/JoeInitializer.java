package io.tradle.joe.server;

import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.socket.SocketChannel;
import io.netty.handler.codec.http.HttpContentCompressor;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.tradle.joe.handlers.DefaultExceptionHandler;
import io.tradle.joe.handlers.FundsCheck;
import io.tradle.joe.handlers.RequestFilter;
import io.tradle.joe.handlers.SendToStorage;
import io.tradle.joe.handlers.TransactionEncrypter;
import io.tradle.joe.handlers.TransactionFeeHandler;
import io.tradle.joe.handlers.TransactionRequestDecoder;

public class JoeInitializer extends ChannelInitializer<SocketChannel> {
	
	private final RequestFilter reqFilter;
	private final FundsCheck fundsCheck;
	private final TransactionRequestDecoder transactionReqDecoder;
	private final TransactionEncrypter transactionEncrypter;
	private final SendToStorage sendToStorage;
	private final TransactionFeeHandler feeHandler;
	private final DefaultExceptionHandler exceptionHandler;

	public JoeInitializer() {
		super();
		reqFilter = new RequestFilter();
		fundsCheck = new FundsCheck();
		transactionReqDecoder = new TransactionRequestDecoder();
		transactionEncrypter = new TransactionEncrypter();
		sendToStorage = new SendToStorage();
		feeHandler = new TransactionFeeHandler();
		exceptionHandler = new DefaultExceptionHandler();
	}
	
    @Override
    public void initChannel(SocketChannel ch) {
       final ChannelPipeline p = ch.pipeline()
		 .addLast(new HttpRequestDecoder())
		 .addLast(new HttpObjectAggregator(1048576))
		 .addLast(new HttpResponseEncoder())
		 .addLast(new HttpContentCompressor())
		 .addLast(reqFilter) 					// filter out requests from remote ips, non-POST requests, etc.
//		 .addLast(new RemoteIPFilter())
		 .addLast(fundsCheck)					// check if we have the funds to pay for the transaction
		 .addLast(transactionReqDecoder)		// parse
		 .addLast(transactionEncrypter)			// encrypt
		 .addLast(sendToStorage)				// send to keeper network
		 .addLast(feeHandler)					// create bitcoin transaction
		 .addLast(exceptionHandler);
    }
}
