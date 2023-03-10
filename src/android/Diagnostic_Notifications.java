/*
       Licensed to the Apache Software Foundation (ASF) under one
       or more contributor license agreements.  See the NOTICE file
       distributed with this work for additional information
       regarding copyright ownership.  The ASF licenses this file
       to you under the Apache License, Version 2.0 (the
       "License"); you may not use this file except in compliance
       with the License.  You may obtain a copy of the License at

         http://www.apache.org/licenses/LICENSE-2.0

       Unless required by applicable law or agreed to in writing,
       software distributed under the License is distributed on an
       "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
       KIND, either express or implied.  See the License for the
       specific language governing permissions and limitations
       under the License.
*/
package cordova.plugins;

/*
 * Imports
 */

import android.annotation.SuppressLint;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaInterface;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CordovaWebView;
import org.json.JSONArray;
import org.json.JSONException;

import androidx.core.app.NotificationManagerCompat;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.appcompat.app.AlertDialog;

/**
 * Diagnostic plugin implementation for Android
 */
public class Diagnostic_Notifications extends CordovaPlugin {


  /*************
   * Constants *
   *************/


  /**
   * Tag for debug log messages
   */
  public static final String TAG = "Diagnostic_Notifications";


  /*************
   * Variables *
   *************/

  /**
   * Singleton class instance
   */
  public static Diagnostic_Notifications instance = null;

  private Diagnostic diagnostic;

  /**
   * Current Cordova callback context (on this thread)
   */
  protected CallbackContext currentContext;


  /*************
   * Public API
   ************/

  /**
   * Constructor.
   */
  public Diagnostic_Notifications() {
  }

  /**
   * Sets the context of the Command. This can then be used to do things like
   * get file paths associated with the Activity.
   *
   * @param cordova The context of the main Activity.
   * @param webView The CordovaWebView Cordova is running in.
   */
  @SuppressLint("LongLogTag")
  public void initialize(CordovaInterface cordova, CordovaWebView webView) {
    Log.d(TAG, "initialize()");
    instance = this;
    diagnostic = Diagnostic.getInstance();

    super.initialize(cordova, webView);
  }


  /**
   * Executes the request and returns PluginResult.
   *
   * @param action          The action to execute.
   * @param args            JSONArry of arguments for the plugin.
   * @param callbackContext The callback id used when calling back into JavaScript.
   * @return True if the action was valid, false if not.
   */
  public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
    Diagnostic.instance.currentContext = currentContext = callbackContext;

    try {
      if (action.equals("isRemoteNotificationsEnabled")) {
        callbackContext.success(isRemoteNotificationsEnabled() ? 1 : 0);
      } else if (action.equals("switchToNotificationSettings")) {
        switchToNotificationSettings();
        callbackContext.success();
      } else if (action.equals("requestIgnoreBatteryOptimizations")) {
        requestIgnoreBatteryOptimizations();
        callbackContext.success();
      } else if (action.equals("switchToActiveInBackgroundSetting")) {
        switchToActiveInBackgroundSetting();
        callbackContext.success();
      } else if (action.equals("getDownloadChannels")) {
        getDownloadChannels(args);
      } else {
        diagnostic.handleError("Invalid action");
        return false;
      }
    } catch (Exception e) {
      diagnostic.handleError("Exception occurred: ".concat(e.getMessage()));
      return false;
    }
    return true;
  }


  public boolean isRemoteNotificationsEnabled() {
    NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(this.cordova.getActivity().getApplicationContext());
    boolean result = notificationManagerCompat.areNotificationsEnabled();
    return result;
  }

  public void switchToNotificationSettings() {
    Context context = this.cordova.getActivity().getApplicationContext();
    Intent settingsIntent = new Intent();
    String packageName = context.getPackageName();
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
      diagnostic.logDebug("Switch to notification Settings");
      settingsIntent.setAction(Settings.ACTION_APP_NOTIFICATION_SETTINGS);
      settingsIntent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName);
    } else {
      settingsIntent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
      settingsIntent.setData(Uri.parse("package:" + packageName));
      diagnostic.logDebug("Switch to notification Settings: Only possible on android O or above. Falling back to application details");
    }
    cordova.getActivity().startActivity(settingsIntent);
  }

  // 检查电池白名单
  private boolean isIgnoringBatteryOptimizations() {
    boolean isIgnoring = false;

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
      PowerManager powerManager = (PowerManager) cordova.getActivity().getSystemService(Context.POWER_SERVICE);
      if (powerManager != null) {
        isIgnoring = powerManager.isIgnoringBatteryOptimizations(cordova.getActivity().getPackageName());
      }
    }
    return isIgnoring;
  }

  // 加入电池白名单
  public void requestIgnoreBatteryOptimizations() {
    try {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        if (!isIgnoringBatteryOptimizations()) {
          Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
          intent.setData(Uri.parse("package:" + cordova.getActivity().getPackageName()));
          cordova.getActivity().startActivity(intent);
        }
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  // 跳转自启动设置
  public void switchToActiveInBackgroundSetting() {
    new AlertDialog.Builder(cordova.getContext())
      .setTitle("提示信息")
      .setMessage("为了及时接收通知，请设置app允许在后台活动")
      .setNegativeButton("取消", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
        }
      })
      .setPositiveButton("确定", new DialogInterface.OnClickListener() {
        @Override
        public void onClick(DialogInterface dialog, int which) {
          if (isHuawei()) {
            goHuaweiSetting();
          } else if (isXiaomi()) {
            goXiaomiSetting();
          } else if (isOPPO()) {
            goOPPOSetting();
          } else if (isVIVO()) {
            goVIVOSetting();
          } else if (isMeizu()) {
            goMeizuSetting();
          } else if (isSamsung()) {
            goSamsungSetting();
          } else if (isLeTV()) {
            goLetvSetting();
          } else if (isSmartisan()) {
            goSamsungSetting();
          }
        }
      }).show();
  }

  // 厂商判断：华为
  private boolean isHuawei() {
    if (Build.BRAND == null) {
      return false;
    } else {
      return Build.BRAND.toLowerCase().equals("huawei") || Build.BRAND.toLowerCase().equals("honor");
    }
  }

  // 厂商判断：小米
  private boolean isXiaomi() {
    return Build.BRAND != null
      && (Build.BRAND.toLowerCase().equals("xiaomi") || Build.BRAND.toLowerCase().equals("redmi"));
  }

  // 厂商判断：OPPO
  private boolean isOPPO() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("oppo");
  }

  // 厂商判断：OPPO
  private boolean isVIVO() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("vivo");
  }

  // 厂商判断：魅族
  private boolean isMeizu() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("meizu");
  }

  // 厂商判断：三星
  private boolean isSamsung() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("samsung");
  }

  // 厂商判断：乐视
  private boolean isLeTV() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("letv");
  }

  // 厂商判断：锤子
  private boolean isSmartisan() {
    return Build.BRAND != null && Build.BRAND.toLowerCase().equals("smartisan");
  }

  // 跳转华为手机管家的启动管理页
  private void goHuaweiSetting() {
    try {
      showActivity("com.huawei.systemmanager",
        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity");
    } catch (Exception e) {
      showActivity("com.huawei.systemmanager",
        "com.huawei.systemmanager.optimize.bootstart.BootStartActivity");
    }
  }

  // 跳转小米安全中心的自启动管理页面
  private void goXiaomiSetting() {
    showActivity("com.miui.securitycenter",
      "com.miui.permcenter.autostart.AutoStartManagementActivity");
  }

  // 跳转 OPPO 手机管家
  private void goOPPOSetting() {
    try {
      showActivity("com.coloros.phonemanager");
    } catch (Exception e1) {
      try {
        showActivity("com.oppo.safe");
      } catch (Exception e2) {
        try {
          showActivity("com.coloros.oppoguardelf");
        } catch (Exception e3) {
          showActivity("com.coloros.safecenter");
        }
      }
    }
  }

  // 跳转 VIVO 手机管家
  private void goVIVOSetting() {
    showActivity("com.iqoo.secure");
  }

  // 跳转魅族手机管家
  private void goMeizuSetting() {
    showActivity("com.meizu.safe");
  }

  // 跳转三星智能管理器
  private void goSamsungSetting() {
    try {
      showActivity("com.samsung.android.sm_cn");
    } catch (Exception e) {
      showActivity("com.samsung.android.sm");
    }
  }

  // 跳转乐视手机管家
  private void goLetvSetting() {
    showActivity("com.letv.android.letvsafe",
      "com.letv.android.letvsafe.AutobootManageActivity");
  }

  // 跳转手机管理
  private void goSmartisanSetting() {
    showActivity("com.smartisanos.security");
  }

  /**
   * 跳转到指定应用的首页
   */
  private void showActivity(@NonNull String packageName) {
    Intent intent = cordova.getActivity().getPackageManager().getLaunchIntentForPackage(packageName);
    cordova.getActivity().startActivity(intent);
  }

  /**
   * 跳转到指定应用的指定页面
   */
  private void showActivity(@NonNull String packageName, @NonNull String activityDir) {
    Intent intent = new Intent();
    intent.setComponent(new ComponentName(packageName, activityDir));
    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    cordova.getActivity().startActivity(intent);
  }

  /**
   * 获取App下载渠道
   */
  public void getDownloadChannels(JSONArray args) throws Exception{
    String key = args.getString(0);
    PackageManager pm = cordova.getContext().getPackageManager();
    ApplicationInfo appInfo = pm.getApplicationInfo(cordova.getContext().getPackageName(), PackageManager.GET_META_DATA);
    currentContext.success(appInfo.metaData.getString(key));
  }

  /************
   * Internals
   ***********/


}
