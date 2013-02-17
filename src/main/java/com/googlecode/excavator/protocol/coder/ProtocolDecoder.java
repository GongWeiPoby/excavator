package com.googlecode.excavator.protocol.coder;

import static com.googlecode.excavator.protocol.Protocol.MAGIC;

import org.apache.log4j.Logger;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.ExceptionEvent;
import org.jboss.netty.handler.codec.frame.FrameDecoder;

import com.googlecode.excavator.constant.Log4jConstant;
import com.googlecode.excavator.protocol.Protocol;

/**
 * ͨѶЭ�������
 * @author vlinux
 *
 */
public class ProtocolDecoder extends FrameDecoder {

	private final Logger logger = Logger.getLogger(Log4jConstant.NETWORK);

	@Override
	protected Object decode(ChannelHandlerContext ctx, Channel channel,
			ChannelBuffer buffer) throws Exception {

		Protocol pro = (Protocol) ctx.getAttachment();
		// ���ȡ��Ϊnull��˵���ǵ�һ����
		if (null == pro) {
			pro = new Protocol();
			if (buffer.readableBytes() < 48/* magic(8)+type(8)+len(32) */) {
				// û�е���ͷ������Ҫ��56���ֽڣ�ֱ�ӹ���
				return null;
			} else {
				// ������ͷ������Ҫ���ֽڣ����ͷ��
				fillHeader(pro, buffer);
			}
		}// if

		// �ж�data���ֵ������Ƿ�Ҳ�Ѿ�������δ����Ҳ����
		if (buffer.readableBytes() < pro.getLength()) {
			ctx.setAttachment(pro);
			return null;
		}

		// ��������data��������
		fillData(pro, buffer);
		ctx.setAttachment(null);

		return pro;
	}

	/**
	 * ���ͷ��
	 * 
	 * @param pro
	 * @param buffer
	 */
	private void fillHeader(Protocol pro, ChannelBuffer buffer) {
		final short magic = buffer.readShort();
		final byte type = buffer.readByte();
		final int len = buffer.readInt();

		if (MAGIC != magic) {
			throw new IllegalStateException(
					String.format("magic=%d does not match, connection will disconnect!", magic));
		}

		pro.setType(type);
		pro.setLength(len);

	}

	/**
	 * ������ݶβ���
	 * 
	 * @param pro
	 * @param buffer
	 */
	private void fillData(Protocol pro, ChannelBuffer buffer) {
		byte[] datas = new byte[pro.getLength()];
		buffer.readBytes(datas);
		pro.setDatas(datas);
	}

	@Override
	public void exceptionCaught(ChannelHandlerContext ctx, ExceptionEvent e)
			throws Exception {
		logger.error("decode protocol failed!", e.getCause());
		ctx.getChannel().close();
		super.exceptionCaught(ctx, e);
	}

}
