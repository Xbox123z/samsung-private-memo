package com.dsh.privatememo;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Build;
import android.os.CancellationSignal;

/**
 * 指纹/生物识别门禁，兼容 Android 6.0 ~ 14：
 *  - API 28+ 用系统 BiometricPrompt
 *  - API 23~27 用 FingerprintManager
 *  - 无硬件时返回 NO_HARDWARE，由上层改用密码
 */
public class Bio {
    public interface Cb { void onOk(); void onFail(String why); void onNoHardware(); }

    private CancellationSignal cs;

    public static boolean hasHardware(Context c) {
        try {
            if (Build.VERSION.SDK_INT >= 28) return Bio28.hasHardware(c);
            if (Build.VERSION.SDK_INT >= 23) {
                FingerprintManager fm = (FingerprintManager) c.getSystemService(Context.FINGERPRINT_SERVICE);
                return fm != null && fm.isHardwareDetected() && fm.hasEnrolledFingerprints();
            }
        } catch (Throwable ignored) {}
        return false;
    }

    public void auth(final Activity act, final String title, final Cb cb) {
        try {
            if (!hasHardware(act)) { cb.onNoHardware(); return; }
            if (Build.VERSION.SDK_INT >= 28) { new Bio28().auth(act, title, cb); return; }
            FingerprintManager fm = (FingerprintManager) act.getSystemService(Context.FINGERPRINT_SERVICE);
            cs = new CancellationSignal();
            fm.authenticate(null, cs, 0, new FingerprintManager.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(FingerprintManager.AuthenticationResult r) { cb.onOk(); }
                @Override public void onAuthenticationFailed() { }
                @Override public void onAuthenticationError(int code, CharSequence msg) {
                    if (code == FingerprintManager.FINGERPRINT_ERROR_CANCELED) return;
                    cb.onFail(msg == null ? ("错误 " + code) : msg.toString());
                }
            }, null);
        } catch (Throwable t) {
            cb.onFail(t.getClass().getSimpleName());
        }
    }

    public void cancel() {
        try { if (cs != null) { cs.cancel(); cs = null; } } catch (Throwable ignored) {}
    }

    /** 设备是否设置了锁屏（用于判断 KO 兜底是否可用） */
    public static boolean hasDeviceLock(Context c) {
        try {
            KeyguardManager km = (KeyguardManager) c.getSystemService(Context.KEYGUARD_SERVICE);
            return km != null && km.isDeviceSecure();
        } catch (Throwable t) { return false; }
    }
}
