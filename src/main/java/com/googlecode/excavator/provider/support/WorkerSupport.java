package com.googlecode.excavator.provider.support;

import static com.googlecode.excavator.PropertyConfiger.getAppName;
import static com.googlecode.excavator.advice.Advices.doAfter;
import static com.googlecode.excavator.advice.Advices.doBefores;
import static com.googlecode.excavator.advice.Advices.doFinally;
import static com.googlecode.excavator.advice.Advices.doThrow;
import static com.googlecode.excavator.advice.Direction.Type.PROVIDER;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_SERVICE_NOT_FOUND;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_FAILED_TIMEOUT;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_SUCCESSED_RETURN;
import static com.googlecode.excavator.protocol.RmiResponse.RESULT_CODE_SUCCESSED_THROWABLE;
import static java.lang.String.format;
import static java.lang.System.currentTimeMillis;

import java.io.Serializable;
import java.lang.reflect.Method;
import java.net.InetSocketAddress;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Semaphore;
import java.util.concurrent.ThreadFactory;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;

import com.google.common.collect.Maps;
import com.googlecode.excavator.Runtimes.Runtime;
import com.googlecode.excavator.Supporter;
import com.googlecode.excavator.constant.Log4jConstant;
import com.googlecode.excavator.message.Message;
import com.googlecode.excavator.message.MessageSubscriber;
import com.googlecode.excavator.message.Messages;
import com.googlecode.excavator.protocol.RmiRequest;
import com.googlecode.excavator.protocol.RmiResponse;
import com.googlecode.excavator.provider.BusinessWorker;
import com.googlecode.excavator.provider.ProviderService;
import com.googlecode.excavator.provider.message.RegisterServiceMessage;

/**
 * �����߳�֧��
 * @author vlinux
 *
 */
public class WorkerSupport implements Supporter, MessageSubscriber,
		BusinessWorker {

	private final Logger logger = Logger.getLogger(Log4jConstant.WORKER);

	private int poolSize; 							// ҵ��ִ���߳�����
	private ExecutorService businessExecutor; 		// ҵ��ִ�����߳�
	private Semaphore semaphore; 					// �����ź���
	private Map<String, ProviderService> services; 	// �����б�

	@Override
	public void init() throws Exception {

		Messages.register(this, RegisterServiceMessage.class);
		
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

		if (logger.isInfoEnabled()) {
			logger.info(format("worker init thread_pool size:%s", poolSize));
		}

	}

	@Override
	public void destroy() throws Exception {
		if( null != businessExecutor ) {
			businessExecutor.shutdown();
		}
	}

	@Override
	public void receive(Message<?> msg) throws Exception {

		if( !(msg instanceof RegisterServiceMessage) ) {
			return;
		}
		
		RegisterServiceMessage rsMsg = (RegisterServiceMessage)msg;
		ProviderService service = rsMsg.getContent();
		services.put(service.getKey(), service);
		
	}

	/**
	 * ������ִ�о���ĵ���
	 * @param req
	 * @param channel
	 * @param service
	 */
	private void doWork(RmiRequest req, Channel channel, ProviderService service) {
		final long start = currentTimeMillis();
		final Object serviceObj = service.getServiceObject();
		final Method serviceMtd = service.getServiceMethod();
		
		// ��������ʱ����
		final Runtime runtime = new Runtime(req, getAppName(), 
				service.getServiceItf(), serviceMtd,
				(InetSocketAddress)channel.getRemoteAddress(), 
				(InetSocketAddress)channel.getLocalAddress());
		
		// �ɻ
		try {
			
			doBefores(PROVIDER, runtime);
			
			try {
				final Serializable returnObj = (Serializable) serviceMtd.invoke(serviceObj, (Object[]) req.getArgs());
				doAfter(PROVIDER, runtime, returnObj, currentTimeMillis() - start/*cost*/);
				handleNormal(returnObj, req, channel);
			}catch(Throwable t) {
				throw t.getCause().getCause().getCause();
			}
			
		} catch (Throwable t) {
			doThrow(PROVIDER, runtime, t);
			handleThrowable(t, req, channel);
		} finally {
			semaphore.release();
			doFinally(PROVIDER, runtime);
		}//try
	}
	
	@Override
	public void work(final RmiRequest req, final Channel channel) {
		
		final String key = req.getKey();
		
		// ���񲻴���
		final ProviderService service = services.get(key);
		if( null == service ) {
			handleServiceNotFound(req, channel);
			return;
		}
		
		// �����ʱ�ˣ����ߴ�������ʱ
		if( isReqTimeout(req.getTimestamp(), req.getTimeout(), service.getTimeout()) ) {
			handleTimeout(req, channel);
			return;
		}
		
		// �߳���������
		if( !semaphore.tryAcquire() ) {
			handleOverflow(req, channel);
			return;
		}
		
		businessExecutor.execute(new Runnable(){

			@Override
			public void run() {
				
				doWork(req, channel, service);
				
			}
			
		});
		
	}
	
	/**
	 * ����������񲻴���
	 * @param req
	 * @param channel
	 */
	private void handleServiceNotFound(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_SERVICE_NOT_FOUND);
		channel.write(respEvt);
	}
	
	/**
	 * �����Ƿ��Ѿ���ʱ
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
	 * @param req
	 * @param channel
	 */
	private void handleTimeout(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_TIMEOUT);
		channel.write(respEvt);
	}
	
	/**
	 * ��������return���ص����
	 * @param returnObj
	 * @param req
	 * @param channel
	 */
	private final void handleNormal(Serializable returnObj, RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_SUCCESSED_RETURN, returnObj);
		channel.write(respEvt);
	}
	
	/**
	 * ���������쳣���ص����
	 * @param returnObj 
	 * @param req
	 * @param channel
	 */
	private void handleThrowable(Serializable returnObj, RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_SUCCESSED_THROWABLE, returnObj);
		channel.write(respEvt);
	}
	
	/**
	 * �����̳߳����쳣
	 * @param req
	 * @param channel
	 */
	private void handleOverflow(RmiRequest req, Channel channel) {
		RmiResponse respEvt = new RmiResponse(req, RESULT_CODE_FAILED_BIZ_THREAD_POOL_OVERFLOW);
		channel.write(respEvt);
	}

	public void setPoolSize(int poolSize) {
		this.poolSize = poolSize;
	}

}
