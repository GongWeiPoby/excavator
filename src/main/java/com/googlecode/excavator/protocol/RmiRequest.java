package com.googlecode.excavator.protocol;

import java.io.Serializable;

/**
 * rmi����Э��:����
 *
 * @author vlinux
 *
 */
public final class RmiRequest extends RmiTracer implements Serializable {

    private static final long serialVersionUID = 823825583753849024L;

    private final String key;			//group+version+sign
    private final String group;			//�������
    private final String version;		//����汾
    private final String sign;			//����ǩ��
    private final Serializable[] args;	//���ݲ���
    private final String appName;		//���Ѷ�Ӧ����
    private final long timeout;			//����ʱʱ��

    /**
     * ����rmi����
     *
     * @param group
     * @param version
     * @param sign
     * @param args
     * @param appName
     * @param timeout
     */
    public RmiRequest(
            String group, String version, String sign,
            Serializable[] args, String appName, long timeout) {
        this.group = group;
        this.version = version;
        this.sign = sign;
        this.args = args;
        this.appName = appName;
        this.timeout = timeout;
        this.key = group + version + sign;
    }

    /**
     * rmi����:ת����<br/>
     * ͨ���˹��캯��������������󽫻�Я��ָ����token
     *
     * @param token
     * @param group
     * @param version
     * @param sign
     * @param args
     * @param appName
     * @param timeout
     */
    public RmiRequest(String token, String group, String version, String sign,
            Serializable[] args, String appName, long timeout) {
        super(token);
        this.group = group;
        this.version = version;
        this.sign = sign;
        this.args = args;
        this.appName = appName;
        this.timeout = timeout;
        this.key = group + version + sign;
    }

    public String getKey() {
        return key;
    }

    public String getAppName() {
        return appName;
    }

    public String getGroup() {
        return group;
    }

    public String getVersion() {
        return version;
    }

    public String getSign() {
        return sign;
    }

    public Serializable[] getArgs() {
        return args;
    }

    public long getTimeout() {
        return timeout;
    }

    @Override
    public String toString() {
        return String.format("REQ[token=%s;group=%s;version=%s;sign=%s;consumer=%s;timeout=%s;]",
                getToken(), group, version, sign, appName, timeout);
    }

}
