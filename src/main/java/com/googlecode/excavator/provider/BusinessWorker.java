package com.googlecode.excavator.provider;

import org.jboss.netty.channel.Channel;

import com.googlecode.excavator.protocol.RmiRequest;

/**
 * ҵ������
 *
 * @author vlinux
 *
 */
public interface BusinessWorker {

    /**
     * �ɻ�~
     *
     * @param req
     * @param channel
     */
    void work(RmiRequest req, Channel channel);

}
