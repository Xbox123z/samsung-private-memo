package com.dsh.privatememo;

import android.app.Activity;
import android.app.AlertDialog;
import android.graphics.Typeface;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import java.util.List;
import java.util.UUID;

public class NoteEditActivity extends Activity {
    private Store store;
    private int vault = 0;
    private Store.Note note = null;
    private EditText etTitle, etBody;

    @Override protected void onCreate(Bundle b) {
        super.onCreate(b);
        store = new Store(this);
        vault = getIntent().getIntExtra("vault", 0);
        String id = getIntent().getStringExtra("noteId");

        List<Store.Note> notes = store.load(vault);
        if (id != null) {
            for (Store.Note n : notes) if (n.id.equals(id)) { note = n; break; }
        }
        if (note == null) {
            note = new Store.Note();
        }

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(0xFF0E1116);
        root.setPadding(dp(14), dp(12), dp(14), dp(12));

        TextView tv = new TextView(this);
        tv.setText(store.vaultName(vault) + " · 编辑");
        tv.setTextColor(0xFFEDEFF2); tv.setTextSize(17); tv.setTypeface(Typeface.DEFAULT_BOLD);
        root.addView(tv);

        etTitle = new EditText(this);
        etTitle.setHint("标题");
        etTitle.setText(note.title);
        etTitle.setTextColor(0xFFEDEFF2);
        etTitle.setHintTextColor(0xFF6E7683);
        etTitle.setTextSize(18);
        etTitle.setSingleLine(true);
        root.addView(etTitle);

        etBody = new EditText(this);
        etBody.setHint("内容…");
        etBody.setText(note.body);
        etBody.setTextColor(0xFFDFE3E8);
        etBody.setHintTextColor(0xFF6E7683);
        etBody.setGravity(Gravity.TOP);
        etBody.setBackgroundColor(0xFF171B22);
        root.addView(etBody, new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, 0, 1f));

        LinearLayout bar = new LinearLayout(this);
        bar.setOrientation(LinearLayout.HORIZONTAL);
        bar.setPadding(0, dp(10), 0, 0);

        Button save = new Button(this);
        save.setText("保存");
        save.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) { doSave(false); }
        });
        bar.addView(save, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        Button del = new Button(this);
        del.setText("删除");
        del.setOnClickListener(new View.OnClickListener() {
            public void onClick(View x) {
                new AlertDialog.Builder(NoteEditActivity.this).setTitle("删除这条笔记？")
                        .setPositiveButton("删除", new android.content.DialogInterface.OnClickListener() {
                            public void onClick(android.content.DialogInterface d, int w) { doDelete(); }
                        }).setNegativeButton("取消", null).show();
            }
        });
        bar.addView(del, new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        root.addView(bar);
        setContentView(root);
    }

    private void doSave(boolean silent) {
        String t = etTitle.getText().toString().trim();
        String body = etBody.getText().toString();
        if (t.isEmpty() && body.trim().isEmpty()) { finish(); return; }
        long now = System.currentTimeMillis();
        if (note.id.isEmpty()) { note.id = UUID.randomUUID().toString(); note.created = now; }
        note.title = t;
        note.body = body;
        note.updated = now;

        List<Store.Note> notes = store.load(vault);
        boolean replaced = false;
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(note.id)) { notes.set(i, note); replaced = true; break; }
        }
        if (!replaced) notes.add(0, note);
        if (store.save(vault, notes)) {
            if (!silent) { Toast.makeText(this, "已加密保存", Toast.LENGTH_SHORT).show(); finish(); }
        } else {
            Toast.makeText(this, "保存失败", Toast.LENGTH_SHORT).show();
        }
    }

    private void doDelete() {
        List<Store.Note> notes = store.load(vault);
        for (int i = 0; i < notes.size(); i++) {
            if (notes.get(i).id.equals(note.id)) { notes.remove(i); break; }
        }
        store.save(vault, notes);
        Toast.makeText(this, "已删除", Toast.LENGTH_SHORT).show();
        finish();
    }

    @Override protected void onPause() { super.onPause(); if (note != null) doSave(true); }

    private int dp(int v) { return (int) (v * getResources().getDisplayMetrics().density); }
}
