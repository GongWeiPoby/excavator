package com.googlecode.excavator.protocol;

import java.io.Serializable;

/**
 * rmi����Э��:Ӧ��
 * @author vlinux
 *
 */
public final class RmiResponse extends RmiTracer {

	private static final long serialVersionUID = -2335302314388825625L;

	/**
	 * ���ؽ���룺���óɹ�����������ʽ���ؽ��
	 */
	public static final short RESULT_CODE_SUCCESSED_RETURN = 1;
	
	/**
	 * ���ؽ���룺���óɹ��������쳣��ʽ���ؽ��
	 */
	public static final short RESULT_CODE_SUCCESSED_THROWABLE = 2;
	
	/**
	 * ���ؽ���룺����ʧ�ܣ������̳߳�����
	 */
	public static final short RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW = 4;
	
	/**
	 * ���ؽ���룺����ʧ�ܣ����񲻴���
	 */
	public static final short RESULT_CODE_FAILED_SERVICE_NOT_FOUND = 5;
	
	/**
	 * ���ؽ���룺����ʧ�ܣ�����ʱ
	 */
	public static final short RESULT_CODE_FAILED_TIMEOUT = 6;
	
	private final short code;			//���ؽ����
	private final Serializable object;	//���ؽ��
	
	/**
	 * ����rmiӦ��
	 * @param req Ӧ������Ӧ������<br/>
	 * 	��ʱ���������Ӧ�������������������ͬ��id��token
	 * @param code
	 * @param object
	 */
	public RmiResponse(RmiRequest req, short code, Serializable object) {
		super(req.getId(), req.getToken());
		this.code = code;
		this.object = object;
	}
	
	/**
	 * ����rmiӦ��<br/>
	 * �޷���ֵ�汾�����ڹ����������rmiӦ��,��ʱ����Ҫ�лظ�ֵ
	 * @param req
	 * @param code
	 */
	public RmiResponse(RmiRequest req, short code) {
		super(req.getId(), req.getToken());
		this.code = code;
		this.object = null;
	}

	public short getCode() {
		return code;
	}

	public Serializable getObject() {
		return object;
	}
	
	@Override
	public String toString() {
		return String.format("RESP[id=%s;token=%s;code=%s;]", getId(), getToken(), code );
	}
	
}
