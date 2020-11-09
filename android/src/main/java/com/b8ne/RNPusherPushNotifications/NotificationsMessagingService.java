package com.b8ne.RNPusherPushNotifications;

import android.app.Activity;
import android.util.Log;

import android.os.Handler;
import android.os.Bundle;
import android.os.Looper;
import android.content.Intent;

import java.util.Map;

import com.facebook.react.ReactApplication;
import com.facebook.react.ReactInstanceManager;
import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.ReactContext;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.bridge.WritableNativeMap;
import com.facebook.react.modules.core.DeviceEventManagerModule;

public class NotificationsMessagingService {

    private static String notificationEvent = "notification";
    private static ReactContext context;
    private static ReactInstanceManager reactInstanceManager;

    public static void read(final ReactInstanceManager reactInstanceManager, Activity reactActivity) {
        Intent intent = reactActivity.getIntent();
        final WritableMap map = new WritableNativeMap();

        boolean launchedFromHistory = intent != null ? (intent.getFlags() & Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY) != 0 : false;

        Bundle extras = intent.getExtras();
        if (!launchedFromHistory & extras != null) {
            WritableMap payload = Arguments.createMap();

            for (String key : extras.keySet()) {
                Object value = extras.get(key);
                payload.putString(key, value.toString());
            }
            Log.d("PUSHER_WRAPPER", "payload " + payload);
            map.putMap("data", payload);

            if (payload != null) {
                // We need to run this on the main thread, as the React code assumes that is true.
                // Namely, DevServerHelper constructs a Handler() without a Looper, which triggers:
                // "Can't create handler inside thread that has not called Looper.prepare()"
                Handler handler = new Handler(Looper.getMainLooper());
                handler.post(new Runnable() {
                    public void run() {
                        // Construct and load our normal React JS code data
                        // ReactInstanceManager reactInstanceManager2 = ((ReactApplication) getApplication()).getReactNativeHost().getReactInstanceManager();
                        // Log.d("PUSHER_WRAPPER", "context test" + reactInstanceManager2.getCurrentReactContext());
                        ReactContext context = reactInstanceManager.getCurrentReactContext();
                        // If it's constructed, send a notification
                        Log.d("PUSHER_WRAPPER", "context " + context);
                        if (context != null) {
                            context.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                    .emit(notificationEvent, map);
                        } else {
                            // Otherwise wait for construction, then send the notification
                            reactInstanceManager.addReactInstanceEventListener(new ReactInstanceManager.ReactInstanceEventListener() {
                                public void onReactContextInitialized(ReactContext context) {
                                    Log.d("PUSHER_WRAPPER", "after context event listener " + context);
                                    final ReactContext reactContext = context;
                                    new android.os.Handler().postDelayed(
                                        new Runnable() {
                                            public void run() {
                                                Log.i("PUSHER_WRAPPER", "This'll run 2 sec later");
                                                reactContext.getJSModule(DeviceEventManagerModule.RCTDeviceEventEmitter.class)
                                                        .emit(notificationEvent, map);
                                            }
                                        }, 
                                    2000);
                                }
                            });
                            if (!reactInstanceManager.hasStartedCreatingInitialContext()) {
                                Log.d("PUSHER_WRAPPER", "create context in background");
                                // Construct it in the background
                                reactInstanceManager.createReactContextInBackground();
                            }
                        }
                    }
                });
            }
        }
    }
}
