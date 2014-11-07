package io.tradle.joe;

import io.tradle.joe.protocols.WebHookProtos.Event;

import java.util.List;
import java.util.Map;

import org.bitcoinj.core.NetworkParameters;
import org.bitcoinj.params.MainNetParams;
import org.bitcoinj.params.RegTestParams;
import org.bitcoinj.params.TestNet3Params;

public class Config {

	static enum Net {
		regtest,
		testnet,
		mainnet
	}
	
	private int maxDataBytes;
	private int webHookMaxTimeouts;
	private boolean allowSpendUnconfirmed;
	private Net net;
	private WalletConfig wallet;
	private String prefix;
	private AddressConfig joeAddress;
	private List<AddressConfig> keeperAddresses;
	private List<WebHooksConfig> webHooks;
	private List<String> testIdentities;
	
	public String prefix() {
		return prefix;
	}
	
	public int maxDataBytes() {
		return maxDataBytes;
	}
	
	public boolean allowSpendUnconfirmed() {
		return allowSpendUnconfirmed;
	}
	
	public NetworkParameters networkParams() {
		switch (net) {
		case mainnet:
			return MainNetParams.get();
		case regtest:
			return RegTestParams.get();
		case testnet:
		default:
			return TestNet3Params.get();
		}
	}
	
	public List<AddressConfig> keepers() {
		return keeperAddresses;
	}
	
	public AddressConfig address() {
		return joeAddress;
	}
	
	public List<WebHooksConfig> webHooks() {
		return webHooks;
	}

	public WalletConfig wallet() {
		return wallet;
	}
	
	public static class AddressConfig {
		private String host;
		private String path = "";
		private int port;

		public String address() {
			return host;
		}
		
		public String path() {
			return path;
		}
		
		public int port() {
			return port;
		}
		
		@Override
		public String toString() {
			return host + ":" + port + "/" + path;
		}
	}
	
	public static class WalletConfig {
		String folder;
		String name;

		public String folder() {
			return folder;
		}
		
		public String name() {
			return name;
		}
	}

	public static class WebHooksConfig {
		String url;
		Event[] events;

		public String url() {
			return url;
		}
		
		public Event[] events() {
			return events;
		}
	}
	
	public List<String> testIdentities() {
		return testIdentities;
	}

	public int webHookMaxTimeouts() {
		return webHookMaxTimeouts;
	}
}
