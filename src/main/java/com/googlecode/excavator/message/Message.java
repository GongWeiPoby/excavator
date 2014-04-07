package com.googlecode.excavator.message;

/**
 * ��Ϣ����
 *
 * @author vlinux
 *
 */
public class Message<T> {

    private final T content;	//��Ϣ����
    private int reTry;			//����Ϣ����Ͷ�ݴ���

    /**
     * ������Ϣ����<br/>
     * һ����Ϣ����Ҫ������
     *
     * @param t
     */
    public Message(T t) {
        this.content = t;
        this.reTry = 0;
    }

    public T getContent() {
        return content;
    }

    public int getReTry() {
        return reTry;
    }

    /**
     * ÿ��Ͷ��++
     *
     * @return
     */
    public Message<T> inc() {
        reTry++;
        return this;
    }

}
