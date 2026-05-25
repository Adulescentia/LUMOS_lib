package io.github.adulescentia.LUMOS_lib;

import android.media.Image;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import org.joml.Vector3f;

import java.util.ArrayList;
import java.util.Collection;
import java.util.function.Consumer;

/**more than one instance of it cannot be in the same application*/
public class Lumos {
    private static final User user = new User();
    private static final ArrayList<Device> devices = new ArrayList<>();
    private static final Detector detector = new Detector(devices, user.getUserCoordinate());

    private Consumer<Image> uiUpdateCallback;
    private Consumer<Result> resultConsumer;

    /**register device at current position
     * @return successfully registered device or null if fails*/
    @Nullable
    public Device registerDevice() {
        try {
            Vector3f base = user.getUserCoordinate();
            Vector3f pos = new Vector3f(base.x, base.y, base.z + 5.0f);
            Device device = new Device("Device-" + devices.size(), pos);
            devices.add(device);
            detector.updateAllDevices(base);
            return device;
        } catch (Exception e) {
            return null;
        }
    }

    public Collection<Device> getDeviceList() {
        return devices;
    }

    /**register UI Handler
     * @param uiUpdateCallback Call Renderer with Image that MediaPipe lib provides (landmark visualized image)*/
    public void registerUIUpdater(Consumer<Image> uiUpdateCallback) {
        this.uiUpdateCallback = uiUpdateCallback;
    }

    /**register external result consumer, cf) please use copy method to copy Result Object.*/
    public void registerExternalResultChannel(Consumer<Result> resultConsumer) {
        this.resultConsumer = resultConsumer;
    }

    /**initializes whole system, ex) checking for camera validity*/
    public void initialize() {
        detector.updateAllDevices(user.getUserCoordinate());
    }

    /**start processing image data
     * this method also initiate sending processed data into resultConsumer channel and UI handler
     */
    public void startIoTControlProcess() {
        if (resultConsumer != null) {
            resultConsumer.accept(new Result());
        }
        if (uiUpdateCallback != null) {
            uiUpdateCallback.accept(null);
        }
    }

    static Detector getDetector() {
        return detector;
    }

    static User getUser() {
        return user;
    }
}
