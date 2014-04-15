package com.googlecode.excavator.consumer.support;

import static com.googlecode.excavator.consumer.message.ChannelChangedMessage.Type.CREATE;
import static com.googlecode.excavator.consumer.message.ChannelChangedMessage.Type.REMOVE;
import static com.netflix.curator.framework.CuratorFrameworkFactory.newClient;

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.googlecode.excavator.Supporter;
import com.googlecode.excavator.constant.LogConstant;
import com.googlecode.excavator.consumer.ConsumerService;
import com.googlecode.excavator.consumer.message.ChannelChangedMessage;
import com.googlecode.excavator.consumer.message.SubscribeServiceMessage;
import com.googlecode.excavator.message.Message;
import com.googlecode.excavator.message.MessageSubscriber;
import com.googlecode.excavator.message.Messager;
import com.netflix.curator.framework.CuratorFramework;
import com.netflix.curator.framework.recipes.cache.PathChildrenCache;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheEvent;
import com.netflix.curator.framework.recipes.cache.PathChildrenCacheListener;
import com.netflix.curator.retry.ExponentialBackoffRetry;

/**
 * ������֧��
 * @author vlinux
 *
 */
public class ServiceDiscoverySupport implements Supporter, MessageSubscriber {

    private final Logger logger = LoggerFactory.getLogger(LogConstant.ZK);

    private final String zkServerLists;	//��������ַ�б�
    private final int zkConnectTimeout;	//���ӳ�ʱʱ��
    private final int zkSessionTimeout;	//�Ự��ʱʱ��
    private final Messager messager;
    
    private CuratorFramework client;
    private Map<String, ConsumerService> services;
    private List<PathChildrenCache> pathChildrenCaches = new ArrayList<PathChildrenCache>();

    /**
     * ���캯��
     * @param zkServerLists
     * @param zkConnectTimeout
     * @param zkSessionTimeout
     * @param messager
     */
    public ServiceDiscoverySupport(String zkServerLists, int zkConnectTimeout, int zkSessionTimeout,
            Messager messager) {
        this.zkServerLists = zkServerLists;
        this.zkConnectTimeout = zkConnectTimeout;
        this.zkSessionTimeout = zkSessionTimeout;
        this.messager = messager;
    }

    private PathChildrenCacheListener listener = new PathChildrenCacheListener() {

        @Override
        public void childEvent(CuratorFramework client,
                PathChildrenCacheEvent event) throws Exception {

            switch (event.getType()) {
                case CHILD_ADDED:
                    postChannelChangedMessage(event, CREATE);
                    logger.info("receive service changed[create]. evnet={}", event);
                    break;
                case CHILD_REMOVED:
                    postChannelChangedMessage(event, REMOVE);
                    logger.info("receive service changed[remove]. evnet={}", event);
                    break;
                default:
                    logger.info("receive an unknow type event, ignore it. event={}", event);
            }
        }

    };

    /**
     * Ͷ�����ӱ����Ϣ
     *
     * @param event
     * @param type
     */
    private void postChannelChangedMessage(PathChildrenCacheEvent event, ChannelChangedMessage.Type type) {
        //"/excavator/nondurable/G1/1.0.0/176349878f5a1bb7df5b61741d981d35/127.0.0.1:3658";
        final String[] strs = event.getData().getPath().split("/");
        final String key = String.format("%s%s%s", strs[3]/*group*/, strs[4]/*version*/, strs[5]/*sign*/);
        final String[] addressStrs = strs[6].split(":");
        final InetSocketAddress address = new InetSocketAddress(addressStrs[0], Integer.valueOf(addressStrs[1]));
        final String provider = addressStrs[2];
        final ConsumerService service = services.get(key);
        messager.post(new ChannelChangedMessage(service, provider, address, type));
    }

    @Override
    public void init() throws Exception {
        messager.register(this, SubscribeServiceMessage.class);
        services = Maps.newConcurrentMap();
        client = newClient(
                zkServerLists,
                zkSessionTimeout,
                zkConnectTimeout,
                new ExponentialBackoffRetry(500, 20));
        client.start();
    }

    @Override
    public void destroy() throws Exception {
        for (PathChildrenCache pathChildrenCache : pathChildrenCaches) {
            pathChildrenCache.close();
        }
        if (null != client) {
            client.close();
        }
    }

    @Override
    public void receive(Message<?> msg) throws Exception {

        if (!(msg instanceof SubscribeServiceMessage)) {
            return;
        }

        final SubscribeServiceMessage ssMsg = (SubscribeServiceMessage) msg;
        final ConsumerService service = ssMsg.getContent();
        final String pref = String.format("/excavator/nondurable/%s/%s/%s",
                service.getGroup(),
                service.getVersion(),
                service.getSign());

        final PathChildrenCache pathCache = new PathChildrenCache(client, pref, false);
        pathChildrenCaches.add(pathCache);
        try {
            pathCache.getListenable().addListener(listener);
            pathCache.start();
            services.put(service.getKey(), service);
        } catch (Exception e) {
            logger.warn("subscribe {} was failed", pref, e);
        } finally {
            pathCache.close();
        }

    }

}
