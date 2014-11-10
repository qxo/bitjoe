package io.tradle.joe.handlers;

import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.QueryStringDecoder;
import io.tradle.joe.exceptions.BadRequestException;
import io.tradle.joe.exceptions.NoSuchPathException;
import io.tradle.joe.requests.AbstractRequest;
import io.tradle.joe.requests.BootstrapRequest;
import io.tradle.joe.requests.TransactionRequest;
import io.tradle.joe.requests.WebHookRequest;
import io.tradle.joe.utils.Utils;

import java.lang.reflect.Constructor;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Sharable
public class PathFinder extends SimpleChannelInboundHandler<HttpRequest> {

	private final Logger logger = LoggerFactory.getLogger(PathFinder.class);
	
	private static enum Path {
		hooks (WebHookRequest.class),
		transaction (TransactionRequest.class),
		bootstrap (BootstrapRequest.class);
		
		private final Class<? extends AbstractRequest> clazz;
		private final Constructor<? extends AbstractRequest> constructor;

		Path (Class<? extends AbstractRequest> clazz) {
			this.clazz = clazz;
			try {
				this.constructor = clazz.getDeclaredConstructor(HttpRequest.class);
			} catch (Exception e) {
				throw new IllegalArgumentException("Matching constructor not found for request class", e);
			}
		}
		
		public AbstractRequest getRequest(HttpRequest req) throws BadRequestException {
			try {
				return (AbstractRequest) constructor.newInstance(req);
			} catch (ReflectiveOperationException e) {
				if (e.getCause() instanceof BadRequestException)
					throw (BadRequestException) e.getCause();
				else
					throw new IllegalArgumentException("Failed to create instance of " + clazz.getCanonicalName(), e);
			}
		}
	}
	
	public PathFinder() {
		super(false); // do not release msg object since it's passed through
	}
	
	@Override
	protected void channelRead0(ChannelHandlerContext ctx, HttpRequest req) throws Exception {
		if (!"127.0.0.1".equals(Utils.getRemoteIPAddress(ctx))) {
			// ignore all network requests from the great outdoors
			ctx.close();
			return;
		}
		
		QueryStringDecoder qsd = new QueryStringDecoder(req.getUri());
		String pathStr = qsd.path();
		Path path = null;
		if (pathStr.startsWith("/")) {
			int nextSlashIdx = pathStr.indexOf("/", 1);
			pathStr = nextSlashIdx == -1 ? pathStr.substring(1) : pathStr.substring(1, nextSlashIdx);
			try {
				path = Path.valueOf(pathStr.toLowerCase());
			} catch (Exception e) {
				// handled below with NoSuchPathException
			}
		}
		
		if (path == null)
			throw new NoSuchPathException();

		ctx.fireChannelRead(path.getRequest(req));
	}
}
