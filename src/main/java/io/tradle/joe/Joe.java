package io.tradle.joe;

import io.netty.util.CharsetUtil;
import io.tradle.joe.Config.WebHooksConfig;
import io.tradle.joe.events.KeyValue;
import io.tradle.joe.extensions.WebHooksExtension;
import io.tradle.joe.protocols.WebHookProtos.Event;
import io.tradle.joe.sharing.StoragePipe;
import io.tradle.joe.utils.Gsons;
import io.tradle.joe.utils.TransactionUtils;
import io.tradle.joe.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.util.List;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;

public enum Joe {

	JOE;
	
	private static final String CONFIG_PATH = "conf/config.json";
	
	private final Logger logger = LoggerFactory.getLogger(Joe.class);
	private final WalletAppKit kit;
	private final StoragePipe storagePipe;
	private Config config;
	private Wallet wallet;
	private NetworkParameters networkParams;
	private WebHooksExtension webHooks;
	private long DIME = (long) Math.pow(10, 7); // 0.1 BTC
	
	public byte[] getDataPrefix() {
		return config.prefix().getBytes(CharsetUtil.UTF_8);
	}
	
	private Joe() {
        try {
			this.config = Gsons.ugly().fromJson(new BufferedReader(new FileReader(CONFIG_PATH)), Config.class);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("couldn't find config file at path: " + CONFIG_PATH);
		}
        
        networkParams = config.networkParams();
		kit = new WalletAppKit(networkParams, 
								new File(config.wallet().folder()), 
								config.wallet().name()) {
			@Override
	        protected List<WalletExtension> provideWalletExtensions() {
	            return ImmutableList.<WalletExtension>of(
            		new WebHooksExtension()
        		);
	        }
		};
		
		kit.startAsync();
		kit.awaitRunning();
		
		wallet = kit.wallet();
		storagePipe = new StoragePipe(wallet);
        if (TestNet3Params.get().equals(params()) && wallet.getBalance().isLessThan(Coin.CENT)) {
        	try {
        		Utils.getTestnetCoins(DIME, wallet.currentReceiveAddress().toString());
        	} catch (Exception e) {
        		logger.error("Failed to charge wallet from testnet faucet: " + e.getMessage());
        	}
        }
		
		webHooks = (WebHooksExtension) wallet.addOrGetExistingExtension(new WebHooksExtension());
		List<WebHooksConfig> webHookConfigs = config.webHooks();
		if (webHookConfigs != null) {
			for (WebHooksConfig w: webHookConfigs) {
				webHooks.putWebHooks(w.url(), w.events());
			}
		}

		kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            	if (!webHooks.hasHooks(Event.KeyValue))
            		return;
            	
            	System.out.println("\nReceived tx " + tx.getHashAsString());
            	System.out.println(tx.toString());
            	if (TransactionUtils.getSentByMe(wallet, tx).isEmpty() || TransactionUtils.getReceived(wallet, tx).size() > 1) {
            		// if this transaction wasn't sent by me, or sent from me to me
        		
	    			KeyValue keyValue = receiveData(tx);
	    			if (keyValue != null)
	    				webHooks.notifyHooks(Event.KeyValue, keyValue);
            	}
            }

            @Override
            public void onCoinsSent(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
                System.out.println("\nSent tx " + tx.getHashAsString() + " to:");
                for (TransactionOutput o: tx.getOutputs()) {
                    System.out.println(o.getAddressFromP2PKHScript(config.networkParams()));                	
                }
                
                System.out.println(tx.toString());
            }
        });
		
		if (config.allowSpendUnconfirmed())
			kit.wallet().allowSpendingUnconfirmedTransactions();
	}

	public Config config() {
		return config;
	}
	
	public NetworkParameters params() {
		return kit.params();
	}

	public WalletAppKit kit() {
		return kit;
	}
	
	public Wallet wallet() {
		return kit.wallet();
	}
	
	public static boolean isTesting() {
		return !MainNetParams.get().equals(JOE.params());
	}

	public WebHooksExtension webHooks() {
		return webHooks;
	}

	public KeyValue receiveData(Transaction t) {
		return storagePipe.receiveData(t);
	}
}
