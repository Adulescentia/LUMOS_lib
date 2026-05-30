package io.github.adulescentia.LUMOS_lib;

/** 잘못된 파라미터 입력 시 발생 */
public class InvalidInputErr extends LumosException {
    public InvalidInputErr(String message) {
        super(message);
    }

    public InvalidInputErr(String message, Throwable cause) {
        super(message, cause);
    }
}
