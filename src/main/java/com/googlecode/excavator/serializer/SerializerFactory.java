package com.googlecode.excavator.serializer;

import static com.googlecode.excavator.PropertyConfiger.getSerializerName;

import java.util.Map;

import com.google.common.collect.Maps;

/**
 * ���л�����
 * @author vlinux
 *
 */
public class SerializerFactory {

	private static final Map<String, Serializer> serializers = Maps.newConcurrentMap();
	
	/**
	 * Java���л���ʽ
	 */
	public static final String SERIALIZER_NAME_JAVA = "java";
	
	/**
	 * hessian���л���ʽ
	 */
	public static final String SERIALIZER_NAME_HESSIAN = "hessian";
	
	/**
	 * ע�����л���ʽ
	 * @param name
	 * @param serializer
	 */
	public static void register(String name, Serializer serializer) {
		serializers.put(name, serializer);
	}
	
	static {
		
		// ע��java���л��ķ�ʽ
		register(SERIALIZER_NAME_JAVA, new JavaSerializer());
		
		// ע��hessian���л��ķ�ʽ
		register(SERIALIZER_NAME_HESSIAN, new HessianSerializer());
		
	}
	
	/**
	 * ��ȡ���л�������
	 * @return
	 */
	public static Serializer getInstance() {
		final String name = getSerializerName();
		if( serializers.containsKey(name) ) {
			return serializers.get(name);
		}
		return serializers.get(SERIALIZER_NAME_HESSIAN);
	}
	
}
