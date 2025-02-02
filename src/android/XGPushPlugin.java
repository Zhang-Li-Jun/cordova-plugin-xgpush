package net.sunlu.xgpush;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.apache.cordova.LOG;
import org.json.JSONArray;
import org.json.JSONException;

import com.tencent.android.tpush.XGIOperateCallback;
import com.tencent.android.tpush.XGLocalMessage;
import com.tencent.android.tpush.XGPushClickedResult;
import com.tencent.android.tpush.XGPushConfig;
import com.tencent.android.tpush.XGPushConstants;
import com.tencent.android.tpush.common.Constants;

import com.tencent.android.tpush.XGPushManager;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.text.TextUtils;
import android.util.Log;

public class XGPushPlugin extends CordovaPlugin {

    private Context context;
    private XGPushReceiver receiver;
    private static final String TAG = "XGPushPlugin";

    @Override
    public void initialize(CordovaInterface cordova, CordovaWebView webView) {
        super.initialize(cordova, webView);
        context = cordova.getActivity().getApplicationContext();
        XGPushConfig.enableOtherPush(context, true);
        String xmId = "";
        String xmKey = "";
        String mzId = "";
        String mzKey = "";
        try {
            ApplicationInfo applicationInfo = cordova.getActivity().getPackageManager()
                    .getApplicationInfo(cordova.getActivity().getPackageName(), PackageManager.GET_META_DATA);
            Bundle bundle = applicationInfo.metaData;
            xmId = bundle.getString("XM_APPID");
            xmKey = bundle.getString("XM_APPKEY");
            mzId = bundle.getString("MZ_APPID");
            mzKey = bundle.getString("MZ_APPKEY");
            Log.d("Vitta", xmId);
            Log.d("Vitta", xmKey);
        } catch (PackageManager.NameNotFoundException e) {
            Log.e(TAG, "Failed to load meta-data, NameNotFound: " + e.getMessage());
        } catch (NullPointerException e) {
            Log.e(TAG, "Failed to load meta-data, NullPointer: " + e.getMessage());
        }
        XGPushConfig.enableFcmPush(context, true);
        XGPushConfig.setHuaweiDebug(true);
        XGPushConfig.setMiPushAppId(context, xmId);
        XGPushConfig.setMiPushAppKey(context, xmKey);
        XGPushConfig.setMzPushAppId(context, mzId);
        XGPushConfig.setMzPushAppKey(context, mzKey);
        XGPushConfig.enableOtherPush(context, true);
    }

    @Override
    public boolean execute(String action, JSONArray data, CallbackContext callbackContext) throws JSONException {

        LOG.d(TAG, "exec : action = " + action + ", callbackId = "
                + (callbackContext != null ? callbackContext.getCallbackId() : "null"));

        if ("addListener".equals(action)) {
            addListener(callbackContext);
            return true;
        }
        if ("registerPush".equals(action)) {
            registerPush(data, callbackContext);
            return true;
        }
        if ("bindAccount".equals(action)) {
            bindAccount(data, callbackContext);
            return true;
        }
        if ("delAccount".equals(action)) {
            delAccount(data, callbackContext);
            return true;
        }
        if ("unRegisterPush".equals(action)) {
            unRegisterPush(callbackContext);
            return true;
        }
        if ("setTag".equals(action)) {
            setTag(data, callbackContext);
            return true;
        }
        if ("deleteTag".equals(action)) {
            deleteTag(data, callbackContext);
            return true;
        }
        if ("addLocalNotification".equals(action)) {
            addLocalNotification(data, callbackContext);
            return true;
        }
        if ("enableDebug".equals(action)) {
            enableDebug(data, callbackContext);
            return true;
        }
        if ("getToken".equals(action)) {
            getToken(callbackContext);
            return true;
        }
        if ("setAccessInfo".equals(action)) {
            setAccessInfo(data, callbackContext);
            return true;
        }
        if ("getLaunchInfo".equals(action)) {
            getLaunchInfo(callbackContext);
            return true;
        }

        Log.d(TAG, "> exec not action : action=" + action);

        return false;
    }

    private void addListener(CallbackContext callbackContext) {
        Log.d(TAG, "addListener : callbackId=" + callbackContext.getCallbackId());

        XGPushReceiver receiver = new XGPushReceiver(callbackContext);
        IntentFilter filter = new IntentFilter();
        filter.addAction(Constants.ACTION_PUSH_MESSAGE);
        filter.addAction(Constants.ACTION_FEEDBACK);
        context.registerReceiver(receiver, filter);
    }

    @Override
    public void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        this.cordova.getActivity().setIntent(intent);
    }

    @Override
    public void onResume(boolean multitasking) {

        super.onResume(multitasking);

        // if(receiver != null){
        // receiver.onResume(context, cordova);
        // }
    }

    @Override
    public void onPause(boolean multitasking) {
        super.onPause(multitasking);
        // Log.d(TAG, "onActivityStoped : multitasking=" + multitasking);
        XGPushManager.onActivityStoped(this.cordova.getActivity());
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (receiver != null) {
            context.unregisterReceiver(receiver);
        }
    }

    ///////////////// ------API---------////////////////////

    private void registerPush(final JSONArray data, final CallbackContext callback) {

        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String account = (data != null && data.length() > 0) ? data.getString(0) : null;
                    XGIOperateCallback reply = new XGPushCallback(callback);

                    if (TextUtils.isEmpty(account)) {
                        Log.d(TAG, "> register public");
                        XGPushManager.registerPush(context, reply);
                    } else {
                        Log.d(TAG, "> 腾讯移动推送删除了带账号的注册API");
                        XGPushManager.registerPush(context, reply);
                        // Log.d(TAG, "> register private:" + account);
                        // XGPushManager.registerPush(context, account, reply);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "register error:" + e.toString());
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private void bindAccount(final JSONArray data, final CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String account = (data != null && data.length() > 0) ? data.getString(0) : null;
                    XGIOperateCallback reply = new XGPushCallback(callback);

                    if (TextUtils.isEmpty(account)) {
                        Log.d(TAG, "> account is null");
                        callback.error("account is null");
                    } else {
                        Log.d(TAG, "> register private:" + account);
                        XGPushManager.bindAccount(context, account, reply);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "register error:" + e.toString());
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private void delAccount(final JSONArray data, final CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    String account = (data != null && data.length() > 0) ? data.getString(0) : null;
                    XGIOperateCallback reply = new XGPushCallback(callback);

                    if (TextUtils.isEmpty(account)) {
                        Log.d(TAG, "> account is null");
                        callback.error("account is null");
                    } else {
                        Log.d(TAG, "> register private:" + account);
                        XGPushManager.delAccount(context, account, reply);
                    }
                } catch (Exception e) {
                    Log.e(TAG, "register error:" + e.toString());
                    callback.error(e.getMessage());
                }
            }
        });
    }

    private void unRegisterPush(final CallbackContext callback) {
        cordova.getThreadPool().execute(new Runnable() {
            @Override
            public void run() {
                XGPushManager.unregisterPush(context);
            }
        });
    }

    private void setTag(JSONArray data, CallbackContext callback) {
        try {
            XGPushManager.setTag(context, data.getString(0));
            callback.success();
        } catch (Exception e) {
            Log.e(TAG, "setTag error:" + e.toString());
            callback.error(e.toString());
        }
    }

    private void deleteTag(JSONArray data, CallbackContext callback) {
        try {
            XGPushManager.deleteTag(context, data.getString(0));
            callback.success();
        } catch (Exception e) {
            Log.e(TAG, "deleteTag error:" + e.toString());
            callback.error(e.toString());
        }
    }

    private void addLocalNotification(JSONArray data, CallbackContext callback) {
        try {
            XGLocalMessage message = new XGLocalMessage();

            message.setType(data.getInt(0));
            message.setTitle(data.getString(1));
            message.setContent(data.getString(2));

            Long id = XGPushManager.addLocalNotification(context, message);

            callback.success(id.toString());
        } catch (Exception e) {
            Log.e(TAG, "addLocalNotification error:" + e.toString());
            callback.error(e.toString());
        }
    }

    private void enableDebug(JSONArray data, CallbackContext callback) {
        try {
            boolean debugMode = data.getBoolean(0);
            XGPushConfig.enableDebug(context, debugMode);
            callback.success();
        } catch (Exception e) {
            Log.e(TAG, "enableDebug error:" + e.toString());
            callback.error(e.toString());
        }
    }

    private void getToken(CallbackContext callback) {
        String token = XGPushConfig.getToken(context);
        callback.success(token);
    }

    private void setAccessInfo(JSONArray data, CallbackContext callback) {
        try {
            long id = data.getLong(0);
            String key = data.getString(1);
            XGPushConfig.setAccessId(context, id);
            XGPushConfig.setAccessKey(context, key);
            callback.success();
        } catch (Exception e) {
            Log.e(TAG, "setAccessInfo error:" + e.toString());
            callback.error(e.toString());
        }
    }

    private void getLaunchInfo(CallbackContext callback) {
        XGPushClickedResult click = XGPushManager.onActivityStarted(cordova.getActivity());
        callback.success(XGPushReceiver.convertClickedResult(click));
    }
}
