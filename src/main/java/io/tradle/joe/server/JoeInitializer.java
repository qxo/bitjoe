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
import io.tradle.joe.handlers.TransactionRequestDecoder;
import io.tradle.joe.handlers.WebHookRegistrar;

public class JoeInitializer extends ChannelInitializer<SocketChannel> {
	
	private final RequestFilter reqFilter;
	private final WebHookRegistrar webHookRegistrar;
	private final FundsCheck fundsCheck;
	private final TransactionRequestDecoder transactionReqDecoder;
	private final SendToStorage sendToStorage;
	private final DefaultExceptionHandler exceptionHandler;
			
	public JoeInitializer() {
		super();
		reqFilter = new RequestFilter();
		webHookRegistrar = new WebHookRegistrar();
		transactionReqDecoder = new TransactionRequestDecoder();
		fundsCheck = new FundsCheck();
		sendToStorage = new SendToStorage();
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
		 .addLast(webHookRegistrar) 			// handle webhook reg/unreg requests
		 .addLast(transactionReqDecoder)		// parse
		 .addLast(fundsCheck)					// check if we have the funds to pay for the transaction
		 .addLast(sendToStorage)				// send to keeper network, put hash on blockchain
		 .addLast(exceptionHandler);
    }
}
