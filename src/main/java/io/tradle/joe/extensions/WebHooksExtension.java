package io.tradle.joe.extensions;

import io.netty.handler.codec.http.HttpResponseStatus;
import io.tradle.joe.Joe;
import io.tradle.joe.protocols.WebHookProtos.Event;
import io.tradle.joe.protocols.WebHookProtos.WebHook;
import io.tradle.joe.protocols.WebHookProtos.WebHooks;
import io.tradle.joe.utils.HttpResponseData;
import io.tradle.joe.utils.Utils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.WalletExtension;

import com.google.common.collect.HashMultimap;
import com.google.common.collect.Multimap;
import com.google.gson.JsonElement;

public class WebHooksExtension implements WalletExtension {

	public static final String EXTENSION_ID = WebHooksExtension.class.getName();
	
	private final Multimap<String, WebHook> urlToWebHooks = HashMultimap.create();
	private final Multimap<Event, WebHook> eventToWebHooks = HashMultimap.create();
	
	/**
	 * Issue POST request to registered web hooks, and unregister them if they return an error code
	 * @param hooks
	 * @param postParams
	 */
	private void notifyHooks(Collection<WebHook> hooks, Map<String, String> postParams) {
		int maxTimeouts = Joe.JOE.config().webHookMaxTimeouts();
		List<WebHook> replacements = null;
		List<WebHook> remove = null;
		for (WebHook hook: hooks) {
			HttpResponseData resp = Utils.post(hook.getUrl(), postParams);
			if (resp.code() > 399) {
				if (remove == null)
					remove = new ArrayList<WebHook>();
					
				remove.add(hook);
			}
			else if (resp.code() < 0 || resp.code() == HttpResponseStatus.REQUEST_TIMEOUT.code()) {
				int newTimeoutsCount = hook.getTimeoutsCount() + 1;
				if (newTimeoutsCount > maxTimeouts)
					remove.add(hook);
				else {
					if (replacements == null)
						replacements = new ArrayList<WebHook>();

					if (remove == null)
						remove = new ArrayList<WebHook>();
					
					remove.add(hook);
					replacements.add(WebHook.newBuilder(hook)
											 .setTimeoutsCount(newTimeoutsCount)
											 .build());
				}
					
			}
		}
		
		if (remove != null)
			remove(remove);
		
		if (replacements != null)
			putWebHooks(replacements);
	}

	public void notifyHooks(Event event, JsonElement data) {
		Collection<WebHook> eventHooks = eventToWebHooks.get(event);
		if (eventHooks == null || eventHooks.isEmpty())
			return;
		
		Map<String, String> params = new HashMap<String, String>();
		params.put("data", data.toString());
		notifyHooks(eventHooks, params);
	}
	
	private void remove(Collection<WebHook> hooks) {
		urlToWebHooks.removeAll(hooks);
		eventToWebHooks.removeAll(hooks);
	}

	@Override
	public String getWalletExtensionID() {
		return EXTENSION_ID;
	}

	@Override
	public boolean isWalletExtensionMandatory() {
		return false;
	}

	@Override
	public byte[] serializeWalletExtension() {
		return WebHooks.newBuilder()
//					   .addAllWebHooks(urlToWebHooks.values())
					   .build()
					   .toByteArray();
	}

	@Override
	public void deserializeWalletExtension(Wallet containingWallet, byte[] data) throws Exception {
		List<WebHook> hooks = WebHooks.parseFrom(data).getWebHooksList();
		for (WebHook hook: hooks) {
			putWebHook(hook);
		}
	}

	private void putWebHook(WebHook hook) {
		Collection<WebHook> forUrl = urlToWebHooks.get(hook.getUrl());
		if (forUrl != null && !forUrl.isEmpty()) {
			for (WebHook h: forUrl) {
				if (h.getEvent() == hook.getEvent()) {
					return;
				}
			}
		}
		
		urlToWebHooks.put(hook.getUrl(), hook);
		eventToWebHooks.put(hook.getEvent(), hook);
	}

	public void putWebHooks(String url, Event[] events) {
		for (Event e: events) {
			putWebHook(url, e);
		}
	}

	public void putWebHook(String url, Event event) {
		putWebHook(WebHook.newBuilder()
						  .setEvent(event)
						  .setUrl(url)
						  .build());
	}

	public void putWebHooks(Collection<WebHook> hooks) {
		for (WebHook hook: hooks) {
			putWebHook(hook);
		}
	}

	public Collection<WebHook> getWebHooksForUrl(String url) {
		return urlToWebHooks.get(url);
	}

	public Collection<WebHook> getWebHooksForEvent(Event event) {
		return eventToWebHooks.get(event);
	}

	public void clear() {
		urlToWebHooks.clear();
		eventToWebHooks.clear();
	}
	
	public void removeAll(String url) {
		Collection<WebHook> removed = urlToWebHooks.removeAll(url);
		if (removed != null && !removed.isEmpty()) {
			for (WebHook w: removed) {
				eventToWebHooks.remove(w.getEvent(), w);
			}
		}
	}

	public void remove(String url, String event) {
		if (event == null) {
			removeAll(url);
			return;
		}
		
		Collection<WebHook> hooks = urlToWebHooks.get(url);
		if (hooks != null) {
			Iterator<WebHook> hooksForUrl = hooks.iterator();
			while (hooksForUrl.hasNext()) {
				WebHook next = hooksForUrl.next();
				if (next.getEvent().equals(event))
					hooksForUrl.remove();
			}
		}
	}

	public boolean hasHooks() {
		return !urlToWebHooks.isEmpty();
	}

//	private void init() {
//		Joe.JOE.addDataListener(new AbstractDataTransactionListener() {
//			@Override
//			public void onKeyReceived(Transaction tx, String key) {
//				Collection<WebHook> newKeyHooks = eventToWebHooks.get(Event.NewKey);
//				if (newKeyHooks == null || newKeyHooks.isEmpty())
//					return;
//				
//				Map<String, String> params = new HashMap<String, String>();
//				params.put("data", key);
//				
//				notifyHooks(newKeyHooks, params);
//			}
//			
//			@Override
//			public void onValueReceived(Transaction tx, String key, String value) {
//				Collection<WebHook> newValHooks = eventToWebHooks.get(Event.NewValue);
//				if (newValHooks == null || newValHooks.isEmpty())
//					return;
//				
//				Map<String, String> params = new HashMap<String, String>();
//				JsonObject keyVal = new JsonObject();
//				keyVal.addProperty("key", key);
//				keyVal.addProperty("value", value);
//				params.put("data", keyVal.toString());
//				
//				notifyHooks(newValHooks, params);
//			}
//		});	
//	}
}
