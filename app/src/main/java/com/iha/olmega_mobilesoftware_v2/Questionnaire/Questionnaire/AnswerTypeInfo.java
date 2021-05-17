package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.graphics.Typeface;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeInfo extends AppCompatActivity {

    private Button mAnswerButton;
    private LinearLayout.LayoutParams answerParams;
    private AnswerLayout parent;
    private Context mContext;
    private Questionnaire mQuestionnaire;

    public AnswerTypeInfo(Context context, Questionnaire questionnaire, AnswerLayout qParent) {

        mContext = context;
        mQuestionnaire = questionnaire;
        parent = qParent;

        mAnswerButton = new Button(context);
        mAnswerButton.setText(R.string.buttonTextFinish);
        mAnswerButton.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
        mAnswerButton.setGravity(Gravity.CENTER_HORIZONTAL);
        mAnswerButton.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
        mAnswerButton.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
        mAnswerButton.setAllCaps(false);
        mAnswerButton.setTypeface(null, Typeface.NORMAL);
        mAnswerButton.setBackgroundResource(R.drawable.button);

        // Parameters of Answer Button
        answerParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);

        answerParams.setMargins(
                (int) mContext.getResources().getDimension(R.dimen.answerFinishMargin_Left),
                (int) mContext.getResources().getDimension(R.dimen.answerFinishMargin_Top),
                (int) mContext.getResources().getDimension(R.dimen.answerFinishMargin_Right),
                (int) mContext.getResources().getDimension(R.dimen.answerFinishMargin_Bottom));
    }

    public boolean addAnswer() {
        parent.layoutAnswer.addView(mAnswerButton, answerParams);
        return true;
    }

    public void addClickListener() {
        mAnswerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mQuestionnaire.finaliseEvaluation();
            }
        });
    }
}
