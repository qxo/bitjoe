package io.tradle.joe;

import java.io.File;
import java.nio.charset.Charset;
import java.util.List;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;
import org.bitcoinj.crypto.KeyCrypter;
import org.bitcoinj.crypto.KeyCrypterScrypt;
import org.bitcoinj.kits.WalletAppKit;
import org.bitcoinj.params.TestNet3Params;
import org.bitcoinj.protocols.channels.StoredPaymentChannelClientStates;
import org.spongycastle.crypto.params.KeyParameter;
import org.spongycastle.util.Arrays;

import com.google.common.collect.ImmutableList;

public enum Joe {

	TestNet(TestNet3Params.get(), "testJoe");
	
	public static final int MAX_DATA_LENGTH = 40;

	private static final Charset CHARSET = Charset.forName("UTF-8");
	private static byte[] PREFIX_BYTES = "TRADLE ".getBytes(CHARSET);

	private final WalletAppKit kit;
	private final Coin APP_FEE;
	private final Address APP_ADDRESS;
	private final String password;
	private final String walletName;
	private KeyParameter keyParameter;

	public static byte[] getDataPrefix() {
		return Arrays.clone(PREFIX_BYTES);
	}
	
	private Joe(NetworkParameters net, String walletName, String password) {
		this.walletName = walletName;
		this.password = password;
		
		kit = new WalletAppKit(net, new File("."), walletName) {
			@Override
	        protected List<WalletExtension> provideWalletExtensions() {
	            return ImmutableList.<WalletExtension>of(new FileKeysExtension());
	        }
		};
		
		kit.startAsync();
		kit.awaitRunning();
		
		if (password != null) {
			KeyCrypter keyCrypter = kit.wallet().getKeyCrypter();
			if (keyCrypter == null) {	
	            final KeyCrypterScrypt scrypt = new KeyCrypterScrypt();
	            keyParameter = scrypt.deriveKey(password);
				kit.wallet().encrypt(scrypt, keyParameter);
				keyCrypter = kit.wallet().getKeyCrypter();
			}
			
			keyParameter = keyCrypter.deriveKey(password);
			if (!kit.wallet().isEncrypted())
				kit.wallet().encrypt(password);
		}
			
		APP_FEE = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.multiply(3);
		APP_ADDRESS = kit.wallet().currentReceiveAddress(); // while testing, just send back to self (for all apps) 
	}

	private Joe(NetworkParameters net, String walletName) {
		this(net, walletName, null);
	}

	public Coin appFee() {
		return APP_FEE;
	}

	public Address appAddress() {
		return APP_ADDRESS;
	}
	
	public Address appAddress(String appName) {
		return APP_ADDRESS;
	}
	
	public Charset charset() {
		return CHARSET;
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
	
	public String walletName() {
		return walletName;
	}
	
	public String password() {
		return password;
	}

	public KeyParameter keyParameter() {
		return keyParameter;
	}

}
