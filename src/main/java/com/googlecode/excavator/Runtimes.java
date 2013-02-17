package com.googlecode.excavator;

import java.lang.reflect.Method;
import java.net.InetSocketAddress;

import com.googlecode.excavator.protocol.RmiRequest;

/**
 * �ھ������ʱruntime
 * @author vlinux
 *
 */
public final class Runtimes {

	/**
	 * ����ʱ״̬
	 */
	private static ThreadLocal<Runtime> runtimes = new ThreadLocal<Runtime>();
	
	/**
	 * ��ȡ��ǰ����ʱ״̬
	 * @return
	 */
	public static Runtime getRuntime() {
		return runtimes.get();
	}

	/**
	 * ע��runtime
	 * @param runtime
	 * @return
	 */
	public static Runtime inject(Runtime runtime) {
		runtimes.set(runtime);
		return runtime;
	}
	
	/**
	 * �Ƴ���ǰruntime
	 * @return
	 */
	public static Runtime remove() {
		Runtime runtime = runtimes.get();
		if( null != runtime ) {
			runtimes.remove();
		}
		return runtime;
	}
	
	/**
	 * runtime��Ϣ
	 * @author vlinux
	 *
	 */
	public static final class Runtime {

		private final RmiRequest req;				//���ѷ�����
		private final String consumer;				//����Ӧ����
		private final String provider;				//����Ӧ����
		private final Class<?> serviceInterface;	//����ӿ�
		private final Method serviceMtd;			//���񷽷�
		private final InetSocketAddress consumerAddress;	//���������ַ(ip:port)
		private final InetSocketAddress providerAddress;	//���������ַ(ip:port)
		
		public Runtime(RmiRequest req,
				String provider,
				Class<?> serviceInterface, Method serviceMtd,
				InetSocketAddress consumerAddress,
				InetSocketAddress providerAddress) {
			this.consumer = req.getAppName();
			this.provider = provider;
			this.serviceInterface = serviceInterface;
			this.serviceMtd = serviceMtd;
			this.consumerAddress = consumerAddress;
			this.providerAddress = providerAddress;
			this.req = req;
		}
		
		public Runtime(RmiRequest req, Class<?> serviceItf, Method serviceMtd) {
			this(req, null, serviceItf, serviceMtd, null, null);
		}

		public String getConsumer() {
			return consumer;
		}

		public String getProvider() {
			return provider;
		}

		public InetSocketAddress getConsumerAddress() {
			return consumerAddress;
		}

		public InetSocketAddress getProviderAddress() {
			return providerAddress;
		}

		public RmiRequest getReq() {
			return req;
		}

		public Class<?> getServiceInterface() {
			return serviceInterface;
		}

		public Method getServiceMtd() {
			return serviceMtd;
		}
		
	}
	
}
