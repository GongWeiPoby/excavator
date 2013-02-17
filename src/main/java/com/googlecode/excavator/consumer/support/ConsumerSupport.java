package com.googlecode.excavator.consumer.support;

import static com.googlecode.excavator.PropertyConfiger.getConsumerConnectTimeout;
import static com.googlecode.excavator.PropertyConfiger.getZkConnectTimeout;
import static com.googlecode.excavator.PropertyConfiger.getZkServerList;
import static com.googlecode.excavator.PropertyConfiger.getZkSessionTimeout;
import static java.lang.String.format;

import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.jboss.netty.channel.Channel;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.googlecode.excavator.Supporter;
import com.googlecode.excavator.constant.Log4jConstant;
import com.googlecode.excavator.consumer.ChannelRing;
import com.googlecode.excavator.consumer.Receiver;
import com.googlecode.excavator.protocol.RmiRequest;

/**
 * ���Ѷ�֧����
 * @author vlinux
 *
 */
public class ConsumerSupport implements Supporter, Receiver, ChannelRing {

	private final Logger logger = Logger.getLogger(Log4jConstant.RECEIVER);
	
	private ChannelRingSupport channelRingSupport;
	private ServiceDiscoverySupport serviceDiscoverySupport;
	private Map<Long,Receiver.Wrapper> wrappers;
	
	@Override
	public void init() throws Exception {

		// new 
		channelRingSupport = new ChannelRingSupport();
		serviceDiscoverySupport = new ServiceDiscoverySupport();
		
		// setter
		channelRingSupport.setConnectTimeout(getConsumerConnectTimeout());
		channelRingSupport.setReceiver(this);
		serviceDiscoverySupport.setZkConnectTimeout(getZkConnectTimeout());
		serviceDiscoverySupport.setZkSessionTimeout(getZkSessionTimeout());
		serviceDiscoverySupport.setZkServerLists(getZkServerList());
		
		// init
		channelRingSupport.init();
		serviceDiscoverySupport.init();
		
		wrappers = Maps.newConcurrentMap();
		
	}

	@Override
	public void destroy() throws Exception {
		if( null != channelRingSupport ) {
			channelRingSupport.destroy();
		}
		if( null != serviceDiscoverySupport ) {
			serviceDiscoverySupport.destroy();
		}
	}

	@Override
	public Receiver.Wrapper register(RmiRequest req) {
		if( wrappers.containsKey(req.getId()) ) {
			// �����ظ���Ͷ��req
			if( logger.isInfoEnabled() ) {
				logger.info(format("an duplicate request existed, this one will ignore. req:%s", req));
			}
			return wrappers.get(req.getId());
		}
		
		final Receiver.Wrapper wrapper = new Receiver.Wrapper(req);
		wrappers.put(req.getId(), wrapper);
		return wrapper;
	}

	@Override
	public Receiver.Wrapper unRegister(long id) {
		return wrappers.remove(id);
	}

	@Override
	public Receiver.Wrapper receive(long id) {
		return wrappers.remove(id);
	}

	@Override
	public ChannelRing.Wrapper ring(
			RmiRequest req) {
		return channelRingSupport.ring(req);
	}

	@Override
	public List<Receiver.Wrapper> unRegister(Channel channel) {
		final List<Receiver.Wrapper> removeWrappers = Lists.newArrayList();
		Iterator<Map.Entry<Long,Receiver.Wrapper>> it = wrappers.entrySet().iterator();
		while( it.hasNext() ) {
			final Map.Entry<Long,Receiver.Wrapper> entry = it.next();
//			final long key = entry.getKey();
			final Receiver.Wrapper wrapper = entry.getValue();
			if( wrapper.getChannel() == channel ) {
				removeWrappers.add(wrapper);
				wrapper.getLock().lock();
				try {
					wrapper.getWaitResp().signal();
				}finally {
					wrapper.getLock().unlock();
				}
				it.remove();
			}
		}
		return removeWrappers;
	}

}
