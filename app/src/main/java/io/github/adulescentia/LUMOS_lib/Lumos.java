package io.github.adulescentia.LUMOS_lib;

import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;
/**more than one instance of it cannot be in the same application*/
abstract class Lumos {

    /**register device at current position
    * @return successfully registered device or null if fails*/
    abstract @Nullable Device registerDevice();

    abstract Collection<Device> getDeviceList();
    /**register UI Handler
     * @param uiUpdateCallback Call Renderer with Image that MediaPipe lib provides (landmark visualized image)*/
    abstract void registerUIUpdater(Consumer<Image> uiUpdateCallback);
    /**register external result consumer, cf) please use copy method to copy Result Object.*/
    abstract void registerExternalResultChannel(Consumer<Result> resultConsumer);
    /**initializes whole system, ex) checking for camera validity*/
    abstract void initialize();

    /**start processing image data
     * this method also initiate sending processed data into resultConsumer channel and UI handler
     */
    abstract void startIoTControlProcess();


}

abstract class Result implements Cloneable {
    /**get user's current normalized direction
     * @return Vector*/
    abstract @NonNull Vector3f getDirection();
    /**get user's currently selected Device
     * @return Device*/
    abstract @NonNull Device getSelectedDevice();

    /**get user's current position ( it can be relative pos at fixed point but it should be consistent enough )*/
    abstract @NonNull Vector3f getCurrentPosition();

    /**get camera's position*/
    abstract @NonNull Vector3f getCameraPos();

    @NonNull
    public abstract Result clone();
}