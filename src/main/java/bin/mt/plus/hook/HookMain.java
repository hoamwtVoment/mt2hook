package bin.mt.plus.hook;

import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

/**
 * MT管理器(bin.mt.plus) VIP 本地判定 Patch
 * -----------------------------------------
 * 逆向结论判定点：
 *   类  Ll/۟᩻ۨ   (cp \u06DF\u1A7B\u06E8)
 *   方法 public static native ۡ()Z   (cp \u06E1)
 *   该布尔被全应用当作功能/VIP门控使用（Menu可见性、wrapper透传、初始化配置）。
 *
 * 修复记录(v2) —— 解决"点击功能后登录状态消失/无法登录"副作用：
 *   旧版在 beforeHookedMethod 里直接 setResult(true)，彻底短路 native 方法体。
 *   若该 native 内部除返回判定值外还有状态副作用(登录/会员缓存建立)，短路会破坏该状态，
 *   导致上层虽拿到 true 但内部状态不一致，触发"点了就掉登录"的异常。
 *   现改为【先让 native 真正执行，再在 after 阶段把返回布尔覆盖为 true】。
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "MT2Hook";
    private static final String PKG = "bin.mt.plus";

    private static final String SHORT = "\u06DF\u1A7B\u06E8";   // ۟᩻ۨ
    private static final String METHOD = "\u06E1";              // ۡ (static native, 无参 -> Z)

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals(PKG)) return;
        XposedBridge.log(TAG + " module loaded on " + PKG);
        try {
            final String clazz = "l." + SHORT;                   // l.۟᩻ۨ
            XposedHelpers.findAndHookMethod(clazz, lp.classLoader, METHOD,
                    new XC_MethodHook() {
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            param.setResult(Boolean.TRUE);
                        }
                    });
            XposedBridge.log(TAG + " hooked " + clazz + "." + METHOD + " (after override -> true)");
        } catch (Throwable t) {
            Log.e(TAG, "hook failed: " + Log.getStackTraceString(t));
            XposedBridge.log(TAG + " HOOK FAIL: " + Log.getStackTraceString(t));
        }
    }
}