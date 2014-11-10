package io.tradle.joe.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.logging.LogLevel;
import io.netty.handler.logging.LoggingHandler;
import io.tradle.joe.Joe;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.crypto.DeterministicKey;
import org.bitcoinj.utils.BriefLogFormatter;
import org.bitcoinj.wallet.WalletTransaction.Pool;
import org.spongycastle.math.ec.ECPoint;
import org.spongycastle.util.encoders.Hex;

public final class JoeServer {
	
    public static void main(String[] args) throws Exception {
        // This line makes the log output more compact and easily read, especially when using the JDK log adapter.
        BriefLogFormatter.init();
        
        List<DeterministicKey> leafKeys = Joe.JOE.wallet().getActiveKeychain().getLeafKeys();
        int numKeys = leafKeys.size();
        numKeys = Math.min(numKeys, 5);
        System.out.println("Some of my public keys, format:   (public key    :   address)");

        // Print out a few public keys so we can play around with sharing data
        List<DeterministicKey> keys = Joe.JOE.wallet().getActiveKeychain().getLeafKeys();
        for (int i = 0; i < 5; i++) {
        	ECKey key = keys.get(i);
	    	ECPoint pt = key.getPubKeyPoint();
	    	String pubKey = new String(Hex.encode(pt.getEncoded(true)));
	        System.out.println(pubKey + "  :  " + key.toAddress(Joe.JOE.params()));
        }
        
        System.out.println("Wallet balance: " + Joe.JOE.wallet().getBalance().toFriendlyString());
        System.out.println("Send coins to: " + Joe.JOE.wallet().currentReceiveAddress());
//        Wallet wallet = Joe.JOE.wallet();
//        Map<Sha256Hash, Transaction> dead = wallet.getTransactionPool(Pool.DEAD);
//        Set<Transaction> transactions = wallet.getTransactions(true);
//        for (Transaction t: transactions) {
//        	if (t.isPending())
//        		t = t;
//        }
        
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            ServerBootstrap b = new ServerBootstrap();
            b.group(bossGroup, workerGroup)
             .channel(NioServerSocketChannel.class)
             .handler(new LoggingHandler(LogLevel.INFO))
             .childHandler(new JoeInitializer());

            Channel ch = b.bind(Joe.JOE.config().address().port()).sync().channel();

            System.err.println("Open your web browser and navigate to " + Joe.JOE.config().address() + "create.json?data={json-formatted-data}");

            ch.closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
	