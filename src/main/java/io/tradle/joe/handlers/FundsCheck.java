package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.tradle.joe.Joe;
import io.tradle.joe.exceptions.NotEnoughFundsException;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class FundsCheck extends SimpleChannelInboundHandler<HttpRequest> {
	
	private final Logger logger = LoggerFactory.getLogger(FundsCheck.class);

	public FundsCheck() {
		super(false);
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		Wallet w = Joe.JOE.wallet();
    	Coin fees = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.add(Joe.JOE.appFee());
        Coin balance = w.getBalance();	
        
        System.out.println("Wallet has " + balance.toFriendlyString());
        if (balance.isGreaterThan(fees))
        	ctx.fireChannelRead(req);
        else
        	throw new NotEnoughFundsException("Not enough funds to send data, send coins to: " + w.currentReceiveKey().toAddress(w.getParams()));
	}
}
