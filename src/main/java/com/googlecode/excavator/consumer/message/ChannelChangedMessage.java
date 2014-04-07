package com.googlecode.excavator.consumer.message;

import java.net.InetSocketAddress;

import com.googlecode.excavator.consumer.ConsumerService;
import com.googlecode.excavator.message.Message;

/**
 * ���ӱ����Ϣ
 *
 * @author vlinux
 *
 */
public class ChannelChangedMessage extends Message<ConsumerService> {

    /**
     * ��Ϣ����
     *
     * @author vlinux
     *
     */
    public static enum Type {

        /**
         * ����channel
         */
        CREATE,
        /**
         * ɾ��channel
         */
        REMOVE

    }

    private final Type type;					//��Ϣ����
    private final InetSocketAddress address;	//��ַ
    private final String provider;				//�����ṩ����Ӧ���� 

    public ChannelChangedMessage(ConsumerService t, String provider, InetSocketAddress address, Type type) {
        super(t);
        this.provider = provider;
        this.address = address;
        this.type = type;
    }

    public Type getType() {
        return type;
    }

    public InetSocketAddress getAddress() {
        return address;
    }

    public String getProvider() {
        return provider;
    }

}
