package com.dsh.privatememo;

import android.app.Activity;
import android.hardware.biometrics.BiometricPrompt;
import android.os.Build;
import android.os.CancellationSignal;
import java.util.concurrent.Executor;

/** API 28+ 的生物识别实现（单独成类，避免低版本设备加载到该 API） */
public class Bio28 {
    public static boolean hasHardware(android.content.Context c) {
        if (Build.VERSION.SDK_INT < 28) return false;
        try {
            android.hardware.biometrics.BiometricManager bm =
                    c.getSystemService(android.hardware.biometrics.BiometricManager.class);
            if (bm == null) return false;
            int r = bm.canAuthenticate();
            return r == android.hardware.biometrics.BiometricManager.BIOMETRIC_SUCCESS;
        } catch (Throwable t) { return false; }
    }

    public void auth(final Activity act, String title, final Bio.Cb cb) {
        try {
            Executor ex = new Executor() { public void execute(Runnable r) { act.runOnUiThread(r); } };
            BiometricPrompt bp = new BiometricPrompt.Builder(act)
                    .setTitle(title)
                    .setNegativeButton("取消", ex, new android.content.DialogInterface.OnClickListener() {
                        public void onClick(android.content.DialogInterface d, int w) { cb.onFail("已取消"); }
                    }).build();
            bp.authenticate(new CancellationSignal(), ex, new BiometricPrompt.AuthenticationCallback() {
                @Override public void onAuthenticationSucceeded(BiometricPrompt.AuthenticationResult r) { cb.onOk(); }
                @Override public void onAuthenticationError(int code, CharSequence msg) {
                    if (code == BiometricPrompt.BIOMETRIC_ERROR_CANCELED
                            || code == BiometricPrompt.BIOMETRIC_ERROR_USER_CANCELED) return;
                    cb.onFail(msg == null ? ("错误 " + code) : msg.toString());
                }
            });
        } catch (Throwable t) {
            cb.onFail(t.getClass().getSimpleName());
        }
    }
}
