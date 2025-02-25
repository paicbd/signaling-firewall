package com.paic.esg.impl.chn;

import com.paic.esg.impl.settings.ChannelSettings;

import com.paic.esg.api.chn.IChannelHandler;
import com.paic.esg.api.chn.ChannelMessage;

import com.paic.esg.impl.queue.QueueInstancePool;
import com.paic.esg.impl.queue.BlockingQueue;

import org.apache.log4j.Logger;

public abstract class ChannelHandler implements IChannelHandler {

    private static final Logger logger = Logger.getLogger(ChannelHandler.class);

    protected BlockingQueue queue;

    public ChannelHandler(ChannelSettings channelSettings) {
        queue = QueueInstancePool.getQueueInstance(channelSettings.getName());
    }

    public int sendMessageRequest(ChannelMessage channelMessage) {
        // send message to application throughout queue
        logger.debug(channelMessage.toString());
        queue.send(channelMessage);
        return 0;
    }

}