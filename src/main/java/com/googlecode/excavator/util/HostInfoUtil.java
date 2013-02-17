package com.googlecode.excavator.util;

import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import org.apache.commons.lang.StringUtils;

/**
 * ������Ϣ��
 * @author vlinux
 *
 */
public final class HostInfoUtil {

	private static String hostIp = StringUtils.EMPTY;;
	static {
		
        try {
        	final Enumeration<NetworkInterface> netInterfaces = NetworkInterface.getNetworkInterfaces();
            while (netInterfaces.hasMoreElements()) {
                NetworkInterface ni = netInterfaces.nextElement();
                Enumeration<InetAddress> ips = ni.getInetAddresses();
                while (ips.hasMoreElements()) {
                	final String host = ips.nextElement().getHostAddress();
                	if( !host.matches("^127\\..*")
                			&& host.matches("([0-9]{0,3}\\.){3}[0-9]{0,3}")) {
                		hostIp = host;
                		break;
                	}
                }
            }
        } catch (Exception e) {
            //
        }
		
	}
	
	/**
	 * ��ȡ������һ����Чip<br/>
	 * ���û��Чip�����ؿմ�
	 * @return
	 */
	public static String getHostFirstIp() {
		return hostIp;
	}
	
}
