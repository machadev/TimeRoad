package com.machadev.android.timeroad;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.TextView;

import java.util.List;

/**
 * 音声情報リスト用Adapter
 */
public class VoiceInfoAdapter extends ArrayAdapter<VoiceInfoRecord> {

    private LayoutInflater mInflater;
    private Context mContext;

    static class ViewHolder {
        TextView tvNumber;          // 番号
        TextView tvVoiceType;       // 音声タイプ
        TextView tvRate;            // rate
        TextView tvPitch;           // pitch
        TextView tvVoiceText;       // 音声テキスト
        CheckBox cbUseFlag;         // 使用フラグ
    }

    /**
     * コンストラクタ
     * @param context
     * @param item
     */
    public VoiceInfoAdapter(Context context, List<VoiceInfoRecord> item) {
        super(context, 0, item);
        mContext = context;
        mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
    }

    /**
     * getView
     * @param position
     * @param convertView
     * @param parent
     * @return
     */
    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        ViewHolder holder;
        if (convertView == null) {
            convertView = mInflater.inflate(R.layout.list_row_voice_info, parent, false);
            holder = new ViewHolder();
            holder.tvNumber = (TextView) convertView.findViewById(R.id.txtRowNumber);
            holder.tvVoiceType = (TextView) convertView.findViewById(R.id.txtRowVoiceType);
            holder.tvRate = (TextView) convertView.findViewById(R.id.txtRowRate);
            holder.tvPitch = (TextView) convertView.findViewById(R.id.txtRowPitch);
            holder.tvVoiceText = (TextView) convertView.findViewById(R.id.txtRowVoiceText);
            holder.cbUseFlag = (CheckBox) convertView.findViewById(R.id.checkUseFlag);

            convertView.setTag(holder);
        } else {
            holder = (ViewHolder) convertView.getTag();
        }

        // 表示データ取得
        VoiceInfoRecord item = (VoiceInfoRecord) getItem(position);
        final int p = position;
        holder.cbUseFlag.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                VoiceInfoRecord data = (VoiceInfoRecord) getItem(p);
                data.mUseFlag = isChecked;
                notifyDataSetChanged();
            }
        });

        // 番号
        holder.tvNumber.setText(String.format("%3d", item.mNumber));
        // 音声タイプ
        holder.tvVoiceType.setText(item.mVoiceType);
        // rate
        holder.tvRate.setText(String.format("%.2f", item.mRate));
        // pitch
        holder.tvPitch.setText(String.format("%.2f", item.mPitch));
        // 音声テキスト
        holder.tvVoiceText.setText(item.mVoiceText);
        // 使用フラグ
        holder.cbUseFlag.setChecked(item.mUseFlag);

        return convertView;
    }
}
