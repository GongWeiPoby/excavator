package com.googlecode.excavator.consumer.support;

import static com.googlecode.excavator.consumer.message.ChannelChangedMessage.Type.CREATE;
import static com.googlecode.excavator.consumer.message.ChannelChangedMessage.Type.REMOVE;

import java.net.InetSocketAddress;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Set;
import java.util.concurrent.Executors;

import org.apache.commons.lang.StringUtils;
import org.jboss.netty.bootstrap.ClientBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.group.ChannelGroup;
import org.jboss.netty.channel.group.DefaultChannelGroup;
import org.jboss.netty.channel.socket.nio.NioClientSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.googlecode.excavator.Ring;
import com.googlecode.excavator.Supporter;
import com.googlecode.excavator.constant.LogConstant;
import com.googlecode.excavator.consumer.ChannelRing;
import com.googlecode.excavator.consumer.Receiver;
import com.googlecode.excavator.consumer.message.ChannelChangedMessage;
import com.googlecode.excavator.message.Message;
import com.googlecode.excavator.message.MessageSubscriber;
import com.googlecode.excavator.message.Messager;
import com.googlecode.excavator.protocol.RmiRequest;
import com.googlecode.excavator.protocol.RmiResponse;
import com.googlecode.excavator.protocol.coder.ProtocolDecoder;
import com.googlecode.excavator.protocol.coder.ProtocolEncoder;
import com.googlecode.excavator.protocol.coder.RmiDecoder;
import com.googlecode.excavator.protocol.coder.RmiEncoder;

/**
 * ���ӻ�֧����
 *
 * @author vlinux
 *
 */
public class ChannelRingSupport implements Supporter, ChannelRing, MessageSubscriber {

    private final Logger logger = LoggerFactory.getLogger(LogConstant.NETWORK);

    private final int connectTimeout;
    private final Receiver receiver;
    private final Messager messager;

    private Map<String/*group+version+sign*/, Ring<ChannelRing.Wrapper>> serviceChannelRings;
    private ClientBootstrap bootstrap;
    private ChannelGroup channelGroup;
    
    /**
     * ���캯��
     * @param connectTimeout
     * @param receiver
     * @param messager
     */
    public ChannelRingSupport(int connectTimeout, Receiver receiver, Messager messager) {
        this.connectTimeout = connectTimeout;
        this.receiver = receiver;
        this.messager = messager;
    }

    /*
     * ҵ������
     */
    private SimpleChannelUpstreamHandler businessHandler = new SimpleChannelUpstreamHandler() {

        @Override
        public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
                throws Exception {

            if (null == e.getMessage()
                    || !(e.getMessage() instanceof RmiResponse)) {
                super.messageReceived(ctx, e);
            }

            final RmiResponse resp = (RmiResponse) e.getMessage();
            final Receiver.Wrapper wrapper = receiver.receive(resp.getId());
            if (null == wrapper) {
                // ����յ���response����wrappers�У�˵���Ѿ���ʱ
                logger.info("received response, but request was not found, looks like timeout. resp:{}",resp);
            } else {
                wrapper.setResponse(resp);
                wrapper.signalWaitResp();
            }
        }

    };

    private ChannelPipelineFactory channelPipelineFactory = new ChannelPipelineFactory() {

        @Override
        public ChannelPipeline getPipeline() throws Exception {
            ChannelPipeline pipeline = Channels.pipeline();
            pipeline.addLast("protocol-decoder", new ProtocolDecoder());
            pipeline.addLast("rmi-decoder", new RmiDecoder());
            pipeline.addLast("businessHandler", businessHandler);
            pipeline.addLast("protocol-encoder", new ProtocolEncoder());
            pipeline.addLast("rmi-encoder", new RmiEncoder());
            return pipeline;
        }

    };

    @Override
    public void init() throws Exception {

        messager.register(this, ChannelChangedMessage.class);

        serviceChannelRings = Maps.newConcurrentMap();
        channelGroup = new DefaultChannelGroup();
        bootstrap = new ClientBootstrap(
                new NioClientSocketChannelFactory(
                        Executors.newCachedThreadPool(),
                        Executors.newCachedThreadPool())
        );

        bootstrap.setPipelineFactory(channelPipelineFactory);
        bootstrap.setOption("tcpNoDelay", true);
        bootstrap.setOption("keepAlive", true);
        bootstrap.setOption("connectTimeoutMillis", connectTimeout);

    }

    @Override
    public void destroy() throws Exception {

        if (null != channelGroup) {
            channelGroup.close();
        }
        if (null != bootstrap) {
            bootstrap.releaseExternalResources();
        }

    }

    @Override
    public void receive(Message<?> msg) throws Exception {

        if (!(msg instanceof ChannelChangedMessage)) {
            return;
        }

        final ChannelChangedMessage ccMsg = (ChannelChangedMessage) msg;
        switch (ccMsg.getType()) {
            case CREATE:
                handleChannelCreate(ccMsg);
                break;
            case REMOVE:
                handleChannelRemove(ccMsg);
                break;
        }

    }

    private Set<Wrapper> channelWrappers = Sets.newLinkedHashSet();

    /**
     * ��ȡһ��ChannelWrapper�����û�ҵ��򴴽�
     *
     * @param ccMsg
     * @return
     */
    private Wrapper getWrapper(ChannelChangedMessage ccMsg) {
        Iterator<Wrapper> it = channelWrappers.iterator();
        while (it.hasNext()) {
            Wrapper wrapper = it.next();
            if (compareAddress((InetSocketAddress) wrapper.getChannel().getRemoteAddress(), ccMsg.getAddress())) {
                wrapper.inc();
                return wrapper;
            }
        }

        // ���û�ҵ��򴴽�һ��
        Wrapper wrapper = createWrapper(ccMsg);
        return wrapper;

    }

    /**
     * �������Ӱ�װ����
     *
     * @param ccMsg
     * @return
     */
    private Wrapper createWrapper(ChannelChangedMessage ccMsg) {

        final Channel channel = createChannel(ccMsg.getAddress());
        if (null != channel) {
            // �����ɹ��򷵻�
            Wrapper wrapper = new Wrapper(channel, ccMsg.getProvider());
            wrapper.inc();
            channelWrappers.add(wrapper);
            return wrapper;
        } else {
            // ������ɹ������ظ�����
            messager.post(ccMsg);
        }

        return null;
    }

    /**
     * �������������¼�
     *
     * @param ccEvt
     */
    private synchronized void handleChannelCreate(ChannelChangedMessage ccMsg) {

        if (ccMsg.getType() != CREATE) {
            return;
        }

        final Wrapper wrapper = getWrapper(ccMsg);
        if (null == wrapper) {
            // ��������Ҳ��������κ��ԣ����´�����
            logger.warn("create channel({}) failed, ingore this time.", ccMsg.getAddress());
            return;
        }

        final String key = ccMsg.getContent().getKey();
        final Ring<Wrapper> ring;
        if (serviceChannelRings.containsKey(key)) {
            ring = serviceChannelRings.get(key);
        } else {
            serviceChannelRings.put(key, ring = new Ring<Wrapper>());
        }

        ring.insert(wrapper);

    }

    /**
     * ����ɾ�������¼�
     *
     * @param ccEvt
     */
    private synchronized void handleChannelRemove(ChannelChangedMessage ccMsg) {

        if (ccMsg.getType() != REMOVE) {
            return;
        }

        final Ring<Wrapper> ring = serviceChannelRings.get(ccMsg.getContent().getKey());
        if (null == ring) {
            return;
        }
        final Iterator<Wrapper> it = ring.iterator();
        while (it.hasNext()) {
            final Wrapper wrapper = it.next();
            final Channel channel = wrapper.getChannel();
            if (compareAddress((InetSocketAddress) channel.getRemoteAddress(), ccMsg.getAddress())) {
                it.remove();
                channelWrappers.remove(wrapper);
                receiver.unRegister(channel);
                wrapper.dec();
            }
        }

    }

    /**
     * �Ƚ����������ַ�Ƿ����
     *
     * @param a
     * @param b
     * @return
     */
    private boolean compareAddress(InetSocketAddress a, InetSocketAddress b) {
        if (null != a && null != b) {
            return StringUtils.equals(a.getHostName(), b.getHostName())
                    && a.getPort() == b.getPort();
        }
        return false;
    }

    /**
     * ����netty��channel
     *
     * @param address
     * @return ���ʧ���򷵻�null
     */
    private Channel createChannel(InetSocketAddress address) {
        final ChannelFuture future = bootstrap.connect(address);
        future.awaitUninterruptibly();
        if (future.isCancelled()) {
            logger.warn("connect is cancelled. address:{}", address);
            return null;
        }
        if (!future.isSuccess()) {
            logger.warn("connect to {} failed.", address, future.getCause());
            return null;
        }
        logger.info("connect to {} successed.", address);
        final Channel channel = future.getChannel();
        channelGroup.add(channel);
        return channel;
    }

    @Override
    public ChannelRing.Wrapper ring(RmiRequest req) {

        final String key = req.getKey();

        // key ������
        if (!serviceChannelRings.containsKey(key)) {
            logger.info("provider not found. key not found. req:{}", req);
            return null;
        }

        // ring Ϊ��
        final Ring<ChannelRing.Wrapper> ring = serviceChannelRings.get(key);
        if (ring.isEmpty()) {
            logger.info("provider not found. ring is empty. req:{}", req);
            return null;
        }

        // ����ȡchannel
        ChannelRing.Wrapper wrapper;
        try {
            wrapper = ring.ring();
        } catch (NoSuchElementException e) {
            logger.info("provider not found. no such elements. req:{}", req);
            return null;
        }

        // ���mabbedown
        if (wrapper.isMaybeDown()) {
            // ������һ��another��Ŭ����ȡ����һ�����õ�����
            Wrapper another = null;
            final Iterator<Wrapper> it = ring.iterator();
            while (it.hasNext()) {
                final Wrapper w = it.next();
                if (!w.isMaybeDown()) {
                    another = w;
                }
                if (w == wrapper) {
                    it.remove();
                    wrapper.getChannel().disconnect();
                    wrapper.getChannel().close();
                    logger.info("{} maybe down. close this channel.", wrapper.getChannel().getRemoteAddress());
//					//ͬʱͶ��һ����Ϣ��֪��Ҫ��������
//					final ConsumerService service = new ConsumerService(req.getGroup(),req.getVersion(),req.getSign(),req.getTimeout());
//					final ChannelChangedMessage ccMsg = new ChannelChangedMessage(
//							service, (InetSocketAddress)wrapper.getChannel().getRemoteAddress(), CREATE);
//					Messages.post(ccMsg);
                }
            }
            wrapper = another;
        }//if

        return wrapper;

    }

}
