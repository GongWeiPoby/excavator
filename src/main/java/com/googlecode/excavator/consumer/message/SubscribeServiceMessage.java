package com.googlecode.excavator.consumer.message;

import com.googlecode.excavator.consumer.ConsumerService;
import com.googlecode.excavator.message.Message;

/**
 * ���ķ�����Ϣ
 *
 * @author vlinux
 *
 */
public class SubscribeServiceMessage extends Message<ConsumerService> {

    public SubscribeServiceMessage(ConsumerService t) {
        super(t);
    }

}
