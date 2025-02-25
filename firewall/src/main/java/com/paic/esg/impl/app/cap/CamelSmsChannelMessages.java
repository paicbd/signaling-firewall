package com.paic.esg.impl.app.cap;

import com.paic.esg.network.layers.CapLayer;
import org.apache.log4j.Logger;
import org.restcomm.protocols.ss7.cap.api.CAPStack;

public class CamelSmsChannelMessages {

    private static final Logger logger = Logger.getLogger(CamelSmsChannelMessages.class);
    private Object capOperationObject;
    private CAPStack capStackIn;
    private CAPStack capStackOut;
    private String channelTransId;

    public CamelSmsChannelMessages(CapLayer capLayerIn, CapLayer capLayerOut,
            Object capOperationObject, String transactionid) {
        this.capOperationObject = capOperationObject;
        this.capStackIn = capLayerIn.getCapStack();
        this.capStackOut = capLayerOut.getCapStack();
        this.channelTransId = transactionid;
        logger.trace(this.capOperationObject + " | " + this.capStackIn + " | " + this.capStackOut
                + " | " + this.channelTransId);
    }
}
