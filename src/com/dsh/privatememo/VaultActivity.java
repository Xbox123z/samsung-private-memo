package com.dsh.privatememo;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import java.util.ArrayList;
import java.util.List;

public class VaultActivity extends Activity {
    private Store store;
    private int vault = 0;
    private final List<Store.Note> all = new ArrayList<>();
    private final List<Store.Note> shown = new ArrayList<>();
    private Adapter adapter;
    private boolean unlocked = false;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        vault = resolveVault();
        if (store.hasPassword(vault) || Bio.hasHardware(this)) gate();
        else { unlocked = true; show(); }
    }

    /** 从 10 个启动别名（.V1 ... .V10）或 intent extra 解析出账号序号 */
    private int resolveVault() {
        int v = getIntent().getIntExtra("vault", -1);
        if (v >= 0 && v < Store.VAULTS) return v;
        try {
            String cn = getIntent().getComponent() == null ? "" : getIntent().getComponent().getClassName();
            if (cn != null) {
                int i = cn.lastIndexOf(".V");
                if (i > 0) {
                    String num = cn.substring(i + 2);
                    int n = Integer.parseInt(num);
                    if (n >= 1 && n <= Store.VAULTS) return n - 1;
                }
            }
        } catch (Throwable ignored) {}
        return 0;
    }

    private void gate() {
        if (Bio.hasHardware(this)) {
            new Bio().auth(this, store.vaultName(vault), new Bio.Cb() {
                public void onOk() { unlocked = true; show(); }
                public void onFail(String why) { askPwd(); }
                public void onNoHardware() { askPwd(); }
            });
        } else askPwd();
    }

    private void askPwd() {
        final EditText e = new EditText(this);
        e.setHint("账号密码");
        e.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
        new AlertDialog.Builder(this).setTitle(store.vaultName(vault)).setView(e)
                .setCancelable(false)
                .setPositiveButton("解锁", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        if (store.checkPassword(vault, e.getText().toString())) { unlocked = true; show(); }
                        else { Toast.makeText(VaultActivity.this, "密码错误", Toast.LENGTH_SHORT).show(); finish(); }
                    }
                }).setNegativeButton("退出", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) { finish(); }
                }).show();
    }

    private void show() {
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E1116);

        // 顶栏
        LinearLayout top = new LinearLayout(this);
        top.setOrientation(LinearLayout.HORIZONTAL);
        top.setGravity(Gravity.CENTER_VERTICAL);
        top.setPadding(dp(12), dp(10), dp(12), dp(6));

        TextView title = new TextView(this);
        title.setText(store.vaultName(vault));
        title.setTextColor(0xFFEDEFF2);
        title.setTextSize(19);
        title.setTypeface(Typeface.DEFAULT_BOLD);
        LinearLayout.LayoutParams tlp = new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f);
        top.addView(title, tlp);

        Button add = new Button(this);
        add.setText("＋ 新建");
        add.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { startActivity(new Intent(VaultActivity.this, NoteEditActivity.class).putExtra("vault", vault)); }
        });
        top.addView(add);
        root.addView(top);

        // 搜索
        EditText search = new EditText(this);
        search.setHint("搜索标题或内容");
        search.setTextColor(0xFFEDEFF2);
        search.setHintTextColor(0xFF6E7683);
        search.setSingleLine(true);
        search.addTextChangedListener(new TextWatcher() {
            public void beforeTextChanged(CharSequence s, int a, int b, int c) {}
            public void onTextChanged(CharSequence s, int a, int b, int c) { filter(s.toString()); }
            public void afterTextChanged(Editable s) {}
        });
        root.addView(search);

        ListView lv = new ListView(this);
        adapter = new Adapter();
        lv.setAdapter(adapter);
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> p, View v, int pos, long id) {
                Store.Note n = shown.get(pos);
                startActivity(new Intent(VaultActivity.this, NoteEditActivity.class)
                        .putExtra("vault", vault).putExtra("noteId", n.id));
            }
        });
        lv.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> p, View v, int pos, long id) {
                confirmDelete(shown.get(pos));
                return true;
            }
        });
        root.addView(lv, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        setContentView(root);
        reload();
    }

    private void reload() {
        all.clear();
        all.addAll(store.load(vault));
        filter("");
    }

    private void filter(String q) {
        shown.clear();
        String s = q == null ? "" : q.trim().toLowerCase();
        for (Store.Note n : all) {
            if (s.isEmpty() || n.title.toLowerCase().contains(s) || n.body.toLowerCase().contains(s)) shown.add(n);
        }
        if (adapter != null) adapter.notifyDataSetChanged();
    }

    private void confirmDelete(final Store.Note n) {
        new AlertDialog.Builder(this).setTitle("删除笔记").setMessage(n.title)
                .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface d, int w) {
                        all.remove(n);
                        store.save(vault, all);
                        reload();
                    }
                }).setNegativeButton("取消", null).show();
    }

    private class Adapter extends BaseAdapter {
        public int getCount() { return shown.size(); }
        public Object getItem(int i) { return shown.get(i); }
        public long getItemId(int i) { return i; }
        public View getView(int i, View cv, ViewGroup p) {
            LinearLayout l = new LinearLayout(VaultActivity.this);
            l.setOrientation(LinearLayout.VERTICAL);
            l.setPadding(dp(14), dp(10), dp(14), dp(10));
            Store.Note n = shown.get(i);
            TextView t = new TextView(VaultActivity.this);
            t.setText(n.title.isEmpty() ? "(无标题)" : n.title);
            t.setTextColor(0xFFEDEFF2); t.setTextSize(16); t.setTypeface(Typeface.DEFAULT_BOLD);
            TextView d = new TextView(VaultActivity.this);
            String body = n.body.replace("\n", " ");
            if (body.length() > 40) body = body.substring(0, 40) + "…";
            d.setText(body); d.setTextColor(0xFF8A93A0); d.setTextSize(12); d.setPadding(0, dp(3), 0, 0);
            TextView tm = new TextView(VaultActivity.this);
            tm.setText(android.text.format.DateFormat.format("yyyy-MM-dd HH:mm", n.updated));
            tm.setTextColor(0xFF5C636E); tm.setTextSize(10); tm.setPadding(0, dp(3), 0, 0);
            l.addView(t); l.addView(d); l.addView(tm);
            return l;
        }
    }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
