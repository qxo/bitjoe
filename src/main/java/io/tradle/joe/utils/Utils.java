package io.tradle.joe.utils;

import io.netty.channel.ChannelHandlerContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.InetSocketAddress;
import java.net.URI;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.HttpClientBuilder;


//import io.netty.handler.codec.http.HttpHeaderUtil;

public class Utils {

	public static String getRemoteIPAddress(ChannelHandlerContext ctx) {
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
	
	public static HttpResponseData get(URI uri) {
		HttpClient client = HttpClientBuilder.create().build();
		HttpGet request = new HttpGet(uri);
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
}
