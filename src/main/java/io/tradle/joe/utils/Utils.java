package io.tradle.joe.utils;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.http.DefaultFullHttpRequest;
import io.netty.handler.codec.http.HttpClientCodec;
import io.netty.handler.codec.http.HttpContentDecompressor;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpVersion;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.util.InsecureTrustManagerFactory;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpContent;
//import io.netty.handler.codec.http.HttpHeaderUtil;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.LastHttpContent;
import io.netty.util.CharsetUtil;

import java.net.URI;

public class Utils {

	public static void request(HttpMethod method, String url) throws Exception {
		request(method, new URI(url));
	}

	public static void request(HttpMethod method, URI uri) throws Exception {
		String scheme = uri.getScheme() == null ? "http" : uri.getScheme();
		String host = uri.getHost() == null ? "127.0.0.1" : uri.getHost();
		int port = uri.getPort();
		if (port == -1) {
			if ("http".equalsIgnoreCase(scheme)) {
				port = 80;
			} else if ("https".equalsIgnoreCase(scheme)) {
				port = 443;
			}
		}

		if (!"http".equalsIgnoreCase(scheme)
				&& !"https".equalsIgnoreCase(scheme))
			throw new IllegalArgumentException("Only HTTP(S) is supported.");

		// Configure SSL context if necessary.
		final boolean ssl = "https".equalsIgnoreCase(scheme);
		final SslContext sslCtx;
		if (ssl) {
			sslCtx = SslContext
					.newClientContext(InsecureTrustManagerFactory.INSTANCE);
		} else {
			sslCtx = null;
		}

		// Configure the client.
		EventLoopGroup group = new NioEventLoopGroup();
		try {
			Bootstrap b = new Bootstrap();
			b.group(group).channel(NioSocketChannel.class)
					.handler(new SimpleClientInitializer(sslCtx));

			// Make the connection attempt.
			Channel ch = b.connect(host, port).sync().channel();

			// Prepare the HTTP request.
			HttpRequest request = new DefaultFullHttpRequest(
					HttpVersion.HTTP_1_1, method, uri.getRawPath());
			request.headers().set(HttpHeaders.Names.HOST, host);
			request.headers().set(HttpHeaders.Names.CONNECTION,
					HttpHeaders.Values.CLOSE);
			request.headers().set(HttpHeaders.Names.ACCEPT_ENCODING,
					HttpHeaders.Values.GZIP);

			// Send the HTTP request.
			ch.writeAndFlush(request);

			// Wait for the server to close the connection.
			ch.closeFuture().sync();
		} finally {
			// Shut down executor threads to exit.
			group.shutdownGracefully();
		}
	}

	public static class SimpleClientInitializer extends
			ChannelInitializer<SocketChannel> {

		private final SslContext sslCtx;

		public SimpleClientInitializer(SslContext sslCtx) {
			this.sslCtx = sslCtx;
		}

		@Override
		public void initChannel(SocketChannel ch) {
			ChannelPipeline p = ch.pipeline();
			if (sslCtx != null)
				p.addLast(sslCtx.newHandler(ch.alloc()));

			p.addLast(new HttpClientCodec());
			p.addLast(new HttpContentDecompressor());
			p.addLast(new SimpleClientHandler());
		}
	}

	public static class SimpleClientHandler extends SimpleChannelInboundHandler<HttpObject> {
		@Override
		public void channelRead0(ChannelHandlerContext ctx, HttpObject msg) {
			if (msg instanceof HttpResponse) {
				HttpResponse response = (HttpResponse) msg;

				System.err.println("STATUS: " + response.getStatus());
				System.err.println("VERSION: " + response.getProtocolVersion());
				System.err.println();

				if (!response.headers().isEmpty()) {
					for (String name : response.headers().names()) {
						for (String value : response.headers().getAll(name)) {
							System.err.println("HEADER: " + name + " = " + value);
						}
					}
					System.err.println();
				}

				if (HttpHeaders.isTransferEncodingChunked(response)) {
					System.err.println("CHUNKED CONTENT {");
				} else {
					System.err.println("CONTENT {");
				}
			}
			
			if (msg instanceof HttpContent) {
				HttpContent content = (HttpContent) msg;

				System.err.print(content.content().toString(CharsetUtil.UTF_8));
				System.err.flush();

				if (content instanceof LastHttpContent) {
					System.err.println("} END OF CONTENT");
					ctx.close();
				}
			}
		}

		@Override
		public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
			cause.printStackTrace();
			ctx.close();
		}
	}
}
