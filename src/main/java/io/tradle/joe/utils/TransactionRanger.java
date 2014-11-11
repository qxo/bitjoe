package io.tradle.joe.utils;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import io.tradle.joe.Joe;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitcoinj.core.Sha256Hash;
import org.bitcoinj.core.StoredBlock;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.store.BlockStore;
import org.bitcoinj.store.BlockStoreException;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;

public class TransactionRanger {

	private final Wallet wallet;
	private final BlockStore store;
	private Integer toDate;
	private Integer fromDate;
	private Integer fromHeight;
	private Integer toHeight;

	public TransactionRanger(Wallet wallet, BlockStore store) {
		this.wallet = checkNotNull(wallet);
		this.store = checkNotNull(store);
	}
	
	/**
	 * @param date - unix timestamp (seconds)
	 * @return same cursor
	 */
	public TransactionRanger fromDate(int date) {
		this.fromDate = date;
//		if (this.toDate == null)
//			this.toDate = (int) (System.currentTimeMillis() / 1000);
		
		return this;
	}

	/**
	 * @param date - unix timestamp (seconds)
	 * @return same cursor
	 */
	public TransactionRanger toDate(int date) {
		this.toDate = date;
		if (this.fromDate == null)
			this.fromDate = 0;
		
		return this;
	}

	public TransactionRanger fromHeight(int height) {
		this.fromHeight = height;
		return this;
	}

	public TransactionRanger toHeight(int height) {
		this.toHeight = height;
		if (this.fromHeight == null)
			this.fromHeight = 0;

		return this;
	}
	
	public List<Transaction> getRange() {
		checkArgument(toDate != null || toHeight != null, "Neither height nor time bounds were set");
		Multimap<Integer, Transaction> timeToTransaction = HashMultimap.create();
		Set<Transaction> transactions = wallet.getTransactions(false);
		for (Transaction t: transactions) {
			Map<Sha256Hash, Integer> appearsInHashes = t.getAppearsInHashes();
			int height, time;
			StoredBlock block = null;
			if (appearsInHashes == null) {
				if (!Joe.isTesting())
					continue;
				
				try {
					block = store.getChainHead();
				} catch (BlockStoreException e) {
					continue;
				}				
			}
			else {
				Sha256Hash mostLikely = getBestHash(appearsInHashes);
				try {
					block = store.get(mostLikely);
				} catch (BlockStoreException e) {
					throw new IllegalStateException("Failed to read from block store");
				}
			}
			
			if (toHeight != null) {
				height = block.getHeight();
				if (height >= fromHeight && height <= toHeight) {
					timeToTransaction.put(height, t);
				}
			}
			else {
				time = (int) block.getHeader().getTimeSeconds();
				if (time >= fromDate && time < toDate) {
					timeToTransaction.put(time, t);
				}
				else
					System.out.println("Time: " + time);
			}
		}
		
		List<Integer> times = new ArrayList<Integer>(timeToTransaction.keySet());
		Collections.sort(times);
		List<Transaction> subset = new ArrayList<Transaction>();
		for (int time: times) {
			Collection<Transaction> txs = timeToTransaction.get(time);
			if (txs != null)
				subset.addAll(txs);
		}
		
		return subset;
	}
	
	private Sha256Hash getBestHash(Map<Sha256Hash, Integer> appearsInHashes) {
		int most = -1;
		Sha256Hash best = null;
		for (Sha256Hash hash: appearsInHashes.keySet()) {
			int appearances = appearsInHashes.getOrDefault(hash, most);
			if (most == -1 || appearances > most) {
				most = appearances;
				best = hash;
			}
		}
		
		return best;
	}
}
