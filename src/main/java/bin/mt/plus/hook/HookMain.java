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
 *   类  l.۟᩻ۨ  (smali: Ll/۟᩻ۨ;)
 *   方法 static native ۡ()Z   (smali: Ll/۟᩻ۨ;->ۡ()Z)
 *   返回 true => 应用本地认为「会员/有权限」，绕过所有 VIP 门禁弹窗。
 * 本类强制使其恒返回 true，并记录原始 native 返回值以便收敛。
 */
public class HookMain implements IXposedHookLoadPackage {

    private static final String TAG = "MT2Hook";
    private static final String PKG = "bin.mt.plus";

    // VIP_CHECK_CLASS 无参方法短名常量被下述内联字符串取代,见 hookVipCheck
    private static final String VIP_CHECK_CLASS = "\u06DF\u1A7B\u06E8"; // l.۟᩻ۨ (码点经逆向核验)
    // 方法名 ۡ = U+06E1(核验过)
    private static final String VIP_CHECK_METHOD = "\u06E1";           // ۡ

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lp) {
        if (!lp.packageName.equals(PKG)) return;
        Log.i(TAG, "attached -> " + PKG);

        // 用 XposedBridge.log 保证一定可见
        XposedBridge.log(TAG + " module loaded on " + PKG);
        hookVipCheck(lp.classLoader);
    }

    private void hookVipCheck(ClassLoader cl) {
        try {
            // 短名落在包 'l' 下 => 完整类名应为 "l.<短名>"
            final String clazz = "l." + "\u06DF\u1A7B\u06E8"; // l.۟᩻ۨ (码点06DF,1A7B,06E8)
            Log.i(TAG, "hooking class=" + clazz + " method=" + "\u06E1");

            XposedHelpers.findAndHookMethod(
                    clazz, cl,
                    "\u06E1",                       // 方法名 ۡ (static native, 无参)
                    new XC_MethodHook() {
                        @Override
                        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
                            // 强制本地判定为「会员/有权限」
                            param.setResult(Boolean.TRUE);
                            Log.i(TAG, "VIP check FORCED->true");
                        }
                        @Override
                        protected void afterHookedMethod(MethodHookParam param) throws Throwable {
                            // 记录原始 native 返回值(即便被 before 覆盖,仍可读原生结果于 param 原引用)
                            Object orig = param.getResult();
                            Log.i(TAG, "native return was: " + orig + " (now forced true)");
                        }
                    });

            XposedBridge.log(TAG + " hooked " + clazz + "." + "\u06E1");
            Log.i(TAG, "hook installed -> forced VIP=true");
        } catch (Throwable t) {
            Log.e(TAG, "hook failed: " + Log.getStackTraceString(t));
            XposedBridge.log(TAG + " HOOK FAIL: " + Log.getStackTraceString(t));
        }
    }
}