package io.github.adulescentia.LUMOS_lib;

/** LUMOS 라이브러리 공통 예외 */
public class LumosException extends RuntimeException {
    public LumosException(String message) {
        super(message);
    }

    public LumosException(String message, Throwable cause) {
        super(message, cause);
    }
}
