package com.demo.chat.messager.secretmedia;

/**
 * @author 林学渊
 * @email linxy59@mail2.sysu.edu.cn
 * @date 2020/8/25
 * @description null
 * @usage null
 */
public class SecureDocumentKey {

    public byte[] file_key;
    public byte[] file_iv;

    public SecureDocumentKey(byte[] key, byte[] iv) {
        file_key = key;
        file_iv = iv;
    }
}
