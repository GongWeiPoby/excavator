package com.googlecode.excavator.consumer;

import java.lang.reflect.Method;

/**
 * consumer��service
 *
 * @author vlinux
 *
 */
public class ConsumerService {

    private final String key;			//group+version+sign
    private final String group;			//�������
    private final String version;		//����汾
    private final String sign;			//����ǩ��
    private final long timeout;			//��ʱʱ��
    
    private final Class<?> targetInterface; //Ŀ��ӿ�
    private final Method targetMethod;      //Ŀ�귽��

    public ConsumerService(String group, String version, String sign, long timeout, Class<?> targetInterface, Method targetMethod) {
        this.group = group;
        this.version = version;
        this.sign = sign;
        this.timeout = timeout;
        this.key = group + version + sign;
        this.targetInterface = targetInterface;
        this.targetMethod = targetMethod;
    }

    public String getSign() {
        return sign;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public long getTimeout() {
        return timeout;
    }

    public String getKey() {
        return key;
    }

    public Class<?> getTargetInterface() {
        return targetInterface;
    }

    public Method getTargetMethod() {
        return targetMethod;
    }

    public String toString() {
        return String.format("sign=%s;group=%s;version=%s;timeout=%s",
                sign, group, version, timeout);
    }

}
