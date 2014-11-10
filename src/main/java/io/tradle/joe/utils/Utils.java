package io.tradle.joe.utils;

import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.multipart.Attribute;
import io.netty.handler.codec.http.multipart.HttpPostRequestDecoder;
import io.netty.handler.codec.http.multipart.InterfaceHttpData;
import io.netty.handler.codec.http.multipart.InterfaceHttpData.HttpDataType;
import io.netty.util.CharsetUtil;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpResponse;
import org.apache.http.NameValuePair;
import org.apache.http.client.HttpClient;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.impl.client.HttpClientBuilder;
import org.apache.http.message.BasicNameValuePair;
import org.bitcoinj.core.Address;
import org.bitcoinj.core.AddressFormatException;
import org.bitcoinj.core.Base58;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendRequest;
import org.bitcoinj.core.Wallet.SendResult;
import org.bitcoinj.params.TestNet3Params;

//import io.netty.handler.codec.http.HttpHeaderUtil;

public class Utils {

	public static String TESTNET_FAUCET_RETURN_ADDRESS = "msj42CCGruhRsFrGATiUuh25dtxYtnpbTx";
	
	public static String getRemoteIPAddress(ChannelHandlerContext ctx) {
		String fullAddress = ((InetSocketAddress) ctx.channel().remoteAddress())
				.getAddress().getHostAddress();

		// Address resolves to /x.x.x.x:zzzz we only want x.x.x.x
		if (fullAddress.startsWith("/")) {
			fullAddress = fullAddress.substring(1);
		}

		int i = fullAddress.indexOf(":");
		if (i != -1) {
			fullAddress = fullAddress.substring(0, i);
		}

		return fullAddress;
	}

	/**
	 * sends GET request to specified uri
	 * @param uri - uri to send GET request to
	 * @return response data in the form of an integer code and string response
	 */
	public static HttpResponseData get(URI uri) {
		return executeHttpRequest(new HttpGet(uri));
	}

	/**
	 * sends POST request to specified uri
	 * @param uri - uri to send GET request to
	 * @return response data in the form of an integer code and string response
	 */
	public static HttpResponseData post(String uri, Map<String, String> params) {
		try {
			return post(new URI(uri), params);
		} catch (URISyntaxException e) {
			throw new IllegalArgumentException("malformed uri: " + uri, e);
		}
	}

	/**
	 * sends POST request to specified uri
	 * @param uri - uri to send GET request to
	 * @return response data in the form of an integer code and string response
	 */
	public static HttpResponseData post(URI uri, Map<String, String> params) {
		HttpPost request = new HttpPost(uri);
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		Set<String> paramNames = params.keySet();
		for (String paramName: paramNames) {
			urlParameters.add(new BasicNameValuePair(paramName, params.get(paramName)));
		}
		
		try {
			request.setEntity(new UrlEncodedFormEntity(urlParameters));
		} catch (UnsupportedEncodingException e) {
			// should never happen, but...
			throw new IllegalArgumentException("Failed to build POST request", e);
		}
	
		return executeHttpRequest(request);
	}
	
	public static HttpResponseData executeHttpRequest(HttpUriRequest request) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpResponse response = null;
		String respStr = null;
		int code = -1;
		try {
			response = client.execute(request);
			code = response.getStatusLine().getStatusCode();

			// Get the response
			StringBuilder respSB = new StringBuilder();
			String line;
			BufferedReader rd = new BufferedReader(new InputStreamReader(response.getEntity().getContent()));
			while ((line = rd.readLine()) != null) {
				respSB.append(line);
			}

			respStr = respSB.toString();
		} catch (IOException i) {
			respStr = i.getMessage();
		}

		return new HttpResponseData(code, respStr);
	}
	
	public static void writeResponse(ChannelHandlerContext ctx, HttpResponseData respData) {		
		boolean ok = respData.code() > 0 && respData.code() < 399;
		FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, ok ? OK : BAD_REQUEST,
				Unpooled.copiedBuffer(Gsons.pretty().toJson(respData) + "\n", CharsetUtil.UTF_8));

		response.headers().set(CONTENT_TYPE, "application/json; charset=UTF-8");
		ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
	}
	
	/**
	 * @param postReq - POST request
	 * @return map of parameters to values
	 */
	public static Map<String, String> getPOSTRequestParameters(HttpRequest postReq) {
		if (!postReq.getMethod().equals(HttpMethod.POST))
			throw new IllegalArgumentException("argument must be POST request");
		
		HttpPostRequestDecoder decoder = new HttpPostRequestDecoder(postReq);
		List<InterfaceHttpData> datas = decoder.getBodyHttpDatas();
		Map<String, String> params = getQueryParams(postReq);
		for (InterfaceHttpData data : datas) {
			if (data.getHttpDataType() == HttpDataType.Attribute) {
				Attribute attribute = (Attribute) data;
				try {
					params.put(attribute.getName(), attribute.getValue());
				} catch (IOException i) {
					// should never happen but...
					throw new IllegalStateException("Failed to read POST data", i);
				}
			}
		}
		
		return params;
	}

	/**
	 * parses GET or POST request into param->value map
	 * @param req - GET or POST request
	 * @return param->value map
	 */
	public static Map<String, String> getRequestParameters(HttpRequest req) {
		if (req.getMethod().equals(HttpMethod.POST))
			return getPOSTRequestParameters(req);
		
		return getQueryParams(req);
	}

	private static Map<String, String> getQueryParams(HttpRequest req) {
		Map<String, String> simplifiedParams = new HashMap<String, String>();
		Map<String, List<String>> originalParams = new QueryStringDecoder(req.getUri()).parameters();
		if (originalParams != null) {
			Set<String> paramNames = originalParams.keySet();
			for (String paramName: paramNames) {
				simplifiedParams.put(paramName, originalParams.get(paramName).get(0));
			}
		}
		
		return simplifiedParams;
	}

	public static String toBase64String(byte[] bytes) {
		return new String(Base64.encodeBase64(bytes));
	}

	public static String toBase58String(byte[] bytes) {
		return Base58.encode(bytes);
	}
	
	public static boolean arrayStartsWith(byte[] a, byte[] b) {
		return rangeEquals(a, 0, b, 0, b.length);
	}

	public static boolean rangeEquals(byte[] a, int aStart, byte[] b, int bStart, int length) {
	    assert a.length - aStart > length && b.length - bStart > length;

	    for (int i = aStart, j = bStart, k = 0; k < length; k++) {
	        if (a[i] != b[j])
	            return false;
	    }
	    
	    return true;
	}

	public static boolean isTruthy(String s) {
		return !Utils.isFalsy(s);
	}
	
	public static boolean isFalsy(String s) {
		if (s == null)
			return true;
		
		s = s.toLowerCase();
		return s.equals("0") || 
			   s.equals("false") ||
			   s.equals("n") ||
			   s.equals("no");
	}
	
	/**
	 * @param items
	 * @param delimiter
	 * @return delimiter separated string of items
	 */
	public static <T> String join(Iterable<T> items, String delimiter) {
		Iterator<T> i = items.iterator();
		if (!i.hasNext())
			return "";
		
		StringBuilder sb = new StringBuilder();
		sb.append(i.next());
		while (i.hasNext()) {
			sb.append(delimiter);
			sb.append(i.next());
		}
		
		return sb.toString();
	}

	@SafeVarargs
	public static <T> boolean isOneOf(T t, T... choices) {		
		for (T choice: choices) {
			if (isEqual(t, choice))
				return true;
		}
		
		return false;
	}
	
	public static <T> boolean isEqual(T a, T b) {
		if (a == null)
			return b == null;
		
		return a.equals(b);
	}
	
	public static void getTestnetCoins(long satoshis, String toAddress) {
		Map<String, String> params = new HashMap<String, String>();
		params.put("value", String.valueOf(satoshis));
		params.put("toAddress", toAddress);
		HttpResponseData resp = Utils.post("http://testnet.helloblock.io/v1/faucet/withdrawal", params);
		if (resp.code() != 200)
			throw new IllegalArgumentException("Failed to get testnet coins: " + resp.response());
	}

	public static SendResult returnTestnetCoins(Wallet wallet, long satoshis) throws InsufficientMoneyException {
		if (!wallet.getNetworkParameters().equals(TestNet3Params.get()))
			throw new IllegalArgumentException("Wallet is not on testnet, can't send coins to a different network");
		
		String addr = TESTNET_FAUCET_RETURN_ADDRESS;
		try {
			Address testNetAddr = new Address(wallet.getNetworkParameters(), addr);
			return wallet.sendCoins(SendRequest.to(testNetAddr, Coin.valueOf(satoshis)));
		} catch (AddressFormatException e) {
			// should never happen, but...
			throw new IllegalArgumentException("Invalid address: " + addr);
		}
	}
}
