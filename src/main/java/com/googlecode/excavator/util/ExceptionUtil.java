package com.googlecode.excavator.util;

import java.io.IOException;

/**
 * �쳣������
 *
 * @author vlinux
 *
 */
public final class ExceptionUtil {

    /**
     * ����׳����쳣���Ƿ����������ͨѶ�쳣
     *
     * @param t
     * @return
     */
    public static boolean hasNetworkException(Throwable t) {
        Throwable cause = t;
        while (cause != null) {
            if (cause instanceof IOException) {
                return true;
            }
            cause = cause.getCause();
        }
        return false;
    }

}
