package com.googlecode.excavator.util;

import java.io.Serializable;

/**
 * ���л�����
 *
 * @author vlinux
 *
 */
public final class SerializerUtil {

    /**
     * �ж�һ�������Ƿ�����л�
     *
     * @param type
     * @return
     */
    public static boolean isSerializableType(Class<?> type) {

        // void �ǿ����л���
        if (Void.class.isAssignableFrom(type)) {
            return true;
        }

        // ���type��Object�����࣬�����Ҫ����ʵ��Serializable�ӿ�
        if (Object.class.isAssignableFrom(type)
                && !Serializable.class.isAssignableFrom(type)) {
            return false;
        }

        // ����������ֻ������͡����Ѿ�ʵ�������л��ӿڵ�Object�������ǿ����л���
        return true;
    }

    /**
     * �ж�һ�������Ƿ�����л�
     *
     * @param types
     * @return
     */
    public static boolean isSerializableType(Class<?>... types) {

        // ������ݵ��б�Ϊ�գ��ռ��ǿ��Խ������л���
        if (null == types
                || types.length == 0) {
            return true;
        }

        for (Class<?> type : types) {
            if (!isSerializableType(type)) {
                return false;
            }
        }

        return true;
    }

    /**
     * �������ת��Ϊ���л�����
     *
     * @param args
     * @return
     */
    public static Serializable[] changeToSerializable(Object[] args) {
        if (null == args) {
            return null;
        }
        Serializable[] serializables = new Serializable[args.length];
        for (int index = 0; index < args.length; index++) {
            serializables[index] = (Serializable) args[index];
        }
        return serializables;
    }

}
