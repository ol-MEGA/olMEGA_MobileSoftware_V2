package com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire;

import android.content.Context;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.core.content.ContextCompat;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.Core.Units;
import com.iha.olmega_mobilesoftware_v2.R;


/**
 * Created by ulrikkowalk on 17.02.17.
 */


public class AnswerTypeWebsite extends AnswerType {

    private static String LOG_STRING = "AnswerTypeWebsite";
    private String url;
    private String clientID;
    private Button button;
    private LayoutInflater inflater;
    private Context context;


    public AnswerTypeWebsite(Context context, Questionnaire questionnaire, AnswerLayout qParent,
                             int nQuestionId, boolean isImmersive, String clientID) {

        super(context, questionnaire, qParent, nQuestionId);

        this.context = context;
        this.clientID = clientID;
        this.inflater = LayoutInflater.from(context);

    }

    public void addAnswer(String url) {
        if (url.contains("$clientID$")) {
            this.url = url.replace("$clientID$", this.clientID);
        } else {
            this.url = url;
        }
    }

    public void buildView() {

        if (isNetworkAvailable()) {

            WebView webView = new WebView(mContext);
            //webView.setLayoutParams(new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 70));
            webView.setWebViewClient(new WebViewClient());
            webView.getSettings().setJavaScriptEnabled(true);
            webView.getSettings().setUseWideViewPort(true);
            webView.getSettings().setLoadWithOverviewMode(true);

            webView.loadUrl(this.url);
            //webView.setInitialScale(70);

            Log.e(LOG, "URL: " + this.url);

            this.button = new Button(mContext);

            button.setText(R.string.buttonTextOkay);
            button.setTextColor(ContextCompat.getColor(mContext, R.color.TextColor));
            button.setBackground(ContextCompat.getDrawable(mContext, R.drawable.button));
            LinearLayout.LayoutParams buttonParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );
            buttonParams.topMargin = 96;
            buttonParams.bottomMargin = 48;

            LinearLayout.LayoutParams webViewParams = new LinearLayout.LayoutParams(
                    Units.getScreenWidth(),
                    Units.getScreenHeight() - buttonParams.bottomMargin - buttonParams.topMargin - 500
            );

            mParent.layoutAnswer.setBackgroundColor(ContextCompat.getColor(mContext, R.color.WebGray));
            mParent.scrollContent.setBackgroundColor(ContextCompat.getColor(mContext, R.color.WebGray));

            mParent.layoutAnswer.addView(webView, webViewParams);
            mParent.layoutAnswer.addView(button, buttonParams);




        } else {

            TextView info = new TextView(mContext);
            info.setText(R.string.noInternet);
            info.setTextSize(20.0f);
            info.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            LinearLayout.LayoutParams infoParams = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT);
            infoParams.topMargin = 150;

            mParent.layoutAnswer.addView(info, infoParams);

            Log.e(LOG, "NO NETWORK AVAILABLE");

        }

    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();
    }

    public void addClickListener() {

        if (isNetworkAvailable()) {
            this.button.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mQuestionnaire.moveForward();
                    //((MainActivity) context).nextQuestion();
                }
            });
        }

    }

    public boolean isNetworkAvailable()
    {
        final ConnectivityManager connectivityManager = ((ConnectivityManager) this.context.getSystemService(Context.CONNECTIVITY_SERVICE));
        return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
    }
}
