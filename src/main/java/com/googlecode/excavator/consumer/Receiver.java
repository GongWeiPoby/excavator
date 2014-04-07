package com.googlecode.excavator.consumer;

import java.util.List;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

import org.jboss.netty.channel.Channel;

import com.googlecode.excavator.protocol.RmiRequest;
import com.googlecode.excavator.protocol.RmiResponse;

/**
 * ������
 *
 * @author vlinux
 *
 */
public interface Receiver {

    /**
     * ��װ�࣬�ڴ˰�װ�з�װ��rmi�����Ӧ��
     *
     * @author vlinux
     *
     */
    public final static class Wrapper {

        private final RmiRequest request;
        private RmiResponse response;
        private final ReentrantLock lock;
        private final Condition waitResp;
        private Channel channel;

        public Wrapper(RmiRequest request) {
            this.request = request;
            this.lock = new ReentrantLock(false);
            this.waitResp = lock.newCondition();
        }

        /**
         * ������Ӧ�ȴ�
         */
        public void signalWaitResp() {
            lock.lock();
            try {
                waitResp.signal();
            } finally {
                lock.unlock();
            }
        }

        public RmiRequest getRequest() {
            return request;
        }

        public RmiResponse getResponse() {
            return response;
        }

        public void setResponse(RmiResponse response) {
            this.response = response;
        }

        public ReentrantLock getLock() {
            return lock;
        }

        public Condition getWaitResp() {
            return waitResp;
        }

        public Channel getChannel() {
            return channel;
        }

        public void setChannel(Channel channel) {
            this.channel = channel;
        }

    }

    /**
     * �Һ�һ������
     *
     * @param req
     * @return
     */
    Receiver.Wrapper register(RmiRequest req);

    /**
     * ɾ��֮ǰ�Һŵ�����
     *
     * @param id
     * @return
     */
    Receiver.Wrapper unRegister(long id);

    /**
     * ɾ��channel�����еĹҺ�
     *
     * @param channel
     * @return
     */
    List<Receiver.Wrapper> unRegister(Channel channel);

    /**
     * ���շ��ص�ѶϢ<br/>
     * ѶϢһ�����գ�����ӽ��ճ���ȥ��
     *
     * @param id
     * @return ���ض�Ӧ�İ�װ�� ���reqEvtId�������򷵻�null<br/>
     */
    Receiver.Wrapper receive(long id);

}
