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
    static User user = new User();
    static ArrayList<Device> devices = new ArrayList<>();
    static Detector detector = new Detector(devices,user.getUserCoordinate());
    /**register device at current position
    * @return successfully registered device or null if fails*/
    @Nullable Device registerDevice(String name, Vector3f pos) {
        try {
            Device newDevice = new Device(name, pos);
            devices.add(newDevice);
            return newDevice;
        } catch (Exception e) {
            return null;
        }
    }

    Collection<Device> getDeviceList() {
        return devices;
    }

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

    /**get user's current normalized direction*/
    abstract @NonNull Vector3f getDirection(); //todo armVector 구하기

    /**get user's currently selected Device*/
    @NonNull Device getSelectedDevice() {
        return Lumos.detector.getDevice(new Vector3f()/*todo 여기에 getDirection() (armVector) 호출*/);
    }

    /**get user's current position ( it can be relative pos at fixed point but it should be consistent enough )*/
    @NonNull Vector3f getCurrentPosition() {
        return Lumos.user.getUserCoordinate();
    }

    @NonNull
    abstract public Result clone();
}