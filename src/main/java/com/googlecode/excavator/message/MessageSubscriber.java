package com.googlecode.excavator.message;

/**
 * ��Ϣ������
 * @author vlinux
 *
 */
public interface MessageSubscriber {

	/**
	 * ������Ϣ
	 * @param msg
	 * @throws Exception
	 */
	void receive(Message<?> msg) throws Exception;
	
}
