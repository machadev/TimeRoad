package com.machadev.android.timeroad;

import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import android.os.Vibrator;
import android.speech.RecognizerIntent;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.BoxInsetLayout;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextClock;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.MessageApi;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.NodeApi;
import com.google.android.gms.wearable.Wearable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends WearableActivity
        implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private static final SimpleDateFormat AMBIENT_DATE_FORMAT =
            new SimpleDateFormat("HH:mm", Locale.US);
    private final String TAG_WHAT_TIME = "What time wear";
    private int REQUEST_CODE_WHAT_TIME = 100;
    private final String KEYWORD_WHAT_TIME = "今何時";
    private final String KEYWORD_G_SHOCK = "g-shock";
    private final String KEYWORD_BULSE = "バルス";
    private final String KEYWORD_PPAP = "PPAP";

    private BoxInsetLayout mContainerView;
    private TextClock mClockView;
    private Vibrator mVibrator;

    private GoogleApiClient mGoogleApiClient;
    private LinearLayout mLayout;
    private int mCountTouch = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setAmbientEnabled();

        mContainerView = (BoxInsetLayout) findViewById(R.id.container);
        mClockView = (TextClock) findViewById(R.id.textClock);
        mLayout = (LinearLayout) findViewById(R.id.layoutBk);

        mVibrator = (Vibrator)getSystemService(VIBRATOR_SERVICE);

        mGoogleApiClient = new GoogleApiClient
                .Builder(this)
                .addConnectionCallbacks(this)
                .addApi(Wearable.API)
                .build();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (mGoogleApiClient != null) {
            mGoogleApiClient.connect();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
        if (mGoogleApiClient != null && mGoogleApiClient.isConnected()) {
            mGoogleApiClient.disconnect();
        }
    }

    @Override
    public boolean onTouchEvent(MotionEvent event) {
//        mVibrator.vibrate(1000);
        return super.onTouchEvent(event);
    }

    @Override
    public boolean dispatchTouchEvent(MotionEvent ev) {
//        mVibrator.vibrate(1000);
//        long[] pattern = {
//                200, 300, 200, 300, 200, 300, 200, 300, 200, 300,
//                200, 300, 200, 300, 200, 300
//        };
//        mVibrator.vibrate(pattern, -1);

        // 音声入力画面を開く
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "今何時？");
        startActivityForResult(intent, REQUEST_CODE_WHAT_TIME);

        mCountTouch++;
        if (mCountTouch > 5) {
            mCountTouch = 1;
        }
        Resources res = getResources();
        int id = res.getIdentifier(String.format("bk_unagi%d", mCountTouch), "drawable", getPackageName());
        mLayout.setBackgroundResource(id);

        return super.dispatchTouchEvent(ev);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (resultCode != RESULT_OK) {
            // OK以外は処理しない
            return;
        }
        if (requestCode != REQUEST_CODE_WHAT_TIME) {
            // 今何時のリクエスト以外は処理しない
            return;
        }
        // 結果の文字列リスト取得
        ArrayList<String> results = data.getStringArrayListExtra(
                RecognizerIntent.EXTRA_RESULTS
        );
        // 文字列リストからキーワードを検索
        String keyword = "";
        for (int i = 0; i < results.size(); i++) {
            String str = results.get(i);
            // 今何時？
            if (str.equals("今何時")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.equals("what time")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.equals("What time")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.equals("掘った芋")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.equals("ほった芋")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.contains("What time")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }
            if (str.contains("What time")) {
                keyword = KEYWORD_WHAT_TIME;
                break;
            }

            // Gショック
            if (str.equals("Gショック")) {
                keyword = KEYWORD_G_SHOCK;
                break;
            }
            if (str.equals("g-shock")) {
                keyword = KEYWORD_G_SHOCK;
                break;
            }

            // バルス
            if (str.equals("バルス")) {
                keyword = KEYWORD_BULSE;
                break;
            }

            // PPAP
            if (str.equals("PPAP")) {
                keyword = KEYWORD_PPAP;
                break;
            }
            if (str.equals("ppap")) {
                keyword = KEYWORD_PPAP;
                break;
            }
        }
        if (!keyword.isEmpty()) {
            sendMessage(keyword);
            Calendar cal = Calendar.getInstance();
            int hour = cal.get(Calendar.HOUR);
            long[] pattern = new long[24];
            for (int i = 0; i < 24; ) {
                if (i < hour * 2) {
                    pattern[i + 0] = 100;
                    pattern[i + 1] = 200;
                } else {
                    pattern[i + 0] = 0;
                    pattern[i + 1] = 0;
                }
                i += 2;
            }
            mVibrator.vibrate(pattern, -1);
        }
        Log.d(TAG_WHAT_TIME, "keyword:" + keyword);

        super.onActivityResult(requestCode, resultCode, data);
    }

    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        updateDisplay();
    }

    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        updateDisplay();
    }

    @Override
    public void onExitAmbient() {
        updateDisplay();
        super.onExitAmbient();
    }

    private void updateDisplay() {
//        if (isAmbient()) {
//            mContainerView.setBackgroundColor(getResources().getColor(android.R.color.black));
//            mClockView.setTextColor(getResources().getColor(android.R.color.white));
//        } else {
//            mContainerView.setBackground(null);
//            mClockView.setTextColor(getResources().getColor(android.R.color.black));
//        }
    }

    //----------------------------------------
    // GoogleApiClient.ConnectionCallbacks
    //----------------------------------------
    @Override
    public void onConnected(Bundle bundle) {

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

    /**
     * Wear->端末へメッセージ送信
     * @param keyword
     */
    private void sendMessage(final String keyword) {
        final PendingResult<NodeApi.GetConnectedNodesResult> nodes =
                Wearable.NodeApi.getConnectedNodes(mGoogleApiClient);
        nodes.setResultCallback(new ResultCallback<NodeApi.GetConnectedNodesResult>() {
            @Override
            public void onResult(NodeApi.GetConnectedNodesResult result) {
                for (Node node : result.getNodes()) {
                    final byte[] bs = keyword.getBytes();
                    //final byte[] bs = (keyword + " " + node.getId()).getBytes();
                    PendingResult<MessageApi.SendMessageResult> messageResult =
                            Wearable.MessageApi.sendMessage(mGoogleApiClient,
                                    node.getId(), "/keyword", bs);
                    messageResult.setResultCallback(new ResultCallback<MessageApi.SendMessageResult>() {
                        @Override
                        public void onResult(MessageApi.SendMessageResult result) {
                            Status status = result.getStatus();
                            Log.d(TAG_WHAT_TIME, status.toString());
                        }
                    });
                }
            }
        });
    }
}
