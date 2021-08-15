package com.file.uploader;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.http.DefaultFullHttpResponse;
import io.netty.handler.codec.http.FullHttpResponse;
import io.netty.handler.codec.http.HttpContent;
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpObject;
import io.netty.handler.codec.http.HttpRequest;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.multipart.*;
import io.netty.util.CharsetUtil;
import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;


import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;

import java.net.URI;
import java.nio.channels.FileChannel;


import static io.netty.handler.codec.http.HttpMethod.POST;
import static io.netty.handler.codec.http.HttpResponseStatus.*;
import static io.netty.handler.codec.http.HttpVersion.HTTP_1_1;

/**
 * Handles uploading of file and then saves it to a known location.
 */
public class MyHttUploadServerHandler extends SimpleChannelInboundHandler<HttpObject> {

    // Factory that writes to disk
    private static final HttpDataFactory factory = new DefaultHttpDataFactory(true);
    private static final String FILE_UPLOAD_PATH = Config.get("file_upload.location");
    private static final Logger logger = LogManager.getLogger(MyHttUploadServerHandler.class);
    private HttpRequest httpRequest;
    private HttpPostRequestDecoder httpDecoder;
    private final long MAX_SIZE = Integer.parseInt(Config.get("file_upload.max_size"));


    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final HttpObject httpObject)
            throws Exception {
        if (httpObject instanceof HttpRequest) {
            httpRequest = (HttpRequest) httpObject;
            final URI uri = new URI(httpRequest.uri());
            logger.log(Level.INFO, "Got URI " + uri);
            if (httpRequest.method() == POST) {
                httpDecoder = new HttpPostRequestDecoder(factory, httpRequest);
                httpDecoder.setDiscardThreshold(0);
            } else {
                logger.log(Level.WARN, "Got unexpected method " + httpRequest.method());
                sendResponse(ctx, METHOD_NOT_ALLOWED, null);
            }
        }
        if (httpDecoder != null) {
            if (httpObject instanceof final HttpContent chunk) {
                httpDecoder.offer(chunk);
                readChunk(ctx);
            }
        }
    }

    private void readChunk(ChannelHandlerContext ctx) {
        try {
            while (httpDecoder.hasNext()) {
                InterfaceHttpData data = httpDecoder.next();
                if (data != null) {
                    try {
                        switch (data.getHttpDataType()) {
                            case Attribute:
                                break;
                            case FileUpload:
                                final DiskFileUpload fileUpload = (DiskFileUpload) data;

                                try {
                                    if (fileUpload.length() > MAX_SIZE) {
                                        throw new RuntimeException("Max limit reached.");
                                    }
                                }
                                catch (Exception e) {
                                    logger.log(Level.ERROR, e.getMessage());
                                    sendResponse(ctx, REQUEST_ENTITY_TOO_LARGE, "Max limit reached.");
                                    break;
                                }
                                final File targetDir = new File(FILE_UPLOAD_PATH );
                                File file = File.createTempFile("uploaded-", null, targetDir);
                                if (!file.exists()) {
                                    file.createNewFile();
                                }
                                logger.log(Level.INFO, "Created file " + file);
                                try (FileChannel inputChannel = new FileInputStream(fileUpload.getFile()).getChannel();
                                     FileChannel outputChannel = new FileOutputStream(file).getChannel()) {
                                    outputChannel.transferFrom(inputChannel, 0, inputChannel.size());
                                    sendResponse(ctx, CREATED, "file name: " + file.getAbsolutePath());
                                }
                                break;
                        }
                    } catch (Exception e) {
                        logger.log(Level.ERROR, e.getMessage());
                        sendResponse(ctx, INTERNAL_SERVER_ERROR, "Server Error.");
                    } finally {
                        data.release();
                    }
                }
            }
        } catch (HttpPostRequestDecoder.EndOfDataDecoderException e) {
            logger.log(Level.DEBUG, "Decoder is closed.");
        }
    }

    private static void sendResponse(ChannelHandlerContext ctx, HttpResponseStatus status, String message) {
        final FullHttpResponse response;
        String msgDesc = message;
        if (message == null) {
            msgDesc = "Failure: " + status;
        }
        msgDesc += " \r\n";

        final ByteBuf buffer = Unpooled.copiedBuffer(msgDesc, CharsetUtil.UTF_8);
        if (status.code() >= HttpResponseStatus.BAD_REQUEST.code()) {
            response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer);
        } else {
            response = new DefaultFullHttpResponse(HTTP_1_1, status, buffer);
        }
        response.headers().set(HttpHeaderNames.CONTENT_TYPE, "text/plain; charset=UTF-8");

        // Close the connection as soon as the response is sent.
        ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.log(Level.ERROR, "Something goes wrong.");
        ctx.channel().close();
    }

}