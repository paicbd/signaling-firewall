package com.paic.firewall;

import com.paic.esg.impl.ExtendedSignalingGateway;

public class SignalingFirewall extends ExtendedSignalingGateway {

  public static void main(String[] args) {
    Runtime.getRuntime().addShutdownHook(new Thread(){
      @Override
      public void run(){
        try {
          Thread.sleep(200);
          SignalingFirewall.haltExtendedSignalingGateway();
          Thread.sleep(200);
        } catch (InterruptedException e) {
          Thread.currentThread().interrupt();
        }
      }
    });
    // initialize and start system
    SignalingFirewall.initialize(args).start();
  }

}