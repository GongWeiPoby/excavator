package com.googlecode.excavator.message;


/**
 * ��ϢͶ��Ա<br/>
 * ���ڳ����ڲ����support֮����໥ͨѶ
 *
 * @author vlinux
 *
 */
public interface Messager {

    /**
     * ע����Ϣ������
     * @param subscriber
     * @param msgTypes
     */
    void register(MessageSubscriber subscriber, Class<?>... msgTypes);
    
    /**
     * Ͷ����Ϣ
     * @param msg
     */
    void post(Message<?> msg);
    
}
