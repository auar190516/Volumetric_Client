package com.example.volumetric;

public class DracoDecoder {
    // 加载本地库
    static {
        System.loadLibrary("native-lib");
    }

    // 声明本地方法
    public native byte[] decodeDraco(byte[] encodedData);
}