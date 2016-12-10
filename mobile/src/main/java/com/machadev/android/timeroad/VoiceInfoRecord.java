package com.machadev.android.timeroad;

import android.content.ContentValues;

/**
 * 音声情報レコードクラス
 */
public class VoiceInfoRecord {

    public int mNumber;         // 番号
    public float mRate;         // rate
    public float mPitch;        // pitch
    public String mVoiceType;   // 音質タイプ
    public String mVoiceText;   // 再生テキストパターン
    public boolean mUseFlag;    // 使用フラグ
    /**
     * コンストラクタ
     */
    public VoiceInfoRecord() {
        // 初期化
        init();
    }

    /**
     * 初期化
     */
    private void init() {
        mNumber = 1;
        mRate = 1.0f;
        mPitch = 1.0f;
        mVoiceType = "fa0001";
        mVoiceText = "今何時？";
        mUseFlag = true;
    }

    /**
     * データベース保存用データ取得
     * @return
     */
    public ContentValues getContentValues() {
        // 番号はLIstView表示用なのでDBに保存しない
        ContentValues values = new ContentValues();
        // 音質タイプ
        values.put(VoiceInfoDbHelper.DB_ITEM_VOICE_TYPE, mVoiceType);
        // rate
        values.put(VoiceInfoDbHelper.DB_ITEM_VOICE_RATE, mRate);
        // pitch
        values.put(VoiceInfoDbHelper.DB_ITEM_VOICE_PITCH, mPitch);
        // 再生テキスト
        values.put(VoiceInfoDbHelper.DB_ITEM_VOICE_TEXT, mVoiceText);
        // 使用フラグ
        int flag = 0;
        if (mUseFlag) {
            flag = 1;
        }
        values.put(VoiceInfoDbHelper.DB_ITEM_VOICE_USE_FLAG, flag);

        return values;
    }
}
