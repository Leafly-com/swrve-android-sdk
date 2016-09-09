package com.swrve.sdk;

import android.content.Context;
import android.content.Intent;

import com.amazon.device.messaging.ADM;
import com.amazon.device.messaging.development.ADMManifest;
import com.swrve.sdk.config.SwrveConfig;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * Main implementation of the Amazon Swrve SDK.
 */
public class Swrve extends SwrveBase<ISwrve, SwrveConfig> implements ISwrve {
    protected static final String AMAZON_REGISTRATION_ID_CATEGORY = "AmazonRegistrationId";
    protected static final String SWRVE_AMAZON_TOKEN = "swrve.adm_token";

    protected String registrationId;
    protected ISwrvePushNotificationListener pushNotificationListener;
    protected String lastProcessedMessage;
    protected boolean admAvailable = false;

    protected Swrve(Context context, int appId, String apiKey, SwrveConfig config) {
        super(context, appId, apiKey, config);
    }

    //ADM callbacks
    @Override
    public void onRegistrationIdReceived(String registrationId) {
        //Record the registrationId.
        if (!SwrveHelper.isNullOrEmpty(registrationId)) {
            setRegistrationId(registrationId);
        }
    }

    @Override
    protected void beforeSendDeviceInfo(final Context context) {
        //TODO remove this.
        try {
            ADMManifest.checkManifestAuthoredProperly(context);
        } catch(Exception ex) {
            ex.printStackTrace();
        }

        //Check for class.
        try {
            Class.forName( "com.amazon.device.messaging.ADM" );
            admAvailable = true ;
        } catch (ClassNotFoundException e) {
            // Handle the exception.
            SwrveLogger.w(LOG_TAG, "ADM message class not found. Is manifest enable-feature for com.amazon.device.messaging set to false?", e);
        }

        //if (config.isPushEnabled()) { //TODO restore this
        if (true) {
            if (admAvailable == true) {
                try {
                    final ADM adm = new ADM(context);
                    String newRegistrationId = adm.getRegistrationId();
                    if (SwrveHelper.isNullOrEmpty(newRegistrationId)) {
                        adm.startRegister();
                    } else {
                        registrationId = newRegistrationId;
                    }
                } catch (Throwable exp) {
                    // Don't trust Amazon and all the moving parts to work as expected
                    SwrveLogger.e(LOG_TAG, "Couldn't obtain the registration key for the device.", exp);
                }

            }
        }
    }

    @Override
    public void setPushNotificationListener(ISwrvePushNotificationListener pushNotificationListener) {
        this.pushNotificationListener = pushNotificationListener;
    }

    @Override
    protected void extraDeviceInfo(JSONObject deviceInfo) throws JSONException {
        if (config.isPushEnabled() && !SwrveHelper.isNullOrEmpty(registrationId)) {
            deviceInfo.put(SWRVE_AMAZON_TOKEN, registrationId);
        }
    }

    /**
     * Gets the current registration ID for application on GCM service.
     * If result is empty, the app needs to register.
     *
     * @return registration ID, or empty string if there is no existing
     * registration ID.
     */
    protected String getRegistrationId() {
        // Try to get registration id from storage
        String registrationIdRaw = cachedLocalStorage.getSharedCacheEntry(AMAZON_REGISTRATION_ID_CATEGORY);
        if (SwrveHelper.isNullOrEmpty(registrationIdRaw)) {
            return "";
        }
        // Check if app was updated; if so, it must clear the registration ID
        // since the existing regID is not guaranteed to work with the new
        // app version.
        String registeredVersion = cachedLocalStorage.getSharedCacheEntry(APP_VERSION_CATEGORY);
        if (!SwrveHelper.isNullOrEmpty(registeredVersion) && !registeredVersion.equals(appVersion)) {
            return "";
        }
        return registrationIdRaw;
    }

    private void setRegistrationId(String regId) {
        try {
            if (registrationId == null || !registrationId.equals(regId)) {
                registrationId = regId;
                if (qaUser != null) {
                    qaUser.updateDeviceInfo();
                }

                // Store registration id and app version
                cachedLocalStorage.setAndFlushSharedEntry(AMAZON_REGISTRATION_ID_CATEGORY, registrationId);
                cachedLocalStorage.setAndFlushSharedEntry(APP_VERSION_CATEGORY, appVersion);
                // Re-send data now
                queueDeviceInfoNow(true);
            }
        } catch (Exception ex) {
            SwrveLogger.e(LOG_TAG, "Couldn't save the GCM registration id for the device", ex);
        }
    }


    public void iapPlay(String productId, double productPrice, String currency, String purchaseData, String dataSignature) {
        //TODO
        SwrveLogger.e(LOG_TAG, "iapPlay TODO");
    }

    /**
     * @deprecated Swrve engaged events are automatically sent, so this is no longer needed.
     */
    @Deprecated
    public void processIntent(Intent intent) {
        SwrveLogger.e(LOG_TAG, "The processIntent method is Deprecated and should not be used anymore");
    }
}
