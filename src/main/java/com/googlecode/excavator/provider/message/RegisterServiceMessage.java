package com.googlecode.excavator.provider.message;

import com.googlecode.excavator.message.Message;
import com.googlecode.excavator.provider.ProviderService;

/**
 * ����ע����Ϣ<br/>
 * �յ�������Ϣ�����յ�����Ҫ���������ע����Ϣ��֪ͨ
 * @author vlinux
 *
 */
public class RegisterServiceMessage extends Message<ProviderService> {

	public RegisterServiceMessage(ProviderService t) {
		super(t);
	}

}
