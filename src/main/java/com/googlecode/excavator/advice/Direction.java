package com.googlecode.excavator.advice;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * ���֪ͨ�ķ���
 *
 * @author vlinux
 *
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE})
public @interface Direction {

    /**
     * ֪ͨ����
     *
     * @author vlinux
     *
     */
    public static enum Type {

        /**
         * �ͻ���
         */
        CONSUMER,
        /**
         * �����
         */
        PROVIDER

    }

    /**
     * ֪ͨ����
     *
     * @return
     */
    Type[] types() default {Type.CONSUMER, Type.PROVIDER};

}
