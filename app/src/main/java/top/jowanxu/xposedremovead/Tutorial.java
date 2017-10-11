package top.jowanxu.xposedremovead;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Tutorial implements IXposedHookLoadPackage {
    private static final String LOG_HOOK = "Hook ";
    private static final String LOG_HOOK_ERROR_STR = " 出错";
    private static final String ANDROID_APP_APPLICATION = "android.app.Application";
    private static final String ON_CREATE_METHOD = "onCreate";
    private static final String WEICO_PACKAGE_NAME = "com.weico.international";
    private static final String WEICO_HOOK_ACTIVITY_NAME = "com.weico.international.activity.v4.Setting";
    private static final String JD_PACKAGE_NAME = "com.jingdong.app.mall";
    private static final String JD_HOOK_CLASS_NAME = "com.jingdong.app.mall.MainFrameActivity";
    private static final String JD_HOOK_METHOD_NAME = "addGuideImage";
    private static final String JD_HOOK_CLASS_COMMON_NAME = "com.jingdong.common.BaseFrameUtil";
    private static final String JD_HOOK_FIELD_NAME = "needStartImage";

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        // 微博国际版
        if (lpparam.packageName.equals(WEICO_PACKAGE_NAME)) {
            removeWeicoAd(lpparam);
            return;
        }
        // 京东
        if (lpparam.packageName.equals(JD_PACKAGE_NAME)) {
            removeJDAd(lpparam);
        }
    }

    /**
     * 去除京东启动广告
     * @param lpparam LoadPackageParam
     */
    private void removeJDAd(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // 主界面Class
            Class<?> mainFrameClass = XposedHelpers.findClassIfExists(JD_HOOK_CLASS_NAME, lpparam.classLoader);
            if (mainFrameClass == null) {
                return;
            }
            // Hook onCreate方法，将needStartImage改为false
            XposedHelpers.findAndHookMethod(mainFrameClass, ON_CREATE_METHOD, Bundle.class, new XC_MethodHook() {
                @Override
                protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                    // 获取Class
                    Class<?> baseFrameUtilClass = XposedHelpers.findClassIfExists(JD_HOOK_CLASS_COMMON_NAME, lpparam.classLoader);
                    if (baseFrameUtilClass == null) {
                        return;
                    }
                    // 将needStartImage改为false，不显示广告
                    XposedHelpers.setStaticBooleanField(baseFrameUtilClass, JD_HOOK_FIELD_NAME, false);
                }
            });
            // Hook addGuideImage方法，不显示有视频广告
            XposedHelpers.findAndHookMethod(mainFrameClass, JD_HOOK_METHOD_NAME, new XC_MethodReplacement() {
                @Override
                protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                    // 消除addGuideImage
                    return null;
                }
            });
        } catch (Throwable t) {
            printErrorLog(JD_HOOK_CLASS_NAME, t);
        }
    }

    /**
     * 去除weico启动广告
     * @param lpparam LoadPackageParam
     */
    private void removeWeicoAd(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook获取上下文
            Class<?> contextClass = XposedHelpers.findClassIfExists(ANDROID_APP_APPLICATION, lpparam.classLoader);
            if (contextClass == null) {
                return;
            }
            // Hook
            XposedHelpers.findAndHookMethod(contextClass, ON_CREATE_METHOD, new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 获取上下文
                    Context context = (Context) param.thisObject;
                    PackageManager packageManager = context.getPackageManager();
                    // 获取app版本
                    PackageInfo packageInfo = packageManager.getPackageInfo(lpparam.packageName, 0);
                    // 获取Activity的Class
                    Class<?> aClass = XposedHelpers.findClassIfExists(WEICO_HOOK_ACTIVITY_NAME, lpparam.classLoader);
                    if (aClass == null) {
                        return;
                    }
                    // Hook，将display_ad返回的值设置为-1
                    XposedHelpers.findAndHookMethod(aClass, getAdMethodNameIntByWeicoVersion(packageInfo.versionName), String.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String param1 = (String) param.args[0];
                            // 如果参数为display_ad的时候将返回值改为-1
                            if (!TextUtils.isEmpty(param1) && param1.equals("display_ad")) {
                                param.setResult(-1);
                            }
                        }
                    });
                    // 从后台返回前台也会出现广告，当时间超过30分钟，就会出现广告，所以要将ad_display_time设置为当前时间
                    XposedHelpers.findAndHookMethod(aClass, getAdMethodNameLongByWeicoVersion(packageInfo.versionName), String.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String param1 = (String) param.args[0];
                            // 如果参数为display_ad的时候将返回值改为-1
                            if (!TextUtils.isEmpty(param1) && param1.equals("ad_display_time")) {
                                param.setResult(System.currentTimeMillis());
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            printErrorLog(WEICO_HOOK_ACTIVITY_NAME, t);
        }
    }

    /**
     * Weico loadInt
     * 根据版本号返回对应方法名
     *
     * @param versionName 版本号
     * @return 方法名
     */
    private String getAdMethodNameIntByWeicoVersion(String versionName) {
        versionName = versionName.split("-")[0];
        switch (versionName) {
            case "2.7.9":
            case "2.7.5":
                return "loadInt";
            default:
                return "loadInt";
        }
    }

    /**
     * Weico loadLong
     * 根据版本号返回对应方法名
     *
     * @param versionName 版本号
     * @return 方法名
     */
    private String getAdMethodNameLongByWeicoVersion(String versionName) {
        versionName = versionName.split("-")[0];
        switch (versionName) {
            case "2.7.9":
            case "2.7.5":
                return "loadLong";
            default:
                return "loadLong";
        }
    }

    /**
     * 打印错误
     * @param packageName 包名
     * @param t Throwable
     */
    private void printErrorLog(String packageName, Throwable t) {
        XposedBridge.log(LOG_HOOK + packageName + LOG_HOOK_ERROR_STR + t);
    }

}
