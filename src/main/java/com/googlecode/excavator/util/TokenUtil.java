package com.googlecode.excavator.util;

import static com.googlecode.excavator.PropertyConfiger.isEnableToken;

import java.util.UUID;

import org.apache.commons.lang.StringUtils;

/**
 * Token���ɹ���
 * @author vlinux
 *
 */
public class TokenUtil {

    /**
     * ����token
     * @return
     */
    public static String genToken() {
        return isEnableToken()
                ? StringUtils.EMPTY
                : UUID.randomUUID().toString();
    }
    
}
