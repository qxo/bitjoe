/*
 * Copyright 2012 The Netty Project
 *
 * The Netty Project licenses this file to you under the Apache License,
 * version 2.0 (the "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at:
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */
package io.tradle.joe;

import static com.google.common.base.Preconditions.checkArgument;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONNECTION;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_LENGTH;
import static io.netty.handler.codec.http.HttpHeaders.Names.CONTENT_TYPE;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.CONTINUE;
import static io.netty.handler.codec.http.HttpResponseStatus.OK;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.DefaultHttpRequest;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.netty.handler.codec.http.QueryStringEncoder;
import io.netty.util.CharsetUtil;

import java.net.InetSocketAddress;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.bitcoinj.core.Address;
import org.bitcoinj.core.Coin;
import org.bitcoinj.core.InsufficientMoneyException;
import org.bitcoinj.core.Transaction;
import org.bitcoinj.core.Wallet;
import org.bitcoinj.core.Wallet.SendRequest;
import org.bitcoinj.kits.WalletAppKit;
import org.h2.security.SHA256;
import org.spongycastle.crypto.params.KeyParameter;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

public class JoeHandler extends SimpleChannelInboundHandler<Object> {
	
    private static final String KEEPER_HOST = "http://127.0.0.1:8080/keeper";

	private HttpRequest req;
    
    /** Buffer that stores the response content */
    private final StringBuilder buf = new StringBuilder();
    private final Joe joe = Joe.TestNet;
    private final WalletAppKit kit = joe.kit();
    private final Wallet wallet = joe.wallet();
    
    @Override
    public void channelReadComplete(ChannelHandlerContext ctx) {
        ctx.flush();
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, Object msg) {
        if (msg instanceof HttpRequest) {
            req = (HttpRequest) msg;
//            HttpMethod method = req.getMethod();
//            if (method == HttpMethod.GET)
//            	get(ctx, req);
//            else if (method == HttpMethod.POST)
//            	post(ctx, req);
            	
            // for now:
            service(ctx);
        }
    }

	private void service(ChannelHandlerContext ctx) {
//		From HttpSnoop example:
//        if (HttpHeaders.is100ContinueExpected(req)) {
//            send100Continue(ctx);
//        }
//
//        buf.setLength(0);
        
        WalletAppKit kit = joe.kit();
        Wallet wallet = joe.wallet();
        
        // check that it's local
        checkArgument("127.0.0.1".equals(getRemoteIPAddress(ctx)));
        
        QueryStringDecoder queryStringDecoder = new QueryStringDecoder(req.getUri());
        String app = queryStringDecoder.path().substring(1);
        if (!app.startsWith("joe/"))
        	return;
        
        app = app.substring(4);
        Map<String, List<String>> params = queryStringDecoder.parameters();
        List<String> dataArg = params.get("data");
        if (dataArg == null) {
        	fail(ctx, "missing required parameter 'data'");
        	return;
        }
        
        if (!checkFunds()) {
        	fail(ctx, "Not enough funds to send data, send coins to: " + kit.wallet().currentReceiveKey().toAddress(kit.params()));
        	return;
        }
    	
    	// TODO: maybe these should be separate handlers?
    	
        // parse
        
    	String data = dataArg.get(0);
    	JsonParser parser = new JsonParser();
    	JsonObject json = (JsonObject)parser.parse(data);
    	JsonArray pubKeys = json.getAsJsonArray("owners");
    	if (pubKeys != null) {
    		// PGP encrypt
    	}
    	
    	// validate tx
    	
    	byte[] unencryptedBytes = data.getBytes();
    	
    	// encrypt & store tx
        // Create an AES encoded version of the unencryptedBytes, using the credentials
    	FileKeysExtension fileKeys = (FileKeysExtension) wallet.getExtensions().get(FileKeysExtension.EXTENSION_ID);
    	KeyParameter keyParameter = new KeyParameter(fileKeys.key().getEncoded());
    	
        byte[] encryptedBytes = AESUtils.encrypt(unencryptedBytes, keyParameter, AESUtils.AES_INITIALISATION_VECTOR);

        // Check that the encryption is reversible
        byte[] rebornBytes = AESUtils.decrypt(encryptedBytes, keyParameter, AESUtils.AES_INITIALISATION_VECTOR);
         
        checkArgument(Arrays.equals(unencryptedBytes, rebornBytes), "The encryption was not reversible so aborting.");
    	
    	// hash tx
    	byte[] hash = SHA256.getHash(encryptedBytes, true);        	
        	
    	// send to storage    	
    	try {
    		storeFile(hash, encryptedBytes);
    	} catch (Exception e) {
    		fail(ctx, "Failed to store file: " + e.getMessage());
    		return;
    	}
    	
    	// send tx
        sendData(ctx, hash, kit.wallet().currentReceiveAddress()); // send to self
    	
//        if (!params.isEmpty()) {
//            for (Entry<String, List<String>> p: params.entrySet()) {
//                String key = p.getKey();
//                List<String> vals = p.getValue();
//                for (String val : vals) {
//                    buf.append("PARAM: ").append(key).append(" = ").append(val).append("\r\n");
//                }
//            }
//            buf.append("\r\n");
//        }
//
//        appendDecoderResult(buf, request);        
	}

	private void storeFile(byte[] hash, byte[] encryptedBytes) throws Exception {
		QueryStringEncoder url = new QueryStringEncoder(KEEPER_HOST);
		url.addParam("key", new String(hash, joe.charset()));
		url.addParam("value", new String(encryptedBytes, joe.charset()));
		Utils.request(HttpMethod.POST, url.toUri());
	}

	private void fail(ChannelHandlerContext ctx, String msg) {
    	buf.setLength(0);
    	buf.append(msg);
		writeResponse(req, ctx);
	}

	private static String getRemoteIPAddress(ChannelHandlerContext ctx) {
        String fullAddress = ((InetSocketAddress) ctx.channel().remoteAddress()).getAddress().getHostAddress();
        
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
	
    private boolean writeResponse(HttpObject currentObj, ChannelHandlerContext ctx) {
        // Decide whether to close the connection or not.
        boolean keepAlive = HttpHeaders.isKeepAlive(req);
        // Build the response object.
        FullHttpResponse response = new DefaultFullHttpResponse(
                HTTP_1_1, currentObj.getDecoderResult().isSuccess()? OK : BAD_REQUEST,
                Unpooled.copiedBuffer(buf.toString(), CharsetUtil.UTF_8));

        response.headers().set(CONTENT_TYPE, "text/plain; charset=UTF-8");

        if (keepAlive) {
            // Add 'Content-Length' header only for a keep-alive connection.
            response.headers().set(CONTENT_LENGTH, response.content().readableBytes());
            // Add keep alive header as per:
            // - http://www.w3.org/Protocols/HTTP/1.1/draft-ietf-http-v11-spec-01.html#Connection
            response.headers().set(CONNECTION, HttpHeaders.Values.KEEP_ALIVE);
        }

        // Write the response.
        ctx.write(response);
        return keepAlive;
    }

    private static void send100Continue(ChannelHandlerContext ctx) {
        FullHttpResponse response = new DefaultFullHttpResponse(HTTP_1_1, CONTINUE);
        ctx.write(response);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        cause.printStackTrace();
        ctx.close();
    }
    
    private boolean checkFunds() {
    	Coin fees = Transaction.REFERENCE_DEFAULT_MIN_TX_FEE.add(joe.appFee());
        Coin balance = kit.wallet().getBalance();	
        System.out.println("Wallet has " + balance.toFriendlyString());
        return balance.isGreaterThan(fees);
    }
    
    private void sendData(ChannelHandlerContext ctx, String data, Address to) {
        sendData(ctx, data.getBytes(joe.charset()), to);
    }
    
    private void sendData(ChannelHandlerContext ctx, byte[] data, Address to) {
    	byte[] prefixBytes = Joe.getDataPrefix();
    	if (prefixBytes.length + data.length > Joe.MAX_DATA_LENGTH)
    		throw new IllegalArgumentException("Data too long by " + (Joe.MAX_DATA_LENGTH - prefixBytes.length - data.length) + " bytes");
    	
    	byte[] appAndData = new byte[Joe.MAX_DATA_LENGTH];
    	System.arraycopy(prefixBytes, 0, appAndData, 0, prefixBytes.length);
    	System.arraycopy(data, 0, appAndData, 0, prefixBytes.length);
    	
    	Wallet.SendRequest sendReq = SendRequest.to(to, appAndData);
    	sendReq.tx.addOutput(joe.appFee(), joe.appAddress());
    	
    	Wallet.SendResult result;
		try {
			result = kit.wallet().sendCoins(kit.peerGroup(), to, Coin.valueOf(20000), appAndData);
//			checkNotNull(result);
			System.out.println("Sent coins to: " + to + ", transaction hash: " + result.tx.toString());
		} catch (InsufficientMoneyException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
