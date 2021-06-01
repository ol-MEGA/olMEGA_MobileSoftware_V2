package com.iha.olmega_mobilesoftware_v2;

import android.content.res.ColorStateList;
import android.os.Build;
import android.os.Bundle;
import android.text.Html;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

public class Help extends AppCompatActivity {
    private String TAG = this.getClass().getSimpleName();
    private LayoutInflater inflater;
    private RadioButton rb1, rb2;
    private CheckBox cb1, cb2, cb3;
    private TextView tv1, tv2, tv3;
    private Button bt1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        inflater = LayoutInflater.from(getApplicationContext());
        setContentView(generateView());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            if (getWindow().getInsetsController() != null) {
                getWindow().getInsetsController().hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                getWindow().getInsetsController().setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    public LinearLayout generateView() {
        LinearLayout view = (LinearLayout) inflater.inflate(R.layout.layout_help, null);

        rb1 = (RadioButton) view.findViewById(R.id.radioButton);
        rb2 = (RadioButton) view.findViewById(R.id.radioButton2);

        cb1 = (CheckBox) view.findViewById(R.id.checkBox);
        cb2 = (CheckBox) view.findViewById(R.id.checkBox2);
        cb3 = (CheckBox) view.findViewById(R.id.checkBox3);

        tv1 = (TextView) view.findViewById(R.id.textView);
        tv2 = (TextView) view.findViewById(R.id.textView2);
        tv3 = (TextView) view.findViewById(R.id.textView3);

        bt1 = (Button) view.findViewById(R.id.button);

        tv2.setText(R.string.help_hint);

        setRadioPrefs(rb1);
        setRadioPrefs(rb2);
        setCheckPrefs(cb1);
        setCheckPrefs(cb2);
        setCheckPrefs(cb3);
        setHeadLinePrefs(tv1);

        setTextPrefs(tv2);
        setTextPrefs(tv3);
        setButtonPrefs(bt1);

        bt1.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Help.this.finish();
            }
        });

        return view;
    }

    private void setRadioPrefs(RadioButton radiobutton) {
        radiobutton.setTextSize(getApplicationContext().getResources().getDimension(R.dimen.textSizeAnswerHelp));
        radiobutton.setGravity(Gravity.CENTER_VERTICAL);
        radiobutton.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.TextColor));
        radiobutton.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.BackgroundColor));
        int states[][] = {{android.R.attr.state_checked}, {}};
        int colors[] = {ContextCompat.getColor(getApplicationContext(), R.color.JadeRed),
                ContextCompat.getColor(getApplicationContext(), R.color.JadeRed)};
        CompoundButtonCompat.setButtonTintList(radiobutton, new ColorStateList(states, colors));
        radiobutton.setMinHeight((int) getApplicationContext().getResources().getDimension(R.dimen.radioMinHeight));
        radiobutton.setPadding(24, 24, 24, 24);
    }

    private void setCheckPrefs(CheckBox checkBox) {
        checkBox.setTextSize(getApplicationContext().getResources().getDimension(R.dimen.textSizeAnswerHelp));
        checkBox.setChecked(false);
        checkBox.setGravity(Gravity.CENTER_VERTICAL);
        checkBox.setPadding(24, 24, 24, 24);
        checkBox.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.TextColor));
        checkBox.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.BackgroundColor));
        int states[][] = {{android.R.attr.state_checked}, {}};
        int colors[] = {ContextCompat.getColor(getApplicationContext(), R.color.JadeRed),
                ContextCompat.getColor(getApplicationContext(), R.color.JadeRed)};
        CompoundButtonCompat.setButtonTintList(checkBox, new ColorStateList(states, colors));
    }

    private void setHeadLinePrefs(TextView textView) {
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.TextColor));
        textView.setBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.lighterGray));
        textView.setTextSize(getApplicationContext().getResources().getDimension(R.dimen.textSizeQuestion));
    }

    private void setTextPrefs(TextView textView) {
        textView.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.TextColor));
        textView.setTextSize(getApplicationContext().getResources().getDimension(R.dimen.textSizeAnswerHelp));
    }

    private void setTextHTML(TextView textView) {
        String string = textView.getText().toString();
        textView.setText(Html.fromHtml(string));
    }

    private void setButtonPrefs(Button button) {
        button.setScaleX(1.2f);
        button.setScaleY(1.2f);
        button.setTextColor(ContextCompat.getColor(getApplicationContext(), R.color.TextColor));
        button.setBackground(ContextCompat.getDrawable(getApplicationContext(), R.drawable.button));
    }
}
