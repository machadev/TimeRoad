package com.machadev.android.timeroad;

import android.app.AlertDialog;
import android.content.ContentValues;
import android.content.DialogInterface;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.media.MediaPlayer;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.Spinner;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.MessageEvent;
import com.google.android.gms.wearable.Wearable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

public class MainActivity extends AppCompatActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        MessageApi.MessageListener,
        TextToSpeech.OnInitListener,
        AdapterView.OnItemClickListener,
        AdapterView.OnItemLongClickListener {

    private final String TAG_WHAT_TIME = "What time client";
    private final float PITCH_LOW = 0.5f;
    private final float PITCH_NORMAL = 1.0f;
    private final float PITCH_HIGH = 1.5f;
    private final float RATE_LOW = 0.5f;
    private final float RATE_NORMAL = 1.0f;
    private final float RATE_HIGH = 1.5f;

    private GoogleApiClient mGoogleApiClient;
    private TextToSpeech mTts;

    private MediaPlayer mp;
    private Timer resultTimer = new Timer();

    private VoiceInfoAdapter mAdapterVoiceInfo = null;
    private List<VoiceInfoRecord> mVoiceInfoItemList = new ArrayList<VoiceInfoRecord>();
    private VoiceInfoDbHelper mVoiceInfoDbHelper;
    // 再生用リスト(ListViewからチェックOFFを除いたもの)
    private List<VoiceInfoRecord> mVoicePatternList = new ArrayList<VoiceInfoRecord>();

    @BindView(R.id.etPitch) EditText etPitch;
    @BindView(R.id.etRate) EditText etRate;
    @BindView(R.id.etText) EditText etTExt;
    @BindView(R.id.spinnerVoice) Spinner spinnerVoice;
    @BindView(R.id.lvVoiceInfo) ListView lvVoiceInfo;

    /**
     * onCreate
     * @param savedInstanceState
     */
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // 初期化
        init();

        // データベースから再生データを読み込みリストに表示
        readVoiceData();
    }

    /**
     * onResume
     */
    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            // Google PAI接続
            mGoogleApiClient.connect();
        }
    }

    /**
     * onPause
     */
    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            // Wearableメッセージリスナー解除
            Wearable.MessageApi.removeListener(mGoogleApiClient, this);
            // Google API切断
            mGoogleApiClient.disconnect();
        }
    }

    /**
     * onDestroy
     */
    @Override
    protected void onDestroy() {
        super.onDestroy();
        shutdown();
        if (resultTimer != null) {
            resultTimer.cancel();
        }
    }

    /**
     * TTS初期化
     * @param status
     */
    @Override
    public void onInit(int status) {
        if (status != TextToSpeech.SUCCESS) {
            Log.d(TAG_WHAT_TIME, "TTS Success.");
        } else {
            Log.d(TAG_WHAT_TIME, "TTS Error.");
        }
    }

    /**
     * 初期化
     */
    private void init() {
        // Wear通信用のGoogleApiClient生成
        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();

        // TTS生成
        mTts = new TextToSpeech(this, this);

// 音データは削除
//        // キーン音を鳴らすMediaPlayer生成
//        mp = MediaPlayer.create(this, R.raw.onmtp_flash04_1);

        // 履歴ListViewのスクロール時のキャッシュを無効にする
        lvVoiceInfo.setScrollingCacheEnabled(false);
        // 履歴ListViewにAdapter設定
        mAdapterVoiceInfo = new VoiceInfoAdapter(this, mVoiceInfoItemList);
        lvVoiceInfo.setAdapter(mAdapterVoiceInfo);
        lvVoiceInfo.setOnItemClickListener(this);
        lvVoiceInfo.setOnItemLongClickListener(this);

        // DBヘルパー生成
        mVoiceInfoDbHelper = new VoiceInfoDbHelper(getApplicationContext());
        SQLiteDatabase db = mVoiceInfoDbHelper.getWritableDatabase();
        db.close();
    }

    /**
     * 速さ(遅く)ボタンクリック
     */
    @OnClick(R.id.btnRateLow)
    public void onRateLow(View view) {
        //etRate.setText(String.valueOf(RATE_LOW));
        String str = etRate.getText().toString();
        float val = Float.valueOf(str);
        val -= 0.1;
        etRate.setText(String.format("%.2f", val));
    }

    /**
     * 速さ(普通)ボタンクリック
     */
    @OnClick(R.id.btnRateNormal)
    public void onRateNormal(View view) {
        etRate.setText(String.format("%.2f", RATE_NORMAL));
    }

    /**
     * 速さ(速く)ボタンクリック
     */
    @OnClick(R.id.btnRateHigh)
    public void onRateHigh(View view) {
        //etRate.setText(String.valueOf(RATE_HIGH));
        String str = etRate.getText().toString();
        float val = Float.valueOf(str);
        val += 0.1;
        etRate.setText(String.format("%.2f", val));
    }

    /**
     * 高さ(低く)ボタンクリック
     */
    @OnClick(R.id.btnPitchLow)
    public void onPitchLow(View view) {
        //etPitch.setText(String.valueOf(PITCH_LOW));
        String str = etPitch.getText().toString();
        float val = Float.valueOf(str);
        val -= 0.1;
        etPitch.setText(String.format("%.2f", val));
    }

    /**
     * 高さ(普通)ボタンクリック
     */
    @OnClick(R.id.btnPitchNormal)
    public void onPitchNormal(View view) {
        etPitch.setText(String.format("%.2f", PITCH_NORMAL));
    }

    /**
     * 高さ(高く)ボタンクリック
     */
    @OnClick(R.id.btnPitchHigh)
    public void onPitchHigh(View view) {
        //etPitch.setText(String.valueOf(PITCH_HIGH));
        String str = etPitch.getText().toString();
        float val = Float.valueOf(str);
        val += 0.1;
        etPitch.setText(String.format("%.2f", val));
    }

    /**
     * しゃべるボタンクリック
     */
    @OnClick(R.id.btnSpeech)
    public void onSpeech(View view) {
        TestSpeech();
    }

    /**
     * 現在の音声情報をリストに追加
     * @param view
     */
    @OnClick(R.id.btnAddList)
    public void onAddList(View view) {
        float rate = Float.valueOf(etRate.getText().toString());
        float pitch = Float.valueOf(etPitch.getText().toString());
        String text = etTExt.getText().toString();
        String item = (String)spinnerVoice.getSelectedItem();
        String voice = item.substring(item.indexOf(" ")+1);
        // 音声リストに音声情報を追加
        addListVoiceInfo(rate, pitch, voice, text, true);
    }

    /**
     * リストの内容でDBに保存して再生データを設定
     * @param view
     */
    @OnClick(R.id.btnSetup)
    public void onSetupData(View view) {
        // リストの再生データをDBに登録
        if (registerVoiceData() == false) {
            showSnackBar("データベースの登録に失敗しました.");
            return;
        }

        // データベースから再生データを読み込みリストに表示
        if (readVoiceData() == false) {
            showSnackBar("データベースの読み込みに失敗しました.");
            return;
        }
        showSnackBar("データセットが完了しました.");
    }

    /**
     * ランダムに今何時を選択するテスト
     * @param view
     */
    @OnClick(R.id.btnRandomTest)
    public void onRandomTest(View view) {
        speechWhatTime();
    }

    /**
     * テキストスピーチ実行
     * @param str
     */
    private void speech(String str) {
        if (mTts != null) {
            String utteranceId = this.hashCode() + "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                // API 22以上
                mTts.speak(str, TextToSpeech.QUEUE_FLUSH, null, utteranceId);
            } else {
                // API 22以前
                HashMap<String, String> map = new HashMap<>();
                map.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceId);
                mTts.speak(str, TextToSpeech.QUEUE_FLUSH, map);
            }
        }
    }

    /**
     * パラメータをしていしてでキストスピート実行
     * @param rate
     * @param pitch
     * @param voice
     * @param text
     */
    private void speech(float rate, float pitch, String voice, String text) {
        Locale locale = new Locale(
                Locale.JAPAN.getLanguage(),
                Locale.JAPAN.getCountry(),
                voice);
        setLanguage(locale);
        setSpeechRate(rate);
        setSpeechPitch(pitch);
        speech(text);
    }

    /**
     * 音声リストに音声情報を追加
     * @param rate
     * @param pitch
     * @param voice
     * @param text
     * @param use
     */
    private void addListVoiceInfo(float rate, float pitch, String voice, String text, boolean use) {
        VoiceInfoRecord record = new VoiceInfoRecord();
        record.mNumber = mAdapterVoiceInfo.getCount()+1;
        record.mRate = rate;
        record.mPitch = pitch;
        record.mVoiceType = voice;
        record.mVoiceText = text;
        record.mUseFlag = use;
        mAdapterVoiceInfo.add(record);
    }

    /**
     * スナックバーを表示
     * @param str
     */
    private void showSnackBar(String str) {
        LinearLayout layout = (LinearLayout)findViewById(R.id.layoutMain);
        Snackbar.make(layout, str, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * TTS解放
     */
    private void shutdown() {
        if (mTts != null) {
            mTts.shutdown();
        }
    }

    /**
     * レート設定
     * @param rate
     */
    private void setSpeechRate(float rate) {
        if (mTts != null) {
            mTts.setSpeechRate(rate);
        }
    }

    /**
     * ピッチ設定
     * @param pitch
     */
    private void setSpeechPitch(float pitch) {
        if (mTts != null) {
            mTts.setPitch(pitch);
        }
    }

    /**
     * 言語設定
     * @param locale
     */
    private void setLanguage(Locale locale) {
        if (mTts != null) {
            mTts.setLanguage(locale);
        }
    }

    /**
     * 「今何時」の応答として喋る
     */
    private void speechWhatTime() {
        int size = mVoicePatternList.size();
        if (size < 1) {
            showSnackBar("応答データがありません.");
            return;
        }
        Random random = new Random();
        int index = random.nextInt(size);
        VoiceInfoRecord data = mVoicePatternList.get(index);
        speech(data.mRate, data.mPitch, data.mVoiceType, data.mVoiceText);
    }

    /**
     * 「g-shock」の応答として喋る
     */
    private void speechGShock() {
        String text = "Gショックとは、カシオ計算機株式会社が、1983年から販売している腕時計のブランドである。";
        String voice = "ma002";
        float rate = 1.0f;
        float pitch = 1.0f;
        speech(rate, pitch, voice, text);
    }

    /**
     * 「バルス」の応答として喋る
     */
    private void speechBulse() {
// 音データは削除
//        // キーン音を鳴らす
//        try {
//            mp.stop();
//            mp.prepare();
//            mp.start();
//        } catch (IOException e) {
//            ;
//        }
        // キーンの後で喋るするタイマー起動
        resultTimer.schedule(new TimerTask() {
            @Override
            public void run() {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        String text = "目があぁあぁあぁ！目があぁあぁあぁあああ！";
                        String voice = "ma005";
                        float rate = 0.8f;
                        float pitch = 1.8f;
                        speech(rate, pitch, voice, text);
                    }
                });
            }
        }, 1000);
    }

    /**
     * 「PPAP」の応答として喋る
     */
    private void speechPpap() {
//        String text = "ペン パイナッポーアッポーペン";
        String text = "ペン パイナッポーAndroidーペン";
        String voice = "ma002";
        float rate = 0.9f;
        float pitch = 0.8f;
        speech(rate, pitch, voice, text);
    }

    /**
     * 画面に入力された内容で喋るテスト
     */
    private void TestSpeech() {
        float rate = Float.valueOf(etRate.getText().toString());
        float pitch = Float.valueOf(etPitch.getText().toString());
        String text = etTExt.getText().toString();
        String item = (String)spinnerVoice.getSelectedItem();
        String voice = item.substring(item.indexOf(" ")+1);
        speech(rate, pitch, voice, text);
    }

    /**
     * リストの再生データをDBに登録
     * @return
     */
    private boolean registerVoiceData() {
        boolean result = true;
        if (mVoiceInfoDbHelper == null) {
            return false;
        }

        // 書き込み用DB取得
        SQLiteDatabase db = mVoiceInfoDbHelper.getWritableDatabase();

        // 先に全レコードを削除
        String sql = "DELETE FROM " + VoiceInfoDbHelper.DB_NAME_VOICE_TABLE;
        db.execSQL(sql);

        // リストの再生データを挿入
        for (int i = 0; i < mAdapterVoiceInfo.getCount(); i++) {
            VoiceInfoRecord record = mAdapterVoiceInfo.getItem(i);
            ContentValues values = record.getContentValues();
            long id = db.insert(VoiceInfoDbHelper.DB_NAME_VOICE_TABLE, null, values);
            // 挿入結果判定
            if (id < 0) {
                // 挿入失敗
                result = false;
                break;
            }
        }

        // データベースを閉じる
        db.close();

        return result;
    }

    /**
     * データベースから再生データを読み込みリストに表示
     * @return
     */
    private boolean readVoiceData() {
        boolean result = false;
        if (mVoiceInfoDbHelper == null) {
            return false;
        }

        // 読み込み用DB取得
        SQLiteDatabase db = mVoiceInfoDbHelper.getReadableDatabase();

        // 全レコードを取得
        String sql = "SELECT * FROM " + VoiceInfoDbHelper.DB_NAME_VOICE_TABLE
                     + " ORDER BY " + VoiceInfoDbHelper.DB_ITEM_ID + " ASC";
        Cursor cursor = db.rawQuery(sql, null);
        // 検索結果をチェックする
        if(null != cursor) {
            // 先頭レコードへ移動
            cursor.moveToFirst();
            // リストの全データを削除
            mAdapterVoiceInfo.clear();
            // 再生用リストの全データを削除
            mVoicePatternList.clear();
            // データ数チェック
            for (int i = 0; i < cursor.getCount(); i++) {
                VoiceInfoRecord record = new VoiceInfoRecord();
                // 音声タイプ
                String voice = cursor.getString(cursor.getColumnIndex(VoiceInfoDbHelper.DB_ITEM_VOICE_TYPE));
                // rate
                float rate = cursor.getFloat(cursor.getColumnIndex(VoiceInfoDbHelper.DB_ITEM_VOICE_RATE));
                // pitch
                float pitch = cursor.getFloat(cursor.getColumnIndex(VoiceInfoDbHelper.DB_ITEM_VOICE_PITCH));
                // テキスト
                String  text = cursor.getString(cursor.getColumnIndex(VoiceInfoDbHelper.DB_ITEM_VOICE_TEXT));
                // 使用フラグ
                boolean use = false;
                if (cursor.getInt(cursor.getColumnIndex(VoiceInfoDbHelper.DB_ITEM_VOICE_USE_FLAG)) == 1) {
                    use = true;
                }

                // 音声リストに音声情報を追加
                addListVoiceInfo(rate, pitch, voice, text, use);
                //Log.d("VoiceData", rate + "," + pitch + "," + voice + "," + text + "," + use);

                // 再生リストに音声情報を追加
                if (use) {
                    VoiceInfoRecord data = new VoiceInfoRecord();
                    data.mRate = rate;
                    data.mPitch = pitch;
                    data.mVoiceType = voice;
                    data.mVoiceText = text;
                    mVoicePatternList.add(data);
                }
                // 次のレコードへ移動
                cursor.moveToNext();
            }
            cursor.close();			// カーソルを閉じる
            result = true;
        }

        // データベースを閉じる
        db.close();

        return result;
    }

    //----------------------------------------
    // AdapterView.OnItemClickListener
    //----------------------------------------
    /**
     * onItemClick(ListViewアイテムクリック)
     * @param parent
     * @param view
     * @param position
     * @param id
     */
    @Override
    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
        // クリックした項目のチェックがONならテキストを再生
        VoiceInfoRecord item = mAdapterVoiceInfo.getItem(position);
        CheckBox check = (CheckBox)view.findViewById(R.id.checkUseFlag);
        item.mUseFlag = check.isChecked();
        if (item.mUseFlag) {
            speech(item.mRate, item.mPitch, item.mVoiceType, item.mVoiceText);
        }
    }

    /**
     * onItemLongClick(ListViewアイテムのロングクリック)
     * @param parent
     * @param view
     * @param position
     * @param id
     * @return
     */
    @Override
    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
        // ロングクリックした項目を削除する
        mAdapterVoiceInfo.remove(mAdapterVoiceInfo.getItem(position));
        return false;
    }

    //----------------------------------------
    // GoogleApiClient.ConnectionCallbacks
    //----------------------------------------
    @Override
    public void onConnected(Bundle bundle) {
        Wearable.MessageApi.addListener(mGoogleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int i) {

    }

    //----------------------------------------
    // GoogleApiClient.OnConnectionFailedListener
    //----------------------------------------
    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }

    //----------------------------------------
    // MessageApi.MessageListener
    //----------------------------------------
    @Override
    public void onMessageReceived(MessageEvent messageEvent) {
        if (messageEvent.getPath().equals("/keyword")) {
            String str = new String(messageEvent.getData());
            Log.d(TAG_WHAT_TIME, str);

            if (str.equals("今何時")) {
                speechWhatTime();
            } else if (str.equals("g-shock")) {
                speechGShock();
            } else if (str.equals("バルス")) {
                speechBulse();
            } else if (str.equals("PPAP")) {
                speechPpap();
            }
        }
    }

}
