package com.googlecode.excavator.provider;

import java.lang.reflect.Method;

/**
 * �����ṩ����service���ݽṹ
 * @author vlinux
 *
 */
public final class ProviderService {

	private final String key;			//group+version+sign
	private final String group;			//�������
	private final String version;		//����汾
	private final String sign;			//����ǩ��
	private final Class<?> serviceItf;	//����ӿ�
	private final Object serviceObject;	//�������
	private final Method serviceMethod;	//���񷽷�
	private final long timeout;			//��ʱʱ��

	/**
	 * �������˶�service������
	 * @param group
	 * @param version
	 * @param sign
	 * @param serviceObject
	 * @param serviceMethod
	 * @param timeout
	 */
	public ProviderService(
			String group, String version, String sign,
			Class<?> serviceItf, Object serviceObject, Method serviceMethod, long timeout) {
		this.group = group;
		this.version = version;
		this.sign = sign;
		this.serviceItf = serviceItf;
		this.serviceObject = serviceObject;
		this.serviceMethod = serviceMethod;
		this.timeout = timeout;
		this.key = group+version+sign;
	}

	public String getKey() {
		return key;
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

	public Object getServiceObject() {
		return serviceObject;
	}

	public Method getServiceMethod() {
		return serviceMethod;
	}

	public long getTimeout() {
		return timeout;
	}
	
	public Class<?> getServiceItf() {
		return serviceItf;
	}

	public String toString() {
		return String.format("group=%s;version=%s;sign=%s;timeout=%s", 
				group,version,sign,timeout);
	}
	
}
