package com.googlecode.excavator.advice;

import java.util.Set;

import com.google.common.collect.Sets;
import com.googlecode.excavator.Runtimes;

/**
 * ֪ͨ
 *
 * @author vlinux
 *
 */
public class Advices {

    /**
     * ֪ͨ��
     *
     * @author vlinux
     *
     */
    public static interface Advice {

        /**
         * ǰ��֪ͨ
         *
         * @param runtime
         * @throws Throwable
         */
        void doBefore(Runtimes.Runtime runtime) throws Throwable;

        /**
         * ����֪ͨ
         *
         * @param runtime
         * @param returnObj
         * @param cost
         * @throws Throwable
         */
        void doAfter(Runtimes.Runtime runtime, Object returnObj, long cost);

        /**
         * �쳣֪ͨ
         *
         * @param runtime
         * @param throwable
         */
        void doThrow(Runtimes.Runtime runtime, Throwable throwable);

        /**
         * ���֪ͨ
         *
         * @param runtime
         */
        void doFinally(Runtimes.Runtime runtime);

    }

    private static final Set<Advice> consumerAdvices = Sets.newLinkedHashSet();
    private static final Set<Advice> providerAdvices = Sets.newLinkedHashSet();

    /**
     * ע��һ��֪ͨ��
     *
     * @param advice
     */
    public static void register(Advice advice) {

        final Direction direction = advice.getClass().getAnnotation(Direction.class);

        // ���û������ܵ�
        if (null == direction) {
            return;
        }

        for (Direction.Type type : direction.types()) {

            // ��������Ϊconsumer�������consumer��
            if (type == Direction.Type.CONSUMER) {
                consumerAdvices.add(advice);
                continue;
            }

            if (type == Direction.Type.PROVIDER) {
                providerAdvices.add(advice);
                continue;
            }

        }//for

    }

    /**
     * Ĭ��֪ͨע��
     */
    static {

        // ע��runtime������֪ͨ��
        register(new RuntimeAdvice());

        // ע����֪ͨ��
        register(new ConsumerMonitorAdvice());
        register(new ProviderMonitorAdvice());

        // ע������֪ͨ��
        register(new ProfilerAdvice());

    }

    /**
     * ѡ��һ��֪ͨ
     *
     * @param type
     * @return
     */
    private final static Set<Advice> switchAdvices(Direction.Type type) {
        if (type == Direction.Type.CONSUMER) {
            return consumerAdvices;
        }

        if (type == Direction.Type.PROVIDER) {
            return providerAdvices;
        }

        // �ⲻ�ᷢ��
        return Sets.newLinkedHashSet();
    }

    /**
     * ǰ��֪ͨ
     *
     * @param type
     * @param runtime
     * @throws Throwable
     */
    public static void doBefores(Direction.Type type, Runtimes.Runtime runtime) throws Throwable {
        for (Advice advice : switchAdvices(type)) {
            advice.doBefore(runtime);
        }
    }

    /**
     * ����֪ͨ
     *
     * @param type
     * @param runtime
     */
    public static void doAfter(Direction.Type type, Runtimes.Runtime runtime, Object returnObj, long cost) {
        for (Advice advice : switchAdvices(type)) {
            try {
                advice.doAfter(runtime, returnObj, cost);
            } catch (Throwable t) {
                //
            }
        }
    }

    /**
     * �쳣֪ͨ
     *
     * @param type
     * @param runtime
     * @param throwable
     */
    public static void doThrow(Direction.Type type, Runtimes.Runtime runtime, Throwable throwable) {
        for (Advice advice : switchAdvices(type)) {
            try {
                advice.doThrow(runtime, throwable);
            } catch (Throwable t) {
                //
            }
        }
    }

    /**
     * ����֪ͨ
     *
     * @param type
     * @param runtime
     */
    public static void doFinally(Direction.Type type, Runtimes.Runtime runtime) {
        for (Advice advice : switchAdvices(type)) {
            try {
                advice.doFinally(runtime);
            } catch (Throwable t) {
                //
            }
        }
    }

}
