# MT管理器(VIP) 本地判定 Patch — LSPosed 模块

## 目标
让 MT管理器(bin.mt.plus 2.26.8) 在**本地判定**层认为当前为会员/已开通 VIP，
从而放行文本编辑、插件、加字典等 VIP 门禁。**不触碰服务器授权逻辑**。

## 判定点（逆向结论）
- 全局统一判定入口：类 `l.۟᩻ۨ`（混淆名，smali 中为 `Ll/۟᩻ۨ;`）
- 方法：`public static native ۡ()Z` —— 返回 `true` 即认定会员/有权限。
- 该方法为 **static native**，函数体在受保护 so 中，但 ArtMethod 层 hook 依然可命中。

## Hook 策略
通过 Xposed/LSPosed 的 `XposedHelpers.findAndHookMethod` hook 该方法，令其恒返回 `true`。

### 文件名说明（Unicode 混淆）
真实类名/方法名含阿拉伯文混淆字符(U+06##区 ۟ ᩻ ۨ)：
- 类短名：`۟᩻ۨ`
- 包：`l`
- 方法：`ۡ`
在 Java 源码中可直接写这些 Unicode 字面量。

## 验证方法
1. 安装模块 → LSPosed 激活作用于 bin.mt.plus(推荐勾选 system 与 root) → 重启进程。
2. 打开 MT管理器底部 Tab 尝试 VIP 功能（文本编辑/插件/加字典）。
3. 若仍提示"该功能需要开通会员"，抓 logcat：
   `logcat -s MT2Hook` 并回传给分析者精确收敛。

## 副作用注意
native 判定内部可能返回多个维度，强改 `true` 若导致崩溃，请回传 Log 以调整到账号对象字段层。
