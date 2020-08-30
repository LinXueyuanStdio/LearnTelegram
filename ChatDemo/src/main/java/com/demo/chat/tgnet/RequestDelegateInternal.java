package com.demo.chat.tgnet;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/30
 * @description null
 * @usage null
 */
public interface RequestDelegateInternal {
    void run(long response, int errorCode, String errorText, int networkType);
}

