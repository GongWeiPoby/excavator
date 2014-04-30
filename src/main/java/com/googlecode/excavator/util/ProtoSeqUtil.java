package com.googlecode.excavator.util;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Э������������
 * @author vlinux
 *
 */
public class ProtoSeqUtil {

    /**
     * rmi�ĵ�������
     */
    private static final AtomicLong seq = new AtomicLong();
    
    /**
     * ���з�����
     * @return
     */
    public static long seq() {
        return seq.incrementAndGet();
    }
    
}
