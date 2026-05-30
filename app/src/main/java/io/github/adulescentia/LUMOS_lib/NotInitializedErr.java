package io.github.adulescentia.LUMOS_lib;

/** 초기화 전 API 호출 시 발생 */
public class NotInitializedErr extends LumosException {
    public NotInitializedErr(String message) {
        super(message);
    }
}
