package top.jowanxu.xposedremovead

import android.content.Context
import android.util.Log
import de.robv.android.xposed.*
import de.robv.android.xposed.callbacks.XC_LoadPackage

/**
 * @author Jowan
 */
class Tutorial : IXposedHookLoadPackage {

    @Throws(Throwable::class)
    override fun handleLoadPackage(lpParam: XC_LoadPackage.LoadPackageParam) {
        when (lpParam.packageName) {
            // 微博国际版
            WEICO_PACKAGE_NAME -> removeWeicoAd(lpParam)
            TOP_JOWANXU_XPOSEDREMOVEAD -> checkModuleLoaded(lpParam)
        }
    }

    fun loge(tag: String, content: String) = Log.e(tag, content)

    fun tryHook(packageName: String, hook: () -> Unit) {
        try {
            hook()
        } catch (t: Throwable) {
            XposedBridge.log("$LOG_HOOK$packageName$LOG_HOOK_ERROR_STR$t")
            loge(TAG, "$LOG_HOOK$packageName$LOG_HOOK_ERROR_STR$t")
        }
    }

    /**
     * 判断模块是否加载成功
     */
    private fun checkModuleLoaded(lpParam: XC_LoadPackage.LoadPackageParam) {
        // 获取Class
        val activityClass = XposedHelpers.findClassIfExists(TOP_JOWANXU_XPOSEDREMOVEAD_ACTIVITY, lpParam.classLoader) ?: return
        tryHook(TOP_JOWANXU_XPOSEDREMOVEAD) {
            // 将方法返回值返回为true
            XposedHelpers.findAndHookMethod(activityClass, HOOK_XPOSEDREMOVEAD_METHOD_NAME, object : XC_MethodReplacement() {
                override fun replaceHookedMethod(param: MethodHookParam?): Any = true
            })
        }
    }

    /**
     * 去除weico启动广告
     * @param lpParam LoadPackageParam
     */
    private fun removeWeicoAd(lpParam: XC_LoadPackage.LoadPackageParam) {
        // Hook获取上下文
        val contextClass = XposedHelpers.findClassIfExists(ANDROID_APP_APPLICATION, lpParam.classLoader) ?: return
        tryHook(WEICO_HOOK_ACTIVITY_NAME) {
            // Hook
            XposedHelpers.findAndHookMethod(contextClass, ON_CREATE_METHOD, object : XC_MethodHook() {
                @Throws(Throwable::class)
                override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                    // 获取Activity的Class
                    val aClass = XposedHelpers.findClassIfExists(WEICO_HOOK_ACTIVITY_NAME, lpParam.classLoader) ?: return
                    // 获取上下文
                    val context = param.thisObject as Context
                    // 获取app版本
                    val packageInfo = context.packageManager.getPackageInfo(lpParam.packageName, 0)
                    tryHook(WEICO_HOOK_ACTIVITY_NAME) {
                        // Hook，将display_ad返回的值设置为-1
                        XposedHelpers.findAndHookMethod(aClass, getAdMethodNameIntByWeicoVersion(packageInfo.versionName),
                                String::class.java, object : XC_MethodHook() {

                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                                val param1 = param.args[0] as String? ?: return
                                // 如果参数为display_ad的时候将返回值改为-1
                                if (DISPLAY_AD == param1) {
                                    param.result = -1
                                }
                            }
                        })
                    }
                    tryHook(WEICO_HOOK_ACTIVITY_NAME) {
                        // 从后台返回前台也会出现广告，当时间超过30分钟，就会出现广告，所以要将ad_display_time设置为当前时间
                        XposedHelpers.findAndHookMethod(aClass, getAdMethodNameLongByWeicoVersion(packageInfo.versionName),
                                String::class.java, object : XC_MethodHook() {

                            @Throws(Throwable::class)
                            override fun afterHookedMethod(param: XC_MethodHook.MethodHookParam) {
                                val param1 = param.args[0] as String? ?: return
                                // 如果参数为display_ad_time的时候将返回值改为当前时间戳
                                if (param1 == AD_DISPLAY_TIME) {
                                    param.result = System.currentTimeMillis()
                                }
                            }
                        })
                    }
                }
            })
        }
    }

    /**
     * Weico loadInt
     * 根据版本号返回对应方法名
     * @param versionName 版本号
     * @return 方法名
     */
    private fun getAdMethodNameIntByWeicoVersion(versionName: String): String = when (versionName) {
        "2.6.2", "2.6.1", "2.5.9", "2.5.7-5", "2.5.7" -> "loadInt"
        else -> "loadInt"
    }

    /**
     * Weico loadLong
     * 根据版本号返回对应方法名
     * @param versionName 版本号
     * @return 方法名
     */
    private fun getAdMethodNameLongByWeicoVersion(versionName: String): String = when (versionName) {
        "2.6.2", "2.6.1", "2.5.9", "2.5.7-5", "2.5.7" -> "loadLong"
        else -> "loadLong"
    }

    companion object {
        private const val TOP_JOWANXU_XPOSEDREMOVEAD = "top.jowanxu.xposedremovead"
        private const val TOP_JOWANXU_XPOSEDREMOVEAD_ACTIVITY = "top.jowanxu.xposedremovead.MainActivity"
        private const val HOOK_XPOSEDREMOVEAD_METHOD_NAME = "isModuleLoaded"
        private const val LOG_HOOK = "Hook "
        private const val LOG_HOOK_ERROR_STR = " 出错"
        private const val ANDROID_APP_APPLICATION = "android.app.Application"
        private const val ON_CREATE_METHOD = "onCreate"
        private const val WEICO_PACKAGE_NAME = "com.weico.international"
        private const val WEICO_HOOK_ACTIVITY_NAME = "com.weico.international.activity.v4.Setting"
        private const val DISPLAY_AD = "display_ad"
        private const val AD_DISPLAY_TIME = "ad_display_time"
        private val TAG = Tutorial::class.java.simpleName
    }

}
