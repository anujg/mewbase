package com.tesco.mewbase.server.impl;

import com.tesco.mewbase.bson.BsonObject;
import com.tesco.mewbase.common.FrameHandler;
import io.vertx.core.Handler;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.net.NetSocket;
import io.vertx.core.parsetools.RecordParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * TODO we should write our own BSON parser and only decode fields when needed.
 * BSON is ordered and `type` should be the first field - we only need to decode that in order to know what to do
 * with the frame - in the case of an EMIT we should be able to pass the buffer direct to subscribers without
 * further decoding of the event, also to storage. I.e. we want to avoid decoding the entire frame in all cases
 *
 * Created by tim on 23/09/16.
 */
public class Codec {

    // Frame types

    public static final String RESPONSE_FRAME = "RESPONSE";
    public static final String CONNECT_FRAME = "CONNECT";
    public static final String EMIT_FRAME = "EMIT";
    public static final String STARTTX_FRAME = "STARTTX";
    public static final String COMMITTX_FRAME = "COMMITTX";
    public static final String ABORTTX_FRAME = "ABORTTX";
    public static final String SUBSCRIBE_FRAME = "SUBSCRIBE";
    public static final String UNSUBSCRIBE_FRAME = "UNSUBSCRIBE";
    public static final String SUBRESPONSE_FRAME = "SUBRESPONSE";
    public static final String RECEV_FRAME = "RECEV";
    public static final String ACKEV_FRAME = "ACKEV";
    public static final String QUERY_FRAME = "QUERY";
    public static final String QUERYRESPONSE_FRAME = "QUERYRESPONSE";
    public static final String QUERYRESULT_FRAME = "QUERYRESULT";
    public static final String QUERYACK_FRAME = "QUERYACK";
    public static final String PING_FRAME = "PING";

    // Frame fields

    public static final String RESPONSE_OK = "ok";
    public static final String RESPONSE_ERRMSG = "errMsg";
    public static final String RESPONSE_ERRCODE = "errCode";

    public static final String SUBRESPONSE_SUBID = "subID";

    public static final String CONNECT_USERNAME = "username";
    public static final String CONNECT_PASSWORD = "password";
    public static final String CONNECT_VERSION = "version";

    public static final String EMIT_STREAMNAME = "streamName";
    public static final String EMIT_EVENTTYPE = "eventType";
    public static final String EMIT_EVENT = "event";
    public static final String EMIT_SESSID = "sessID";

    public static final String STARTTX_SESSID = "sessID";

    public static final String COMMITTX_SESSID = "sessID";

    public static final String ABORTTX_SESSID = "sessID";

    public static final String SUBSCRIBE_STREAMNAME = "streamName";
    public static final String SUBSCRIBE_EVENTTYPE = "eventType";
    public static final String SUBSCRIBE_STARTSEQ = "startSeq";
    public static final String SUBSCRIBE_STARTTIMESTAMP = "startTimestamp";
    public static final String SUBSCRIBE_DURABLEID = "durableID";
    public static final String SUBSCRIBE_MATCHER = "matcher";

    public static final String UNSUBSCRIBE_SUBID = "subID";

    public static final String RECEV_SUBID = "subID";
    public static final String RECEV_EVENTTYPE = "eventType";
    public static final String RECEV_TIMESTAMP = "timestamp";
    public static final String RECEV_SEQNO = "seqNo";
    public static final String RECEV_EVENT = "event";

    public static final String ACKEV_SUBID = "subID";
    public static final String ACKEV_BYTES = "bytes";

    // Query stuff TODO


    private final static Logger log = LoggerFactory.getLogger(Codec.class);

    private final FrameHandler frameHandler;

    public Codec(NetSocket socket, FrameHandler frameHandler) {
        this.frameHandler = frameHandler;
        RecordParser parser = RecordParser.newFixed(4, null);
        Handler<Buffer> handler = new Handler<Buffer>() {
            int size = -1;
            public void handle(Buffer buff) {
                if (size == -1) {
                    size = buff.getIntLE(0) - 4;
                    parser.fixedSizeMode(size);
                } else {
                    handleFrame(size, buff);
                    parser.fixedSizeMode(4);
                    size = -1;
                }
            }
        };
        parser.setOutput(handler);
        socket.handler(parser);
    }

    private void handleFrame(int size, Buffer buffer) {
        // TODO bit clunky - need to add size back in so it can be decoded, improve this!
        Buffer buff2 = Buffer.buffer(buffer.length() + 4);
        buff2.appendIntLE(size  + 4).appendBuffer(buffer);
        BsonObject bson = new BsonObject(buff2);
        handleBson(bson);
    }

    private void handleBson(BsonObject bson) {
        String type = bson.getString("type");
        BsonObject frame = bson.getBsonObject("frame");
        switch (type) {
            case RESPONSE_FRAME:
                frameHandler.handleResponse(frame);
                break;
            case CONNECT_FRAME:
                frameHandler.handleConnect(frame);
                break;
            case EMIT_FRAME:
                frameHandler.handleEmit(frame);
                break;
            case STARTTX_FRAME:
                frameHandler.handleStartTx(frame);
                break;
            case COMMITTX_FRAME:
                frameHandler.handleCommitTx(frame);
                break;
            case ABORTTX_FRAME:
                frameHandler.handleAbortTx(frame);
                break;
            case SUBSCRIBE_FRAME:
                frameHandler.handleSubscribe(frame);
                break;
            case UNSUBSCRIBE_FRAME:
                frameHandler.handleUnsubscribe(frame);
                break;
            case SUBRESPONSE_FRAME:
                frameHandler.handleSubResponse(frame);
                break;
            case RECEV_FRAME:
                frameHandler.handleRecev(frame);
                break;
            case ACKEV_FRAME:
                frameHandler.handleAckEv(frame);
                break;
            case QUERY_FRAME:
                frameHandler.handleQuery(frame);
                break;
            case QUERYRESPONSE_FRAME:
                frameHandler.handleQueryResponse(frame);
                break;
            case QUERYRESULT_FRAME:
                frameHandler.handleQueryResult(frame);
                break;
            case QUERYACK_FRAME:
                frameHandler.handleQueryAck(frame);
                break;
            case PING_FRAME:
                frameHandler.handlePing(frame);
                break;
            default:
                log.error("Invalid frame type: " + type);
        }
    }

    public static Buffer encodeFrame(String frameType, BsonObject frame) {
        BsonObject env = new BsonObject();
        env.put("type", frameType).put("frame", frame);
        return env.encode();
    }
}

