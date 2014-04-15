package com.googlecode.excavator.provider;

import static com.googlecode.excavator.util.SerializerUtil.isSerializableType;
import static com.googlecode.excavator.util.SignatureUtil.signature;
import static com.googlecode.excavator.util.TimeoutUtil.getFixTimeout;
import static org.apache.commons.lang.StringUtils.isBlank;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Map;

import org.apache.log4j.Logger;

import com.googlecode.excavator.constant.Log4jConstant;
import com.googlecode.excavator.message.MemeryMessager;
import com.googlecode.excavator.message.Messager;
import com.googlecode.excavator.provider.message.RegisterServiceMessage;
import com.googlecode.excavator.provider.support.ProviderSupport;

/**
 * ����˴�����
 *
 * @author vlinux
 *
 */
public class ProviderProxyFactory {

    private final Logger agentLog = Logger.getLogger(Log4jConstant.AGENT);

    private ProviderSupport support;
    private Messager messager;

    private ProviderProxyFactory() throws Exception {
        messager = new MemeryMessager();
        support = new ProviderSupport(messager);
        support.init();
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    support.destroy();
                } catch (Exception e) {
                    agentLog.warn("destory consumer support failed.", e);
                }
            }
        });
    }

    /**
     * ���ɷ���˴������
     *
     * @param targetObject
     * @return
     */
    private InvocationHandler createProviderProxyHandler(final Object targetObject) {
        return new InvocationHandler() {

            @Override
            public Object invoke(Object proxy, Method method, Object[] args)
                    throws Throwable {
                return method.invoke(targetObject, args);
            }

        };
    }

    /**
     * ���ɴ������
     *
     * @param targetInterface
     * @param targetObject
     * @param group
     * @param version
     * @param defaultTimeout
     * @param methodTimeoutMap
     * @return
     * @throws Exception
     */
    @SuppressWarnings("unchecked")
    public <T> T proxy(Class<T> targetInterface, Object targetObject, String group, String version,
            long defaultTimeout, Map<String, Long> methodTimeoutMap)
            throws Exception {

        check(targetInterface, targetObject, group, version);

        final InvocationHandler providerProxyHandler = createProviderProxyHandler(targetObject);

        // ����Ŀ��������
        Object proxyObject = Proxy.newProxyInstance(
                Thread.currentThread().getContextClassLoader(),
                new Class<?>[]{targetInterface}, providerProxyHandler);

        publishService(targetInterface, group, version, proxyObject, defaultTimeout, methodTimeoutMap);

        return (T) proxyObject;
    }

    /**
     * ע�����У��
     */
    private void check(Class<?> targetInterface, Object targetObject, String group, String version) {

        // ������У��
        if (isBlank(version)) {
            throw new IllegalArgumentException("version is blank");
        }
        if (isBlank(group)) {
            throw new IllegalArgumentException("group is blank");
        }
        if (null == targetInterface) {
            throw new IllegalArgumentException("targetInterface is null");
        }
        if (null == targetObject) {
            throw new IllegalArgumentException("targetObject is null");
        }

        // ��鴫�ݽ����Ĵ���Ŀ������Ƿ�ʵ���˽ӿ�
        if (!targetInterface.isAssignableFrom(targetObject.getClass())) {
            throw new IllegalArgumentException(
                    String.format("targetObject[%s] does not instanceof proxyInterface[%s]",
                            targetObject.getClass(),
                            targetInterface.getClass()));
        }

        // ��鴫�ݽ����Ľӿڷ����У��Ƿ�����в������л��Ķ�������
        Method[] methods = targetInterface.getMethods();
        if (null != methods) {
            for (Method method : methods) {
                if (!isSerializableType(method.getReturnType())) {
                    throw new IllegalArgumentException("method returnType is not serializable");
                }
                if (!isSerializableType(method.getParameterTypes())) {
                    throw new IllegalArgumentException("method parameter is not serializable");
                }
            }//for
        }//if

    }

    /**
     * ��������
     *
     * @param targetInterface
     * @param group
     * @param version
     * @param proxyObject
     * @param defaultTimeout
     * @param methodTimeoutMap
     */
    private void publishService(Class<?> targetInterface, String group, String version, Object proxyObject, long defaultTimeout, Map<String, Long> methodTimeoutMap) {
        Method[] methods = targetInterface.getMethods();
        if (null == methods) {
            return;
        }

        for (Method method : methods) {
            final String sign = signature(method);
            final ProviderService providerService = new ProviderService(
                    group, version, sign, targetInterface, proxyObject, method, getFixTimeout(method, defaultTimeout, methodTimeoutMap));
            messager.post(new RegisterServiceMessage(providerService));
        }
    }

    private static volatile ProviderProxyFactory singleton;

    /**
     * ����
     *
     * @return
     * @throws Exception
     */
    public static ProviderProxyFactory singleton() throws Exception {
        if (null == singleton) {
            synchronized (ProviderProxyFactory.class) {
                if (null == singleton) {
                    singleton = new ProviderProxyFactory();
                }
            }
        }
        return singleton;
    }

}
