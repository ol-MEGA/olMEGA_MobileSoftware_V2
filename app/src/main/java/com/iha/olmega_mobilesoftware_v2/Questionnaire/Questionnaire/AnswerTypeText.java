package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.provider.Settings.Secure;
import android.text.InputType;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.QuestionnaireActivity;
import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeText extends AnswerType {

    private final String LOG_STRING = "AnswerTypeText";
    public EditText mAnswerText;
    public LinearLayout.LayoutParams answerParams;
    private Button mButtonOkay;
    private boolean isSystem = false;
    private boolean isImmersive = false;

    public AnswerTypeText(Context context, Questionnaire questionnaire, AnswerLayout qParent,
                          int nQuestionId, boolean isImmersive) {

        super(context, questionnaire, qParent, nQuestionId);
        this.isImmersive = isImmersive;
    }

    public void addQuestion(String sAnswer) {
        switch (sAnswer) {
            case "$device.id":
                isSystem = true;
                mQuestionnaire.addTextToEvaluationLst(mQuestionId, generateDeviceId());
                break;
        }
    }

    public void buildView() {

        if (!isSystem) {
            mAnswerText = new EditText(mContext);
            mAnswerText.setRawInputType(InputType.TYPE_CLASS_TEXT);
            mAnswerText.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
            mAnswerText.setGravity(Gravity.START);
            mAnswerText.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            mAnswerText.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
            mAnswerText.setHint(R.string.hintTextAnswer);
            mAnswerText.setHintTextColor(ContextCompat.getColor(mContext, R.color.JadeGray));

            // Parameters of Answer Button Layout
            answerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.MATCH_PARENT);

            answerParams.setMargins(
                    (int) mContext.getResources().getDimension(R.dimen.answerTextMargin_Left),
                    (int) mContext.getResources().getDimension(R.dimen.answerTextMargin_Top),
                    (int) mContext.getResources().getDimension(R.dimen.answerTextMargin_Right),
                    (int) mContext.getResources().getDimension(R.dimen.answerTextMargin_Bottom));

            mButtonOkay = new Button(mContext);
            mButtonOkay.setText(R.string.buttonTextOkay);
            mButtonOkay.setScaleX(1.5f);
            mButtonOkay.setScaleY(1.5f);
            mButtonOkay.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            mButtonOkay.setBackground(ContextCompat.getDrawable(mContext, R.drawable.button));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

            buttonParams.topMargin = 96;
            buttonParams.bottomMargin = 48;

            mAnswerText.isFocusableInTouchMode();

            mParent.layoutAnswer.addView(mAnswerText, answerParams);
            mParent.layoutAnswer.addView(mButtonOkay, buttonParams);
        }
    }

    public void addClickListener() {

        if (!isSystem) {
            mButtonOkay.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {

                    // Check if no view has focus, then hide soft keyboard:
                    View view = mAnswerText;
                    if (view != null) {
                        InputMethodManager imm = (InputMethodManager) mAnswerText.getContext().
                                getSystemService(Context.INPUT_METHOD_SERVICE);
                        imm.hideSoftInputFromWindow(mAnswerText.getWindowToken(), 0);
                        mAnswerText.setCursorVisible(false);
                    }

                    String text = mAnswerText.getText().toString();
                    if (text.length() != 0) {
                        mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                        mQuestionnaire.addTextToEvaluationLst(mQuestionId, text);
                    } else {
                        Log.e(LOG_STRING, "No text was entered.");
                    }

                    ((QuestionnaireActivity) mContext).hideSystemUI(isImmersive);
                    ((QuestionnaireActivity) mContext).incrementPage();
                }
            });
        }
    }

    private String generateDeviceId() {
        return Secure.getString(mContext.getContentResolver(), Secure.ANDROID_ID);
    }
}
