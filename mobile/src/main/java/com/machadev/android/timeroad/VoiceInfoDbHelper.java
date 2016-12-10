package com.machadev.android.timeroad;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

/**
 * 音声情報DBヘルパークラス
 */
public class VoiceInfoDbHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME_DATABASE = "voice_info.db";
    public static final String DB_NAME_VOICE_TABLE = "voice_info";
    public static final String DB_ITEM_ID = "id";
    public static final String DB_ITEM_VOICE_RATE = "rate";             // rate
    public static final String DB_ITEM_VOICE_PITCH = "pitch";           // pitch
    public static final String DB_ITEM_VOICE_TYPE = "voice_type";       // 音質タイプ
    public static final String DB_ITEM_VOICE_TEXT = "voice_text";       // テキストパターン
    public static final String DB_ITEM_VOICE_USE_FLAG = "use_flag";     // 使用フラグ

    /**
     * コンストラクタ
     * @param context
     */
    public VoiceInfoDbHelper(Context context) {
        super(context, DB_NAME_DATABASE, null, DB_VERSION);
    }

    /**
     * onCreate
     * @param db
     */
    @Override
    public void onCreate(SQLiteDatabase db) {
        // データベース作成
        StringBuffer sb = new StringBuffer();
        sb.append("create table " + DB_NAME_VOICE_TABLE);
        sb.append("(");
        sb.append(DB_ITEM_ID + " integer primary key autoincrement,");
        sb.append(DB_ITEM_VOICE_TYPE + " text,");
        sb.append(DB_ITEM_VOICE_RATE + " real,");
        sb.append(DB_ITEM_VOICE_PITCH + " real,");
        sb.append(DB_ITEM_VOICE_TEXT + " text,");
        sb.append(DB_ITEM_VOICE_USE_FLAG + " integer");
        sb.append(")");
        String sql = sb.toString();
        db.execSQL(sql);
    }

    /**
     * onUpdate
     * @param db
     * @param oldVersion
     * @param newVersion
     */
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "";
        sql += "drop table ";
        sql += DB_NAME_VOICE_TABLE;
        db.execSQL(sql);
        onCreate(db);
    }
}
