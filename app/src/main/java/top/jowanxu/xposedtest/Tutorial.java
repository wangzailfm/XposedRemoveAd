package top.jowanxu.xposedtest;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

public class Tutorial implements IXposedHookLoadPackage {
    private final String WEICO_PACKAGE_NAME = "com.weico.international";
    private final String WEICO_HOOK_ACTIVITY_NAME = "com.weico.international.activity.v4.Setting";
    private final String JD_PACKAGE_NAME = "com.jingdong.app.mall";
    private final String JD_HOOK_ACTIVITY_NAME = "com.jingdong.app.mall.MainFrameActivity";

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
            // Hook获取上下文
            Class<?> contextClass = XposedHelpers.findClassIfExists("android.content.ContextWrapper", lpparam.classLoader);
            if (contextClass == null) {
                return;
            }
            // Hook
            XposedHelpers.findAndHookMethod(contextClass, "getApplicationContext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 获取上下文
                    Context context = (Context) param.getResult();
                    PackageManager packageManager = context.getPackageManager();
                    // 获取app版本
                    PackageInfo packageInfo = packageManager.getPackageInfo(lpparam.packageName, 0);
                    // 获取Activity的Class
                    Class<?> jdClass = XposedHelpers.findClassIfExists(JD_HOOK_ACTIVITY_NAME, lpparam.classLoader);
                    if (jdClass == null) {
                        return;
                    }
                    // Hook
                    XposedHelpers.findAndHookMethod(jdClass, getAdMethodNameByVersion(JD_PACKAGE_NAME, packageInfo.versionName), new XC_MethodReplacement() {
                        @Override
                        protected Object replaceHookedMethod(MethodHookParam param) throws Throwable {
                            // 替换掉该方法
                            return null;
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + JD_HOOK_ACTIVITY_NAME + " 出错" + t);
        }
    }

    /**
     * 去除weico启动广告
     * @param lpparam LoadPackageParam
     */
    private void removeWeicoAd(final XC_LoadPackage.LoadPackageParam lpparam) {
        try {
            // Hook获取上下文
            Class<?> contextClass = XposedHelpers.findClassIfExists("android.content.ContextWrapper", lpparam.classLoader);
            if (contextClass == null) {
                return;
            }
            // Hook
            XposedHelpers.findAndHookMethod(contextClass, "getApplicationContext", new XC_MethodHook() {
                @Override
                protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                    // 获取上下文
                    Context context = (Context) param.getResult();
                    PackageManager packageManager = context.getPackageManager();
                    // 获取app版本
                    PackageInfo packageInfo = packageManager.getPackageInfo(lpparam.packageName, 0);
                    // 获取Activity的Class
                    Class<?> aClass = XposedHelpers.findClassIfExists(WEICO_HOOK_ACTIVITY_NAME, lpparam.classLoader);
                    if (aClass == null) {
                        return;
                    }
                    // Hook
                    XposedHelpers.findAndHookMethod(aClass, getAdMethodNameByVersion(WEICO_PACKAGE_NAME, packageInfo.versionName), String.class, new XC_MethodHook() {

                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            String param1 = (String) param.args[0];
                            // 如果参数为display_ad的时候将返回值改为-1
                            if (!TextUtils.isEmpty(param1) && param1.equals("display_ad")) {
                                Log.e("info", "com.weico.international---loadInt---display_ad");
                                param.setResult(-1);
                            }
                        }
                    });
                }
            });
        } catch (Throwable t) {
            XposedBridge.log("Hook " + WEICO_HOOK_ACTIVITY_NAME + " 出错" + t);
        }
    }

    /**
     * 根据版本号返回对应方法名
     *
     * @param packageName 包名
     * @param versionName 版本号
     * @return 方法名
     */
    private String getAdMethodNameByVersion(String packageName, String versionName) {
        versionName = versionName.split("-")[0];
        switch (packageName) {
            case WEICO_PACKAGE_NAME:
                switch (versionName) {
                    case "2.7.9":
                    case "2.7.5":
                        return "locaInt";
                    default:
                        return "locaInt";
                }
            case JD_PACKAGE_NAME:
            default:
                switch (versionName) {
                    case "6.4.0":
                        return "fr";
                    case "6.3.0":
                        return "hN";
                    case "6.2.4":
                        return "fs";
                    case "6.2.3":
                        return "fs";
                    case "6.2.0":
                        return "fn";
                    case "6.1.3":
                        return "gC";
                    case "6.1.0":
                        return "gC";
                    case "6.0.0":
                        return "gE";
                    default:
                        return "fr";
                }
        }
    }

}
