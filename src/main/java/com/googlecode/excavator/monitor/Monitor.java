package com.googlecode.excavator.monitor;

/**
 * ��־��ʽ
 *
 * @author vlinux
 *
 */
public final class Monitor {

    /**
     * ��־����
     *
     * @author vlinux
     *
     */
    public static enum Type {

        /**
         * �����
         */
        PROVIDER,
        /**
         * ���Ѷ�
         */
        CONSUMER
    }

    private final Type type; 		// ����
    private final String group; 	// ���÷���
    private final String version; 	// ���ð汾
    private final String sign; 		// ����ǩ��
    private final String from; 		// ��Դ
    private final String to; 		// Ŀ��
    private long times; 			// �����ڵ��ô���
    private long cost; 				// ����������ʱ��ms

    /**
     * ���캯�������ڵ�һ�δ���monitor��Ϣ
     *
     * @param type
     * @param group
     * @param version
     * @param sign
     * @param from
     * @param to
     */
    public Monitor(Type type, String group, String version, String sign,
            String from, String to) {
        this.type = type;
        this.group = group;
        this.version = version;
        this.sign = sign;
        this.from = from;
        this.to = to;
    }

    /**
     * ���캯��������ÿ��monitor��Ϣ���ۼ�
     *
     * @param m
     */
    public Monitor(Monitor m) {
        this.type = m.type;
        this.group = m.group;
        this.version = m.version;
        this.sign = m.sign;
        this.from = m.from;
        this.to = m.to;
    }

    public Type getType() {
        return type;
    }

    public String getFrom() {
        return from;
    }

    public String getTo() {
        return to;
    }

    public long getTimes() {
        return times;
    }

    public void setTimes(long times) {
        this.times = times;
    }

    public long getCost() {
        return cost;
    }

    public void setCost(long cost) {
        this.cost = cost;
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

    @Override
    protected Monitor clone() {
        final Monitor m = new Monitor(this);
        m.cost = cost;
        m.times = times;
        return m;
    }

}
