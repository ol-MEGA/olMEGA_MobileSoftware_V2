package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.view.Gravity;
import android.widget.LinearLayout;
import android.widget.ScrollView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.R;

/**
 * Created by ulrikkowalk on 17.02.17.
 */

public class AnswerLayout extends AppCompatActivity {

    final LinearLayout layoutAnswer;
    final ScrollView scrollContent;
    private final Context mContext;

    public AnswerLayout(Context context) {
        mContext = context;
        // Main Layout has to be incorporated in ScrollView for Overflow Handling
        scrollContent = new ScrollView(context);
        scrollContent.setBackgroundColor(ContextCompat.getColor(context, R.color.BackgroundColor));
        scrollContent.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));
        scrollContent.setId(0);

        // Main Layout - Right now Framework carrying ONE Question
        layoutAnswer = new LinearLayout(context);

        layoutAnswer.setPadding((int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Left),
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Top),
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Right),
                (int) mContext.getResources().getDimension(R.dimen.answerLayoutPadding_Bottom));
        layoutAnswer.setOrientation(LinearLayout.VERTICAL);
        layoutAnswer.setBackgroundColor(ContextCompat.getColor(context, R.color.BackgroundColor));
        layoutAnswer.setGravity(Gravity.CENTER_HORIZONTAL);
        layoutAnswer.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT));

        // Linear Layout is Child to ScrollView (must always be)
        scrollContent.addView(layoutAnswer);

    }
}
