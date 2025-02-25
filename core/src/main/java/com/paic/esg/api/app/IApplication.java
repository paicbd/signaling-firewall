package com.paic.esg.api.app;

import com.paic.esg.api.chn.ChannelMessage;

public interface IApplication {
    void processMessage(ChannelMessage channelMessage);
}
