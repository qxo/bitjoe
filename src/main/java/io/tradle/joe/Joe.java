package io.tradle.joe;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.util.CharsetUtil;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.TransactionUtils;
import io.tradle.joe.utils.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.net.URISyntaxException;
import java.util.List;

import org.bitcoinj.core.AbstractWalletEventListener;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.common.collect.ImmutableList;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public enum Joe {

	JOE;
	
	private static final String CONFIG_PATH = "conf/config.json";
	
	public static final String TEST_NET_RETURN_ADDRESS = "msVmFrtQ6Z9pxxQmQn7UgKuNikCYNYJf9G";
	
	private final Logger logger = LoggerFactory.getLogger(Joe.class);
	private final WalletAppKit kit;
	private final Coin APP_FEE;
//	private final Address APP_ADDRESS;
//	private final String password;
	private KeyParameter sharedSecret;
	private Config config;

	public byte[] getDataPrefix() {
		return config.prefix().getBytes(CharsetUtil.UTF_8);
	}
	
	private Joe() {
		Gson gson = new GsonBuilder().create();
        try {
			this.config = gson.fromJson(new BufferedReader(new FileReader(CONFIG_PATH)), Config.class);
		} catch (FileNotFoundException e) {
			throw new IllegalStateException("couldn't find config file at path: " + CONFIG_PATH);
		}
        
		kit = new WalletAppKit(config.networkParams(), 
								new File(config.wallet().folder()), 
								config.wallet().name()) {
			@Override
	        protected List<WalletExtension> provideWalletExtensions() {
	            return ImmutableList.<WalletExtension>of(
            		new FileKeysExtension()
        		);
	        }
		};
		
		kit.startAsync();
		kit.awaitRunning();
		
//		kit.chain().addListener(new BlockMonitor());
		kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            	JOE.extractDataFromTransaction(tx);
            }
        });
		
		if (config.allowSpendUnconfirmed())
			kit.wallet().allowSpendingUnconfirmedTransactions();
		
		APP_FEE = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(3);
	}

	public Config config() {
		return config;
	}
	
	public Coin txCost() {
		return APP_FEE;
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
	
	/**
	 * @param from
	 * @param to
	 * @return
	 */
	public KeyParameter getSharedSecretKeyParameter(ECKey from, ECKey to) {
		// TODO: implement separate store of keys used for encryption
		return null;
	}

	public static boolean isOnTestnet() {
		return JOE.params().equals(TestNet3Params.get());
	}

	public void extractDataFromTransaction(Transaction tx) {
		Joe joe = Joe.JOE;
		Config config = joe.config();
		byte[] dataBytes = TransactionUtils.getDataFromTransaction(tx, true);
		String data = TransactionUtils.transactionDataToString(dataBytes);
        
        QueryStringEncoder qs = new QueryStringEncoder(config.keepers().get(0).toString());
        qs.addParam("key", data);
        
        HttpResponseData response = null;
        try {
			response = Utils.get(qs.toUri());
        } catch (URISyntaxException e) {
			logger.error("Constructed bad URI: " + qs, e);
			return;
		} 
        
		if (response.code() > 399) {
			System.err.println("Hash not found in storage: " + data);
			return;
		}
			
		System.out.println("Hash found in storage!");
		System.out.println("Key: " + data);
		System.out.println("Value: " + response.response());
		
		KeyParameter keyParameter = joe.getSharedSecretKeyParameter(null, null);
		boolean isEncrypted = keyParameter != null;
		byte[] decrypted = null;
		byte[] encrypted = response.response().getBytes(CharsetUtil.UTF_8);
		
		if (isEncrypted) {
			try {
				decrypted = AESUtils.decrypt(encrypted, keyParameter, AESUtils.AES_INITIALISATION_VECTOR);
			} catch (Exception e) {
				logger.error("Failed to decrypt data: " + response.response(), e);
				return;
			}
		}
		else
			decrypted = encrypted;
		
		String originalData = new String(decrypted, CharsetUtil.UTF_8);
		if (isEncrypted)
			System.out.println("Decrypted data from storage: " + originalData);
		else
			System.out.println("Retrieved unencrypted data from storage: " + originalData);				
	}
}
