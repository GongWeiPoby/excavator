package com.googlecode.excavator;

import static com.googlecode.excavator.constant.PropertyConfigerConstant.APPNAME;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.CONSUMER_CONNECT_TIMEOUT;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.MONITOR_ENABLE;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.PROFILER_ENABLE;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.PROFILER_LIMIT;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.PROVIDER_IP;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.PROVIDER_PORT;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.PROVIDER_WORKERS;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.SERIALIZER_NAME;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.TOKEN_ENABLE;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.ZK_CONNECT_TIMEOUT;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.ZK_SERVER_LIST;
import static com.googlecode.excavator.constant.PropertyConfigerConstant.ZK_SESSION_TIMEOUT;
import static com.googlecode.excavator.util.HostInfoUtil.getHostFirstIp;
import static java.lang.String.format;
import static org.apache.commons.lang.StringUtils.isBlank;
import static com.googlecode.excavator.serializer.SerializerFactory.*;

import java.io.InputStream;
import java.net.InetSocketAddress;
import java.util.Properties;

import org.apache.log4j.Logger;

import com.googlecode.excavator.constant.Log4jConstant;


/**
 * ��ָ����property�ļ��л�ȡ������Ϣ
 * @author vlinux
 *
 */
public final class PropertyConfiger {

	private static final String PROPERTY_CLASSPATH = "/excavator.properties";
	private static final Logger logger = Logger.getLogger(Log4jConstant.CONFIG);
	
	private static final Properties properties = new Properties();
	
	// �Ƿ������
	private static boolean isEnableMonitor = false;
	
	// Ӧ������
	private static String appname;
	
	// �����ṩ����������ַ
	private static InetSocketAddress providerAddress;
	
	// �Ƿ�����token
	private static boolean isEnableToken = false;
	
	// �Ƿ��������ܼ��
	private static boolean isEnableProfiler = false;
	
	// ���ܸ澯��ֵ
	private static long profilerLimit = 100;
	
	// ���л���ʽ
	private static String serializerName = SERIALIZER_NAME_HESSIAN;
	
	
	
	
	/**
	 * ��ʼ��
	 */
	static {
		
		InputStream is = null;
		try {
			is = PropertyConfiger.class.getResourceAsStream(PROPERTY_CLASSPATH);
			if( null == is ) {
				throw new IllegalStateException("excavator.properties can not found in the classpath.");
			}
			properties.load(is);
			
			// pre-load
			preloadMonitorEnable();
			preloadAppName();
			preloadTokenEnable();
			preloadProfilerLimit();
			preloadProfilerEnable();
			preloadSerializer();
			
		} catch(Throwable t) {
			logger.warn("load excavator's properties file failed.", t);
		} finally {
			if( null != is ) {
				try {is.close();}catch(Exception e) {}
			}//if
		}
		
	}
	
	
	/**
	 * Ԥ�������л���ʽ
	 */
	private static void preloadSerializer() {
		final String cfgSerName = properties.getProperty(SERIALIZER_NAME);
		if( !isBlank(cfgSerName) ) {
			serializerName = cfgSerName;
		}
		logger.info(format("%s=%s",SERIALIZER_NAME,serializerName));
	}
	
	/**
	 * Ԥ�������ܿ���
	 */
	private static void preloadProfilerEnable() {
		try {
			isEnableProfiler = Boolean.valueOf(properties.getProperty(PROFILER_ENABLE));
		}catch(Exception e) {
			//
		}
		logger.info(format("%s=%s",PROFILER_ENABLE,isEnableProfiler));
	}
	
	/**
	 * Ԥ�������ܸ澯��ֵ
	 */
	private static void preloadProfilerLimit() {
		try {
			profilerLimit = Long.valueOf(properties.getProperty(PROFILER_LIMIT));
		}catch(Exception e) {
			//
		}
		logger.info(format("%s=%s",PROFILER_LIMIT,profilerLimit));
	}
	
	/**
	 * Ԥ����token׷��ѡ��
	 */
	private static void preloadTokenEnable() {
		try {
			isEnableToken = Boolean.valueOf(properties.getProperty(TOKEN_ENABLE));
		}catch(Exception e) {
			//
		}
		logger.info(format("%s=%s",TOKEN_ENABLE,isEnableToken));
	}
	
	/**
	 * �ж��ַ����Ƿ����ip�ĸ�ʽ
	 * @param ip
	 * @return
	 */
	private static boolean isIp(String ip) {
		return !isBlank(ip) 
				&& ip.matches("([0-9]{1,3}\\.{1}){3}[0-9]{1,3}");
	}
	
	/**
	 * ע��provider�����ṩ�����������Ϣ<br/>
	 * �������ֻ�ᱻprovider������
	 */
	private static void preloadAddress() {
		
		final String ip;
		final String cfgIp = properties.getProperty(PROVIDER_IP);
		if( properties.containsKey(PROVIDER_IP) ) {
			// �������д��ip��ַ������ȴ����һ����Ч��ip��ַ���򱨴�
			if( !isIp(cfgIp) ) {
				throw new IllegalArgumentException(
						format("%s=%s isn't an ip", PROVIDER_IP, properties.getProperty(PROVIDER_IP)));
			}
			ip = properties.getProperty(PROVIDER_IP);
		}
		// ���ûָ��ip������������ҵ�һ��
		else {
			ip = getHostFirstIp();
		}
		
		final int port;
		if( ! properties.containsKey(PROVIDER_PORT)
				|| isBlank(properties.getProperty(PROVIDER_PORT))) {
			throw new IllegalArgumentException(format("%s cant' be empty", PROVIDER_PORT));
		}
		
		try {
			port = Integer.valueOf(properties.getProperty(PROVIDER_PORT));
		}catch(Exception e) {
			throw new IllegalArgumentException(
					format("%s=%s illegal", PROVIDER_PORT, properties.getProperty(PROVIDER_PORT)), e);
		}
		
		providerAddress = new InetSocketAddress(ip,port);
		
		logger.info(format("address=%s",providerAddress));
	}
	
	/**
	 * Ԥ����monitor_enableѡ��<br/>
	 * �Ǳ��Ĭ��Ϊfalse�������򿪼��
	 */
	private static void preloadMonitorEnable() {
		try {
			isEnableMonitor = Boolean.valueOf(properties.getProperty(MONITOR_ENABLE));
		}catch(Exception e) {
			//
		} finally {
			logger.info(format("%s=%s",MONITOR_ENABLE,isEnableMonitor));
		}
	}
	
	/**
	 * ע�뱾��Ӧ����<br/>
	 * ����ҽ������ַ�������
	 */
	private static void preloadAppName() {
		if( ! properties.containsKey(APPNAME)
				|| isBlank(appname = properties.getProperty(APPNAME))) {
			throw new IllegalArgumentException(format("%s can't be empty", APPNAME));
		}
		if( !appname.matches("[\\w|-]+") ) {
			throw new IllegalArgumentException(format("%s must in [A-z0-9]", APPNAME));
		}
		logger.info(format("%s=%s",APPNAME,appname));
	}
	
	/**
	 * �����ػ�ȡ������ṩ�����address
	 * @return
	 */
	public final static InetSocketAddress getProviderAddress() {
		// ����û̫�ಢ������ҿ��Ի������ĺ���dcl
		if( null == providerAddress ) {
			synchronized (PropertyConfiger.class) {
				if( null == providerAddress ) {
					preloadAddress();
				}
			}
		}
		return providerAddress;
	}
	
	/**
	 * ��ȡ����˹����߳���
	 * @return
	 */
	public static int getProviderWorkers() {
		return Integer.valueOf(properties.getProperty(PROVIDER_WORKERS));
	}
	
	/**
	 * ��ȡӦ����
	 * @return
	 */
	public static String getAppName() {
		return appname;
	}
	
	/**
	 * ��ȡzk���ӳ�ʱʱ��
	 * @return
	 */
	public static int getZkConnectTimeout() {
		return Integer.valueOf(properties.getProperty(ZK_CONNECT_TIMEOUT));
	}
	
	/**
	 * ��ȡzk�Ự��ʱʱ��
	 * @return
	 */
	public static int getZkSessionTimeout() {
		return Integer.valueOf(properties.getProperty(ZK_SESSION_TIMEOUT));
	}
	
	/**
	 * ��ȡzk�ķ������б�
	 * @return
	 */
	public static String getZkServerList() {
		return properties.getProperty(ZK_SERVER_LIST);
	}
	
	/**
	 * �ͻ��˻�ȡ���ã����ӳ�ʱʱ��
	 * @return
	 */
	public static int getConsumerConnectTimeout() {
		return Integer.valueOf(properties.getProperty(CONSUMER_CONNECT_TIMEOUT));
	}
	
	/**
	 * �Ƿ������
	 * @return
	 */
	public static boolean isEnableMonitor() {
		return isEnableMonitor;
	}
	
	/**
	 * �Ƿ���token����<br/>
	 * RmiTrace��token���ɲ���<br/>
	 * false�������ڲ�����������token(UUID) true��������Է�����token��RmiTrace�����Զ���æ����һ���µ�token
	 * 
	 * @return
	 */
	public static boolean isEnableToken() {
		return isEnableToken;
	}

	/**
	 * ��ȡ���ܸ澯��ֵ<br/>
	 * ��timeout-costС�ڵ���limitʱ��ӡ���澯��Ϣ
	 * @return
	 */
	public static long getProfilerLimit() {
		return profilerLimit;
	}

	/**
	 * ��ȡ���ܿ���
	 * @return
	 */
	public static boolean isEnableProfiler() {
		return isEnableProfiler;
	}
	
	/**
	 * ȡ�����л���ʽ����
	 * @return
	 */
	public static String getSerializerName() {
		return serializerName;
	}
	
}
