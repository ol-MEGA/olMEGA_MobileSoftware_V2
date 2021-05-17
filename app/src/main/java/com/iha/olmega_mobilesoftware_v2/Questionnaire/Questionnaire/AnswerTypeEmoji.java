package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;

import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.Units;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StringAndInteger;
import com.iha.olmega_mobilesoftware_v2.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerTypeEmoji extends AnswerType {

    //private final Context mContext;
    //private final AnswerLayout mParent;
    //private final List<StringAndInteger> mListOfAnswers;
    private final List<Integer> mListOfIds;
    private final int[] drawables = new int[5];
    private final int[] drawables_pressed = new int[5];
    //private final Questionnaire mQuestionnaire;
    //private final int mQuestionId;
    private String LOG_STRING = "AnswerTypeEmoji";
    private int mDefault = -1;
    private int mViewPagerHeight = 0;
    private boolean isImmersive = false;

    public AnswerTypeEmoji(Context context, Questionnaire questionnaire,
                           AnswerLayout qParent, int questionId, boolean immersive) {


        super(context, questionnaire, qParent, questionId);
        //mContext = context;
        //mParent = qParent;
        //mQuestionnaire = questionnaire;
        //mListOfAnswers = new ArrayList<>();
        mListOfIds = new ArrayList<>();
        //mQuestionId = questionId;
        isImmersive = immersive;

        drawables[0] = R.drawable.em1of5;
        drawables[1] = R.drawable.em2of5;
        drawables[2] = R.drawable.em3of5;
        drawables[3] = R.drawable.em4of5;
        drawables[4] = R.drawable.em5of5;

        drawables_pressed[0] = R.drawable.em1of5_active;
        drawables_pressed[1] = R.drawable.em2of5_active;
        drawables_pressed[2] = R.drawable.em3of5_active;
        drawables_pressed[3] = R.drawable.em4of5_active;
        drawables_pressed[4] = R.drawable.em5of5_active;
    }

    public boolean addAnswer(int nId, String sAnswer, boolean isDefault) {
        mListOfAnswers.add(new StringAndInteger(sAnswer, nId));
        mListOfIds.add(nId);
        if (isDefault) {
            mDefault = mListOfAnswers.size() - 1;
        }
        return true;
    }

    public void buildView() {

        int usableHeight = (new Units(mContext)).getUsableSliderHeight(isImmersive);
        int numEmojis = mListOfAnswers.size();
        // Make size of emojis adaptive
        int emojiSize = (int) (usableHeight / (1.2f * numEmojis));

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {

            final Button answerButton = new Button(mContext);
            answerButton.setLayoutParams(new LinearLayout.LayoutParams(
                    emojiSize,
                    emojiSize,
                    1.0f));

            String sAnswer = mListOfAnswers.get(iAnswer).getText();
            switch (sAnswer) {
                case "emoji_happy2":
                    answerButton.setBackground(ContextCompat.getDrawable(mContext, drawables[0]));
                    answerButton.setTag(0);
                    break;
                case "emoji_happy1":
                    answerButton.setBackground(ContextCompat.getDrawable(mContext, drawables[1]));
                    answerButton.setTag(1);
                    break;
                case "emoji_neutral":
                    answerButton.setBackground(ContextCompat.getDrawable(mContext, drawables[2]));
                    answerButton.setTag(2);
                    break;
                case "emoji_sad1":
                    answerButton.setBackground(ContextCompat.getDrawable(mContext, drawables[3]));
                    answerButton.setTag(3);
                    break;
                case "emoji_sad2":
                    answerButton.setBackground(ContextCompat.getDrawable(mContext, drawables[4]));
                    answerButton.setTag(4);
                    break;
                default:
                    break;
            }

            if (iAnswer == mDefault) {
                setChecked(true, answerButton);
                mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                mQuestionnaire.addIdToEvaluationList(mQuestionId, mListOfAnswers.get(iAnswer).getId());
            } else {
                setChecked(false, answerButton);
            }
            answerButton.setId(mListOfAnswers.get(iAnswer).getId());

            mParent.layoutAnswer.addView(answerButton);

            // Placeholder View because padding has no effect
            View placeHolder = new View(mContext);
            placeHolder.setBackgroundColor(ContextCompat.getColor(
                    mContext, R.color.BackgroundColor));
            placeHolder.setLayoutParams(new LinearLayout.LayoutParams(
                    (int) (0.16f * emojiSize),
                    (int) (0.16f * emojiSize),
                    1.0f
            ));

            // The lowest placeholder is unnecessary
            if (iAnswer<mListOfAnswers.size()-1) {
                mParent.layoutAnswer.addView(placeHolder);
            }
        }
    }

    public void addClickListener() {

        for (int iAnswer = 0; iAnswer < mListOfAnswers.size(); iAnswer++) {
            final Button button = (Button) mParent.layoutAnswer.findViewById(
                    mListOfAnswers.get(iAnswer).getId());
            final int currentAnswer = iAnswer;
            button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    for (int iButton = 0; iButton < mListOfAnswers.size(); iButton++) {
                        final Button button = (Button) mParent.layoutAnswer.findViewById(
                                mListOfAnswers.get(iButton).getId());
                        if (iButton == currentAnswer) {
                            setChecked(true, button);
                        } else {
                            setChecked(false, button);
                        }
                    }
                    mQuestionnaire.removeQuestionIdFromEvaluationList(mQuestionId);
                    mQuestionnaire.addIdToEvaluationList(mQuestionId, mListOfAnswers.get(currentAnswer).getId());
                    mQuestionnaire.checkVisibility();
                }
            });
        }
    }

    private void setChecked(boolean isChecked, Button answerButton) {
        if (isChecked) {
            answerButton.setBackground(ContextCompat.getDrawable(mContext,
                    drawables_pressed[(int) answerButton.getTag()]));
        } else {
            answerButton.setBackground(ContextCompat.getDrawable(mContext,
                    drawables[(int) answerButton.getTag()]));
        }
    }
}

/*
    public void toggleChecked(Button answerButton) {
        if (answerButton.isPressed()) {
            answerButton.setBackground(ContextCompat.getDrawable(mContext,
                    drawables_pressed[(int) answerButton.getTag()]));
        } else {
            answerButton.setBackground(ContextCompat.getDrawable(mContext,
                    drawables[(int) answerButton.getTag()]));
        }
    }
    */