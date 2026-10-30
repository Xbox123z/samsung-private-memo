package com.dsh.privatememo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {
    private Store store;
    private LinearLayout grid;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        setContentView(buildUi());
    }

    @Override protected void onResume() { super.onResume(); refresh(); }

    private View buildUi() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E1116);

        // 封面
        ImageView cover = new ImageView(this);
        cover.setImageResource(R.drawable.cover);
        cover.setScaleType(ImageView.ScaleType.CENTER_CROP);
        root.addView(cover, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(150)));

        TextView t = new TextView(this);
        t.setText("三星隐私备忘录");
        t.setTextColor(0xFFEDEFF2);
        t.setTextSize(22);
        t.setTypeface(Typeface.DEFAULT_BOLD);
        t.setGravity(Gravity.CENTER);
        t.setPadding(0, dp(14), 0, dp(4));
        root.addView(t);

        TextView sub = new TextView(this);
        sub.setText("选择账号 · 指纹或密码解锁 · 每个账号内容独立加密");
        sub.setTextColor(0xFF8A93A0);
        sub.setTextSize(12);
        sub.setGravity(Gravity.CENTER);
        sub.setPadding(0, 0, 0, dp(10));
        root.addView(sub);

        ScrollView sv = new ScrollView(this);
        grid = new LinearLayout(this);
        grid.setOrientation(LinearLayout.VERTICAL);
        grid.setPadding(dp(10), 0, dp(10), dp(20));
        sv.addView(grid);
        root.addView(sv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));
        return root;
    }

    private void refresh() {
        grid.removeAllViews();
        for (int row = 0; row < 5; row++) {
            LinearLayout line = new LinearLayout(this);
            line.setOrientation(LinearLayout.HORIZONTAL);
            for (int col = 0; col < 2; col++) {
                final int idx = row * 2 + col;
                line.addView(card(idx), new LinearLayout.LayoutParams(0, dp(96), 1f));
            }
            grid.addView(line);
        }
    }

    private View card(final int idx) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        c.setGravity(Gravity.CENTER);
        c.setBackgroundColor(0xFF171B22);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(0, dp(88), 1f);
        lp.setMargins(dp(5), dp(5), dp(5), dp(5));
        c.setLayoutParams(lp);

        TextView name = new TextView(this);
        name.setText(store.vaultName(idx));
        name.setTextColor(0xFFEDEFF2);
        name.setTextSize(15);
        name.setTypeface(Typeface.DEFAULT_BOLD);

        TextView info = new TextView(this);
        int n = store.count(idx);
        info.setText(n + " 条笔记" + (store.hasPassword(idx) ? " · 🔒" : ""));
        info.setTextColor(0xFF8A93A0);
        info.setTextSize(11);
        info.setPadding(0, dp(4), 0, 0);

        c.addView(name); c.addView(info);
        c.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) { openVault(idx, true); }
        });
        c.setOnLongClickListener(new View.OnLongClickListener() {
            public boolean onLongClick(View v) { menu(idx); return true; }
        });
        return c;
    }

    private void menu(final int idx) {
        final String[] items = {"重命名账号", "设置/修改密码", "清除密码"};
        new AlertDialog.Builder(this).setTitle(store.vaultName(idx))
                .setItems(items, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (w == 0) askName(idx);
                        else if (w == 1) askSetPwd(idx);
                        else { store.clearPassword(idx); Toast.makeText(MainActivity.this, "已清除密码", Toast.LENGTH_SHORT).show(); refresh(); }
                    }
                }).show();
    }

    private void askName(final int idx) {
        final EditText e = new EditText(this);
        e.setText(store.vaultName(idx));
        new AlertDialog.Builder(this).setTitle("账号名称").setView(e)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        String s = e.getText().toString().trim();
                        if (!s.isEmpty()) store.setVaultName(idx, s);
                        refresh();
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void askSetPwd(final int idx) {
        final EditText e = new EditText(this);
        e.setHint("输入新密码（至少 4 位）");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle("设置密码").setView(e)
                .setPositiveButton("保存", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        String s = e.getText().toString();
                        if (s.length() < 4) { Toast.makeText(MainActivity.this, "太短了", Toast.LENGTH_SHORT).show(); return; }
                        store.setPassword(idx, s);
                        Toast.makeText(MainActivity.this, "已设置", Toast.LENGTH_SHORT).show(); refresh();
                    }
                }).setNegativeButton("取消", null).show();
    }

    /** 打开某账号：先过指纹/密码 */
    private void openVault(final int idx, boolean needAuth) {
        if (!needAuth || (!Bio.hasHardware(this) && !store.hasPassword(idx))) { go(idx); return; }
        if (Bio.hasHardware(this)) {
            new Bio().auth(this, store.vaultName(idx), new Bio.Cb() {
                public void onOk() { go(idx); }
                public void onFail(String why) { Toast.makeText(MainActivity.this, "认证失败：" + why, Toast.LENGTH_SHORT).show(); }
                public void onNoHardware() { askPwd(idx); }
            });
        } else askPwd(idx);
    }

    private void askPwd(final int idx) {
        final EditText e = new EditText(this);
        e.setHint("账号密码");
        e.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle(store.vaultName(idx)).setView(e)
                .setPositiveButton("进入", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (store.checkPassword(idx, e.getText().toString())) go(idx);
                        else Toast.makeText(MainActivity.this, "密码错误", Toast.LENGTH_SHORT).show();
                    }
                }).setNegativeButton("取消", null).show();
    }

    private void go(int idx) {
        Intent i = new Intent(this, VaultActivity.class);
        i.putExtra("vault", idx);
        startActivity(i);
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
