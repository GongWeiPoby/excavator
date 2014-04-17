package com.googlecode.excavator.consumer;

import java.util.concurrent.atomic.AtomicInteger;

import org.jboss.netty.channel.Channel;

import com.googlecode.excavator.protocol.Protocol;
import com.googlecode.excavator.protocol.RmiRequest;

/**
 * ���ӻ�
 *
 * @author vlinux
 *
 */
public interface ChannelRing {

    /**
     * ���Ӱ�װ��
     *
     * @author vlinux
     *
     */
    public static final class Wrapper {

        private final Channel channel;			//����
        private final String provider;			//�����ṩ��Ӧ������
        private boolean maybeDown;				//������
        private final AtomicInteger counter;	//���ü���

        public Wrapper(Channel channel, String provider) {
            this.channel = channel;
            this.provider = provider;
            this.counter = new AtomicInteger();
        }

        public boolean isMaybeDown() {
            return maybeDown;
        }

        public void setMaybeDown(boolean maybeDown) {
            this.maybeDown = maybeDown;
        }

        public Channel getChannel() {
            return channel;
        }

        public String getProvider() {
            return provider;
        }

        /**
         * ����++
         */
        public void inc() {
            counter.incrementAndGet();
        }

        /**
         * ����--
         */
        public void dec() {
            if (0 >= counter.decrementAndGet()
                    && null != channel) {
                channel.disconnect();
                channel.close();
            }

        }

    }

    /**
     * ���������ȡ���������
     * @param reqPro
     * @param req
     * @return
     */
    ChannelRing.Wrapper ring(Protocol reqPro, RmiRequest req);

}
