package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.tradle.joe.Joe;
import io.tradle.joe.requests.TransactionRequest;

import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class FundsCheck extends SimpleChannelInboundHandler<TransactionRequest> {
	
	private final Logger logger = LoggerFactory.getLogger(FundsCheck.class);

	public FundsCheck() {
		super(false); // do not release msg object since it's passed through
	}

	@Override
	protected void channelRead0(ChannelHandlerContext ctx, TransactionRequest req) throws Exception {
		Wallet w = Joe.JOE.wallet();
    	Coin fees = Transaction.MIN_NONDUST_OUTPUT
    						   .multiply(req.to().size())
    						   .add(Transaction.REFERENCE_DEFAULT_MIN_TX_FEE);
    	
        Coin balance = w.getBalance();
        System.out.println("Wallet has " + balance.toFriendlyString());
        if (balance.isGreaterThan(fees))
        	ctx.fireChannelRead(req);
        else
        	throw new InsufficientMoneyException(fees.subtract(balance), "Not enough funds to send data, send coins to: " + w.currentReceiveKey().toAddress(w.getParams()));
	}
}
