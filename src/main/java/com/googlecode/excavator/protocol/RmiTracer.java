package com.googlecode.excavator.protocol;

import static com.googlecode.excavator.PropertyConfiger.isEnableToken;
import static java.lang.System.currentTimeMillis;

import java.io.Serializable;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

import org.apache.commons.lang.StringUtils;

/**
 * rmi���ø�����<br/>
 * ������rmi request/response �Ļ��࣬��Ҫ���ڸ������id��token���Ա����
 * @author vlinux
 *
 */
public class RmiTracer implements Serializable {

	private static final long serialVersionUID = -940954284480781971L;

	/**
	 * rmi�ĵ�������
	 */
	private static transient final AtomicLong seq = new AtomicLong();
	
	private final long id;
	private final String token;
	private long timestamp;
	
	/**
	 * Я��id��token�Ĺ��캯��<br/>
	 * ����rmiӦ���๹�캯���ĳ���
	 * @param id
	 * @param token
	 */
	public RmiTracer(long id, String token) {
		this.id = id;
		this.token = token;
		this.timestamp = currentTimeMillis();
	}
	
	/**
	 * Я��token�Ĺ��캯��<br/>
	 * ����rmi����ת���ĳ���
	 * @param token
	 */
	public RmiTracer(String token) {
		this.id = seq.incrementAndGet();
		this.token = token;
		this.timestamp = currentTimeMillis();
	}
	
	/**
	 * ��Я��id��token�Ĺ��캯��<br/>
	 * id��������,token�����������<br/>
	 * �����������rmi����ĳ���
	 */
	public RmiTracer() {
		this.id = seq.incrementAndGet();
		this.token = isEnableToken()
				? StringUtils.EMPTY
				: UUID.randomUUID().toString();
		this.timestamp = currentTimeMillis();
	}

	public long getId() {
		return id;
	}

	public String getToken() {
		return token;
	}

	public long getTimestamp() {
		return timestamp;
	}

	public void setTimestamp(long timestamp) {
		this.timestamp = timestamp;
	}
	
}
