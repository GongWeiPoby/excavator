package com.googlecode.excavator.provider.support;

import static com.googlecode.excavator.PropertyConfiger.getAppName;
import static com.googlecode.excavator.advice.Advices.doAfter;
import static com.googlecode.excavator.advice.Advices.doBefores;
import static com.googlecode.excavator.advice.Advices.doFinally;
import static com.googlecode.excavator.advice.Advices.doThrow;
import static com.googlecode.excavator.advice.Direction.Type.PROVIDER;
import static com.googlecode.excavator.protocol.Protocol.TYPE_RMI;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_SERVICE_NOT_FOUND;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_TIMEOUT;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_SUCCESSED_RETURN;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_SUCCESSED_THROWABLE;
import static java.lang.System.currentTimeMillis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import org.jboss.netty.channel.Channel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.googlecode.excavator.Runtimes.Runtime;
import com.googlecode.excavator.Supporter;
import com.googlecode.excavator.constant.LogConstant;
import com.googlecode.excavator.message.Message;
import com.googlecode.excavator.message.MessageSubscriber;
import com.googlecode.excavator.message.Messager;
import com.googlecode.excavator.protocol.Protocol;
import com.googlecode.excavator.protocol.RmiRequest;
import com.googlecode.excavator.protocol.RmiResponse;
import com.googlecode.excavator.protocol.RmiTracer;
import com.googlecode.excavator.provider.BusinessWorker;
import com.googlecode.excavator.provider.ProviderService;
import com.googlecode.excavator.provider.message.RegisterServiceMessage;
import com.googlecode.excavator.serializer.SerializationException;
import com.googlecode.excavator.serializer.Serializer;
import com.googlecode.excavator.serializer.SerializerFactory;

/**
 * �����߳�֧��
 *
 * @author vlinux
 *
 */
public class WorkerSupport implements Supporter, MessageSubscriber,
        BusinessWorker {

    private final Logger logger = LoggerFactory.getLogger(LogConstant.WORKER);

    private final Messager messager;
    private final int poolSize; 					// ҵ��ִ���߳�����
    private final Serializer serializer = SerializerFactory.getInstance();
    
    private ExecutorService businessExecutor; 		// ҵ��ִ�����߳�
    private Semaphore semaphore; 					// �����ź���
    private Map<String, ProviderService> services; 	// �����б�

    /**
     * ���캯��
     * @param messager
     * @param poolSize
     */
    public WorkerSupport(Messager messager, int poolSize) {
        this.messager = messager;
        this.poolSize = poolSize;
    }

    @Override
    public void init() throws Exception {

        messager.register(this, RegisterServiceMessage.class);

        // ��ʼ�������б�
        services = Maps.newConcurrentMap();

        // ִ���̳߳�
        businessExecutor = Executors.newCachedThreadPool(new ThreadFactory() {

            @Override
            public Thread newThread(Runnable r) {
                return new Thread(r, "excavator-biz-worker");
            }

        });

        // ��ʼ���ź���
        semaphore = new Semaphore(poolSize);

        logger.info("worker init thread_pool size:{}", poolSize);

    }

    @Override
    public void destroy() throws Exception {
        if (null != businessExecutor) {
            businessExecutor.shutdown();
        }
    }

    @Override
    public void receive(Message<?> msg) throws Exception {

        if (!(msg instanceof RegisterServiceMessage)) {
            return;
        }

        RegisterServiceMessage rsMsg = (RegisterServiceMessage) msg;
        ProviderService service = rsMsg.getContent();
        services.put(service.getKey(), service);

    }

    /**
     * ������ִ�о���ĵ���
     * @param reqPro
     * @param req
     * @param channel
     * @param service
     */
    private void doWork(Protocol reqPro, RmiRequest req, Channel channel, ProviderService service) {
        final long start = currentTimeMillis();
        final Object serviceObj = service.getServiceObject();
        final Method serviceMtd = service.getServiceMethod();

        // ��������ʱ����
        final Runtime runtime = new Runtime(req, getAppName(),
                service.getServiceItf(), serviceMtd,
                (InetSocketAddress) channel.getRemoteAddress(),
                (InetSocketAddress) channel.getLocalAddress());

        // �ɻ
        try {

            doBefores(PROVIDER, runtime);

            try {
                final Serializable returnObj = (Serializable) serviceMtd.invoke(serviceObj, (Object[]) req.getArgs());
                doAfter(PROVIDER, runtime, returnObj, currentTimeMillis() - start/*cost*/);
                handleNormal(reqPro, returnObj, req, channel);
            } catch (Throwable t) {
                throw t.getCause().getCause().getCause();
            }

        } catch (Throwable t) {
            doThrow(PROVIDER, runtime, t);
            handleThrowable(reqPro, t, req, channel);
        } finally {
            semaphore.release();
            doFinally(PROVIDER, runtime);
        }//try
    }

    @Override
    public void work(final Protocol reqPro, final Channel channel) {

        businessExecutor.execute(new Runnable() {

            /**
             * ��Э�����ת��Ϊ�������
             * @param reqPro
             * @return
             */
            private RmiRequest convertToReq(Protocol reqPro) {
                if (reqPro.getType() != TYPE_RMI) {
                    logger.warn("ingore this request, because proto.type was {}, need TYPE_RMI. remote={};", reqPro.getType(), channel.getRemoteAddress());
                    return null;
                }
                final RmiTracer rmiTracer;
                try {
                    rmiTracer = serializer.decode(reqPro.getDatas());
                } catch (SerializationException e) {
                    logger.warn("ingore this request, because decode failed. remote={}", channel.getRemoteAddress(), e);
                    return null;
                }
                if( null == rmiTracer ) {
                    // ����Ӧ�ò������ߵ�
                    logger.warn("ingore this request, because it was null.");
                    return null;
                }
                if( !(rmiTracer instanceof RmiRequest) ) {
                    logger.warn("ingore this request, because rmiTracer's class was {}, need RmiRequest. remote={};", 
                        rmiTracer.getClass().getSimpleName(), 
                        channel.getRemoteAddress());
                    return null;
                }
                return (RmiRequest)rmiTracer;
            }
            
            @Override
            public void run() {
                
                final RmiRequest req = convertToReq(reqPro);
                if( null == req ) {
                    logger.warn("convertToReq failed. remote={};", channel.getRemoteAddress());
                    // ����Э�����͵ĳ�������ֻ�ܺ��ź��Ĵ�ӡһ��log��Ȼ��ȴ��ͻ��˳�ʱ
                    return;
                }
                
                final String key = req.getKey();

                // ���񲻴���
                final ProviderService service = services.get(key);
                if (null == service) {
                    handleServiceNotFound(reqPro, req, channel);
                    return;
                }

                // �����ʱ�ˣ����ߴ�������ʱ
                if (isReqTimeout(req.getTimestamp(), req.getTimeout(), service.getTimeout())) {
                    handleTimeout(reqPro, req, channel);
                    return;
                }

                // �߳���������
                if (!semaphore.tryAcquire()) {
                    handleOverflow(reqPro, req, channel);
                    return;
                }
                
                doWork(reqPro, req, channel, service);

            }

        });

    }

    /**
     * �����л���д�뷵����Ϣ
     * @param reqPro
     * @param resp
     * @param channel
     */
    private void writeResponse(Protocol reqPro, RmiResponse resp, Channel channel) {
        try {
            RmiTracer rmi = (RmiTracer) resp;
            Protocol pro = new Protocol();
            pro.setId(reqPro.getId());
            pro.setType(TYPE_RMI);
            byte[] datas = serializer.encode(rmi);
            pro.setLength(datas.length);
            pro.setDatas(datas);
            channel.write(pro);
        } catch (SerializationException e) {
            logger.warn("write response failed, because serializer failed. resp={};remote={};",
                new Object[]{resp,channel.getRemoteAddress(),e});
        }
        
    }
    
    /**
     * ����������񲻴���
     * @param reqPro
     * @param req
     * @param channel
     */
    private void handleServiceNotFound(Protocol reqPro, RmiRequest req, Channel channel) {
        RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_SERVICE_NOT_FOUND);
        writeResponse(reqPro, respEvt, channel);
    }

    /**
     * �����Ƿ��Ѿ���ʱ
     *
     * @param reqTimestamp
     * @param reqTimeout
     * @param proTimeout
     * @return
     */
    private final boolean isReqTimeout(long reqTimestamp, long reqTimeout, long proTimeout) {
        final long nowTimestamp = System.currentTimeMillis();
        return nowTimestamp - reqTimestamp > Math.min(reqTimeout, proTimeout);
    }
    
    /**
     * ����ʱ���
     * @param reqPro
     * @param req
     * @param channel
     */
    private void handleTimeout(Protocol reqPro, RmiRequest req, Channel channel) {
        RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_TIMEOUT);
        writeResponse(reqPro, respEvt, channel);
    }

    /**
     * ��������return���ص����
     * @param reqPro
     * @param returnObj
     * @param req
     * @param channel
     */
    private final void handleNormal(Protocol reqPro, Serializable returnObj, RmiRequest req, Channel channel) {
        RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_SUCCESSED_RETURN, returnObj);
        writeResponse(reqPro, respEvt, channel);
    }

    /**
     * ���������쳣���ص����
     * @param reqPro
     * @param returnObj
     * @param req
     * @param channel
     */
    private void handleThrowable(Protocol reqPro, Serializable returnObj, RmiRequest req, Channel channel) {
        RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_SUCCESSED_THROWABLE, returnObj);
        writeResponse(reqPro, respEvt, channel);
    }

    /**
     * �����̳߳����쳣
     * @param reqPro
     * @param req
     * @param channel
     */
    private void handleOverflow(Protocol reqPro, RmiRequest req, Channel channel) {
        RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW);
        writeResponse(reqPro, respEvt, channel);
    }

}
