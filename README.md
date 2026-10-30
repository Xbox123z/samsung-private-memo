# 三星隐私备忘录 · Samsung Private Memo

10 个独立加密账号的安卓备忘录。**一根手指 = 一个账号**：不同手指通过系统「指纹快捷启动」绑定到不同账号入口，进入后内容完全隔离、各自加密。

## 功能

| 功能 | 说明 |
|---|---|
| 🔐 10 个独立账号 | 每个账号一个独立加密库（vault_0 ~ vault_9），互不可见 |
| ☝️ 指纹/生物识别解锁 | Android 6.0+ 通用（API 28+ 用 BiometricPrompt，23~27 用 FingerprintManager） |
| 🔑 AES-256-GCM 加密 | 每个账号随机 256 位数据密钥，主密钥由 **Android Keystore（硬件级）** 保护 |
| 🔒 账号密码兜底 | 无指纹硬件 / 指纹被拒时可用密码；PBKDF2-SHA256 10000 轮 |
| 📝 完整笔记功能 | 新建 / 编辑 / 删除 / 搜索（标题+正文）/ 时间戳 / 自动保存 |
| 🏷️ 账号重命名 | 长按账号卡片 → 重命名 / 设置密码 / 清除密码 |
| 🌙 深色主题 | Material Dark，原生 View，无第三方依赖 |

## 十指十账号是怎么实现的（诚实说明）

Android 平台 API **不会告诉 App 是哪根手指**按的（所有已注册指纹在 API 看来等价）。所以：

1. App 提供 **10 个独立桌面入口**（`账号 1` ~ `账号 10`，activity-alias 实现，任何安卓手机都有 10 个图标）
2. 在支持「**指纹快捷启动**」的手机（华为/荣耀等）上：设置 → 生物识别 → 指纹快捷启动，把不同手指分别绑到不同图标 → **按哪根手指，直接进哪个账号** ✅ 完全还原设想
3. 在**不支持**该功能的手机上（小米/OV/三星/原生）：打开 App 后在账号列表里点选目标账号 → 指纹验证一次 → 进入。功能等价，只是多一步点击

## 安全模型

```
数据密钥 DK（每账号随机 32 字节）
  └─ 被 Android Keystore 主密钥 KEK 包装后存 SharedPreferences
       └─ KEK 永不离开 Keystore（硬件 TEE 优先）
笔记库 vault_N.dat = AES-256-GCM(DK, JSON)
密码校验 = PBKDF2WithHmacSHA256(pwd, salt, 10000) → SHA 比对
```

## 兼容性

- `minSdkVersion 23`（Android 6.0）~ `targetSdkVersion 33`（Android 13/14）
- 纯 Java + 原生 View，**零第三方依赖**（不依赖 androidx / gradle）
- 已适配：Huawei HarmonyOS 4.3 (Android 12) 实测安装运行正常

## 下载

[Releases](../../releases) 页有签名 APK（v1+v2 双签，约 194 KB）。

```
v1: files=4 covered=4 missing=0 baddigest=0
v2: algo_match=True(0x103/0x103) digest=OK
```

## 构建（可选）

仓库 `src/` + `res/` + `AndroidManifest.xml` 即完整工程。用 Android SDK 工具链：

```
aapt2 compile + link → javac（source/target 8，勿用 lambda）→ d8 → 注入 classes.dex → apksigner
```

或直接下载 release APK。

## 隐私

- 所有内容仅存本机（应用私有目录），**无任何网络请求、无广告、无统计**
- 不申请存储/通讯录/位置等无关权限，只申请生物识别
