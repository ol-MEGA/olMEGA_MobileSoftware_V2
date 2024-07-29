package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.content.res.ColorStateList;
import android.view.Gravity;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;

import androidx.core.content.ContextCompat;
import androidx.core.widget.CompoundButtonCompat;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StringAndInteger;
import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeRadio extends AnswerType {

    private static String LOG_STRING = "AnswerTypeRadio";
    private final RadioGroup mRadioGroup;
    /*private final AnswerLayout mParent;
    private final Context mContext;
    private final Questionnaire mQuestionnaire;
    private final int mQuestionId;
    private final List<StringAndInteger> mListOfAnswers;*/
    private int mDefault = -1;


    public AnswerTypeRadio(Context context, Questionnaire questionnaire, AnswerLayout qParent, int nQuestionId) {

        super(context, questionnaire, qParent, nQuestionId);

        // Answer Buttons of type "radio" are grouped and handled together
        mRadioGroup = new RadioGroup(mContext);
        mRadioGroup.setOrientation(RadioGroup.VERTICAL);
    }

    public void addAnswer(int nAnswerId, String sAnswer, boolean isDefault) {
        mListOfAnswers.add(new StringAndInteger(sAnswer, nAnswerId));
        if (isDefault) {
            mDefault = mListOfAnswers.size() - 1;
        }
    }

    public void buildView() {

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {

            RadioButton button = new RadioButton(mContext);
            button.setId(mListOfAnswers.get(iAnswer).getId());
            button.setText(mListOfAnswers.get(iAnswer).getText());
            button.setTextSize(mContext.getResources().getDimension(R.dimen.textSizeAnswer));
            button.setChecked(false);
            button.setGravity(Gravity.CENTER_VERTICAL);
            button.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            button.setBackgroundColor(ContextCompat.getColor(mContext, R.color.BackgroundColor));
            int states[][] = {{android.R.attr.state_checked}, {}};
            int colors[] = {ContextCompat.getColor(mContext, R.color.JadeRed),
                    ContextCompat.getColor(mContext, R.color.JadeRed)};
            CompoundButtonCompat.setButtonTintList(button, new ColorStateList(states, colors));
            button.setMinHeight((int) mContext.getResources().getDimension(R.dimen.radioMinHeight));
            button.setPadding(24, 24, 24, 24);

            if (iAnswer == mDefault) {
                button.setChecked(true);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, mListOfAnswers.get(mDefault).getId());
            }

            // Parameters of Answer Button
            LinearLayout.LayoutParams answerParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);

                if (mListOfAnswers.get(iAnswer).getId() == 66666) {
                    button.setEnabled(false);
                    button.setVisibility(View.INVISIBLE);
                }

                mRadioGroup.addView(button, answerParams);
        }
        mParent.layoutAnswer.addView(mRadioGroup);
    }

    public void addClickListener() {

        mRadioGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // In Case of Radio Buttons checking one means un-checking all other Elements
                // Therefore onClickListening must be handled on Group Level
                // listOfRadioIds contains all Ids of current Radio Group
                mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, checkedId);
                mRadioGroup.check(checkedId);

                // Toggle Visibility of suited/unsuited frames
                mQuestionnaire.checkVisibility();
            }
        });
    }
}
