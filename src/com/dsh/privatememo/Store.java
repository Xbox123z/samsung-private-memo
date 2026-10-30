package com.dsh.privatememo;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Base64;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.List;

/** 10 个独立账号库；每个库单独的 AES 密钥 + 单独的加密文件 */
public class Store {
    public static final int VAULTS = 10;
    private static final String PREFS = "pm_store";

    public static class Note {
        public String id = "", title = "", body = "";
        public long created = 0, updated = 0;
        public Note() {}
        Note(String i, String t, String b, long c, long u) {
            id=i; title=t; body=b; created=c; updated=u;
        }
    }

    private final Context ctx;
    private final SharedPreferences sp;

    public Store(Context c) {
        ctx = c.getApplicationContext();
        sp = ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    // ---------- 账号元数据 ----------
    public String vaultName(int idx) {
        return sp.getString("name_" + idx, "账号 " + (idx + 1));
    }
    public void setVaultName(int idx, String name) {
        sp.edit().putString("name_" + idx, name).apply();
    }
    public boolean hasPassword(int idx) { return sp.contains("pwd_" + idx); }
    public String salt(int idx) {
        String s = sp.getString("salt_" + idx, null);
        if (s == null) {
            s = Base64.encodeToString(Crypto.random(16), Base64.NO_WRAP);
            sp.edit().putString("salt_" + idx, s).apply();
        }
        return s;
    }
    public void setPassword(int idx, String pwd) {
        try {
            String h = Crypto.hashPwd(pwd, salt(idx));
            sp.edit().putString("pwd_" + idx, h).apply();
        } catch (Exception ignored) {}
    }
    public boolean checkPassword(int idx, String pwd) {
        String h = sp.getString("pwd_" + idx, null);
        if (h == null) return true;
        try { return h.equals(Crypto.hashPwd(pwd, salt(idx))); } catch (Exception e) { return false; }
    }
    public void clearPassword(int idx) { sp.edit().remove("pwd_" + idx).apply(); }

    // ---------- 每个库的密钥 ----------
    private byte[] key(int idx) throws Exception {
        String w = sp.getString("key_" + idx, null);
        if (w == null) {
            byte[] k = Crypto.random(32);
            sp.edit().putString("key_" + idx, Crypto.wrap(k)).apply();
            return k;
        }
        return Crypto.unwrap(w);
    }

    // ---------- 笔记读写 ----------
    private File file(int idx) { return new File(ctx.getFilesDir(), "vault_" + idx + ".dat"); }

    public List<Note> load(int idx) {
        List<Note> out = new ArrayList<>();
        File f = file(idx);
        if (!f.exists()) return out;
        try {
            byte[] all = new byte[(int) f.length()];
            FileInputStream in = new FileInputStream(f);
            int off = 0, r;
            while (off < all.length && (r = in.read(all, off, all.length - off)) > 0) off += r;
            in.close();
            byte[] plain = Crypto.open(key(idx), new String(all, "UTF-8"));
            JSONArray arr = new JSONArray(new String(plain, "UTF-8"));
            for (int i = 0; i < arr.length(); i++) {
                JSONObject o = arr.getJSONObject(i);
                out.add(new Note(o.optString("id"), o.optString("title"), o.optString("body"),
                        o.optLong("created"), o.optLong("updated")));
            }
        } catch (Exception e) {
            android.util.Log.e("Memo", "load vault " + idx + " failed: " + e);
        }
        return out;
    }

    public boolean save(int idx, List<Note> notes) {
        try {
            JSONArray arr = new JSONArray();
            for (Note n : notes) {
                JSONObject o = new JSONObject();
                o.put("id", n.id); o.put("title", n.title); o.put("body", n.body);
                o.put("created", n.created); o.put("updated", n.updated);
                arr.put(o);
            }
            String enc = Crypto.seal(key(idx), arr.toString().getBytes("UTF-8"));
            FileOutputStream fo = new FileOutputStream(file(idx));
            fo.write(enc.getBytes("UTF-8"));
            fo.close();
            return true;
        } catch (Exception e) {
            android.util.Log.e("Memo", "save vault " + idx + " failed: " + e);
            return false;
        }
    }

    public int count(int idx) { return load(idx).size(); }
}
