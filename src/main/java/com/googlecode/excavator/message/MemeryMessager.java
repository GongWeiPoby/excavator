package com.googlecode.excavator.message;

import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.excavator.Ring;
import com.googlecode.excavator.constant.LogConstant;

/**
 * �ڴ���ϢͶ��ʵ��
 * @author vlinux
 *
 */
public class MemeryMessager implements Messager {

    private final Logger logger = LoggerFactory.getLogger(LogConstant.MESSAGES);
    
    /*
     * �������һ����Ϣ�ظ�Ͷ��5��
     */
    private final int MAX_RETRY = 5;

    /*
     * ÿ���ظ�Ͷ�ݵĳͷ�ʱ�䲽��
     * ���յĳͷ�ʱ��Ϊ RETRY * PUNISH_TIME_STEP
     */
    private final long PUNISH_TIME_STEP = 500;

    /*
     * ���Ĺ�ϵ��
     */
    private Map<Class<?>, Set<MessageSubscriber>> subscriptionRelationships = Maps.newHashMap();

    /*
     * �ͷ�Ͷ�ݻ�
     */
    private Ring<Wrapper> punishPostRing = new Ring<Wrapper>();

    /**
     * �ͷ�Ͷ�ݷ�װ
     *
     * @author vlinux
     *
     */
    private class Wrapper {

        private final Message<?> message;   //��Ϣ
        private final long expirt;          //����ʱ��

        public Wrapper(Message<?> message, long expirt) {
            this.message = message;
            this.expirt = expirt;
        }

    }

    /**
     * �ͷ���ϢͶ��Ա
     */
    private Thread deamon = new Thread("message-punish-deamon") {

        @Override
        public void run() {
            while (true) {
                try {
                    Thread.sleep(500);
                    if (punishPostRing.isEmpty()) {
                        continue;
                    }
                    final long now = System.currentTimeMillis();
                    Iterator<Wrapper> it = punishPostRing.iterator();
                    while (it.hasNext()) {
                        Wrapper wrapper = it.next();
                        if (now < wrapper.expirt) {
                            // ʱ��û��������
                            continue;
                        } else {
                            // ʱ�䵽�ˣ������ٴ�Ͷ����
                            it.remove();
                            normalPost(wrapper.message);
                        }
                    }
                } catch (Throwable t) {
                    logger.warn("punish post message failed!", t);
                }

            }
        }

    };

    /**
     * ���캯��
     */
    public MemeryMessager() {
        deamon.setDaemon(true);
        deamon.start();
    }
    
    /**
     * Ͷ����Ϣ
     *
     * @param msg
     */
    public void post(Message<?> msg) {
        if (null == msg) {
            return;
        }

        int reTry = msg.getReTry();
        // ����Ͷ�����Դ�������������
        if (MAX_RETRY <= reTry) {
            return;
        }

        msg.inc();

        // ��һ������ͬѧ������ͨͶ��
        if (reTry == 0) {
            normalPost(msg);
        } // ����������ʵʵ����ʱ�ͷ�Ͷ��
        else {
            punishPost(msg);
        }

    }

    /**
     * ��ͨͶ��
     *
     * @param msg
     */
    private void normalPost(Message<?> msg) {

        final Class<?> clazz = msg.getClass();
        final Set<MessageSubscriber> subscribers = subscriptionRelationships.get(clazz);
        if (CollectionUtils.isEmpty(subscribers)) {
            return;
        }
        final Iterator<MessageSubscriber> subIt = subscribers.iterator();
        while (subIt.hasNext()) {
            MessageSubscriber subscriber = subIt.next();
            try {
                subscriber.receive(msg);
            } catch (Throwable t) {
              logger.warn("post msg:{} to subscriber:{} failed.", new Object[]{msg, subscriber.getClass().getSimpleName(), t});
                // Ͷ��ʧ�ܣ������ٴ�Ͷ�ݣ��Գͷ�
                post(msg);
            }
        }

    }

    /**
     * �ͷ�Ͷ��
     *
     * @param msg
     */
    private void punishPost(Message<?> msg) {

        long now = System.currentTimeMillis();
        long punishTime = msg.getReTry() * PUNISH_TIME_STEP;

        punishPostRing.insert(new Wrapper(msg, now + punishTime));

    }

    /**
     * ע�ᶩ�Ĺ�ϵ
     *
     * @param subscriber
     * @param msgTypes
     */
    public synchronized void register(MessageSubscriber subscriber, Class<?>... msgTypes) {
        if (ArrayUtils.isEmpty(msgTypes)
                || null == subscriber) {
            return;
        }
        for (Class<?> clazz : msgTypes) {
            final Set<MessageSubscriber> subscribers;
            if (!subscriptionRelationships.containsKey(clazz)) {
                subscribers = Sets.newHashSet();
                subscriptionRelationships.put(clazz, subscribers);
            } else {
                subscribers = subscriptionRelationships.get(clazz);
            }//if
            subscribers.add(subscriber);
        }
    }
    
}
