package com.googlecode.excavator.provider;

import org.jboss.netty.channel.Channel;

import com.googlecode.excavator.protocol.Protocol;

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
     * @param proto
     * @param channel
     */
    void work(Protocol proto, Channel channel);

}
