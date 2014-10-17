package io.tradle.joe;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;

import java.net.URI;

public class Utils {

	public static void request(HttpMethod method, String url) throws Exception {
		request(method, new URI(url));
	}
	
	public static void request(HttpMethod method, URI uri) throws Exception {
        String scheme = uri.getScheme() == null? "http" : uri.getScheme();
        String host = uri.getHost() == null? "127.0.0.1" : uri.getHost();
        int port = uri.getPort();
        if (port == -1) {
            if ("http".equalsIgnoreCase(scheme)) {
                port = 80;
            } else if ("https".equalsIgnoreCase(scheme)) {
                port = 443;
            }
        }

        if (!"http".equalsIgnoreCase(scheme) && !"https".equalsIgnoreCase(scheme))
            throw new IllegalArgumentException("Only HTTP(S) is supported.");

        // Configure SSL context if necessary.
        final boolean ssl = "https".equalsIgnoreCase(scheme);
        final SslContext sslCtx;
        if (ssl) {
            sslCtx = SslContext.newClientContext(InsecureTrustManagerFactory.INSTANCE);
        } else {
            sslCtx = null;
        }

        // Configure the client.
        EventLoopGroup group = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(group)
             .channel(NioSocketChannel.class)
             .handler(new HttpSnoopClientInitializer(sslCtx));

            // Make the connection attempt.
            Channel ch = b.connect(host, port).sync().channel();

            // Prepare the HTTP request.
            HttpRequest request = new DefaultFullHttpRequest(HttpVersion.HTTP_1_1, method, uri.getRawPath());
            request.headers().set(HttpHeaders.Names.HOST, host);
            request.headers().set(HttpHeaders.Names.CONNECTION, HttpHeaders.Values.CLOSE);
            request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING, HttpHeaders.Values.GZIP);

            // Send the HTTP request.
            ch.writeAndFlush(request);

            // Wait for the server to close the connection.
            ch.closeFuture().sync();
        } finally {
            // Shut down executor threads to exit.
            group.shutdownGracefully();
        }
	}
	
//	public static HttpResponse request1(HttpMethod method, Map<String, String> params) {
//		HttpRequest httpReq=new DefaultHttpRequest(HttpVersion.HTTP_1_1,HttpMethod.POST,uri);
//		httpReq.setHeader(HttpHeaders.Names.HOST,host);
//		httpReq.setHeader(HttpHeaders.Names.CONNECTION,HttpHeaders.Values.KEEP_ALIVE);
//		httpReq.setHeader(HttpHeaders.Names.ACCEPT_ENCODING,HttpHeaders.Values.GZIP);
//		String params="a=b&c=d";
//		ChannelBuffer cb=ChannelBuffers.copiedBuffer(params,Charset.defaultCharset());
//		httpReq.setHeader(HttpHeaders.Names.CONTENT_LENGTH,cb.readableBytes());
//		httpReq.setContent(cb);
//	}
}
