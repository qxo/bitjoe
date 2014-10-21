package io.tradle.joe;

import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.util.CharsetUtil;
import io.tradle.joe.utils.AESUtils;
import io.tradle.joe.utils.ECUtils;
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
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.ECKey;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.TransactionOutput;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.wallet.KeyChain.KeyPurpose;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.Arrays;

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
	            return ImmutableList.<WalletExtension>of(new FileKeysExtension());
	        }
		};
		
		kit.startAsync();
		kit.awaitRunning();
		
		kit.wallet().addEventListener(new AbstractWalletEventListener() {
            @Override
            public void onCoinsReceived(Wallet wallet, Transaction tx, Coin prevBalance, Coin newBalance) {
            	List<TransactionOutput> txOuts = tx.getOutputs();
            	NetworkParameters params = config.networkParams();
            	Address receivedAt = null;
            	Address from = null;
            	byte[] dataBytes = null;
            	for (TransactionOutput o: txOuts) {
            		if (o.getValue().isZero()) {
            			dataBytes = TransactionUtils.scriptToMetadata(o.getScriptBytes());
            		}
            		else if (o.isMine(wallet)) {
            			receivedAt = o.getAddressFromP2PKHScript(params);
//            			from = o.getSpentBy().getFromAddress();
            			from = tx.getInputs().get(0).getFromAddress();
            		}
            	}
            	
            	
            	
            	if (dataBytes == null)
            		return;
            	
                String data = Base58.encode(Arrays.copyOfRange(dataBytes, getDataPrefix().length, dataBytes.length));
                
                QueryStringEncoder qs = new QueryStringEncoder(config.keepers().get(0).toString() + "keeper");
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
		
				KeyParameter keyParameter = JOE.getSharedSecretKeyParameter(null, null);
		    	byte[] decrypted = null;
				try {
					decrypted = AESUtils.decrypt(Base58.decode(response.response()), keyParameter, AESUtils.AES_INITIALISATION_VECTOR);
				} catch (AddressFormatException e) {
					logger.error("Failed to decrypt data, wasn't in expected base58 format: " + response.response(), e);
					return;
				}
				
				String originalData = new String(decrypted, CharsetUtil.UTF_8);
				System.out.println("Decrypted data from storage: " + originalData);
				
//				JsonParser parser = new JsonParser();
//		    	JsonElement json = parser.parse(originalData);
            }
        });
		
		if (config.allowSpendUnconfirmed())
			kit.wallet().allowSpendingUnconfirmedTransactions();
		
//		if (password != null) {
//			KeyCrypter keyCrypter = kit.wallet().getKeyCrypter();
//			if (keyCrypter == null) {	
//	            final KeyCrypterScrypt scrypt = new KeyCrypterScrypt();
//	            keyParameter = scrypt.deriveKey(password);
//				kit.wallet().encrypt(scrypt, keyParameter);
//				keyCrypter = kit.wallet().getKeyCrypter();
//			}
//			
//			keyParameter = keyCrypter.deriveKey(password);
//			if (!kit.wallet().isEncrypted())
//				kit.wallet().encrypt(password);
//		}
			
		APP_FEE = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(3);
//		APP_ADDRESS = kit.wallet().currentReceiveAddress(); // while testing, just send back to self (for all apps) 
	}

	public Config config() {
		return config;
	}
	
//	public Coin appFee() {
//		return APP_FEE;
//	}
//
//	public Address appAddress() {
//		return APP_ADDRESS;
//	}
//	
//	public Address appAddress(String appName) {
//		return APP_ADDRESS;
//	}
	
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
	
//	public String walletName() {
//		return walletName;
//	}
//	
//	public String password() {
//		return password;
//	}
//
//	public KeyParameter keyParameter() {
//		return keyParameter;
//	}

	public KeyParameter getSharedSecretKeyParameter(ECKey from, ECKey to) {
		// HACK:
		if (sharedSecret == null) {
			ECKey change = JOE.wallet().currentKey(KeyPurpose.CHANGE);
			ECKey receive = JOE.wallet().currentKey(KeyPurpose.RECEIVE_FUNDS);
			sharedSecret = ECUtils.getSharedSecretKeyParameter(change.getPubKeyPoint(), receive.getPrivKey());
		}
		
		return sharedSecret;
	}
}
