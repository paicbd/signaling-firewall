package com.paic.esg.api.app;

import com.paic.esg.api.chn.ChannelMessage;

public interface QueueInterface {
    void send(ChannelMessage element);
    ChannelMessage receive();
}
