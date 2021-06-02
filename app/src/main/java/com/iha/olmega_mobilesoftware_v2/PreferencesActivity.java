package com.iha.olmega_mobilesoftware_v2;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Application;
import android.app.DownloadManager;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.database.Cursor;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.util.Log;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.AppUpdaterUtils;
import com.github.javiersantos.appupdater.DisableClickListener;
import com.github.javiersantos.appupdater.UpdateClickListener;
import com.github.javiersantos.appupdater.enums.AppUpdaterError;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.github.javiersantos.appupdater.objects.Update;
import com.iha.olmega_mobilesoftware_v2.Core.FileIO;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.Objects;

import javax.xml.parsers.DocumentBuilderFactory;

public class PreferencesActivity extends PreferenceActivity {
    private String TAG = this.getClass().getSimpleName();
    public boolean isDeviceOwner = false;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        isDeviceOwner = getIntent().getBooleanExtra("isDeviceOwner", false);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Preferences()).commit();
    }

    // https://github.com/javiersantos/AppUpdater/issues/193#issuecomment-721537960
    //You write your code here when the download finished

    private void DownloadUpdate(String url) {
        DownloadManager.Request request = new DownloadManager.Request(Uri.parse(url));
        request.setTitle("Download Update");
        request.allowScanningByMediaScanner();
        //request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED);
        request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_HIDDEN);
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, url.substring(url.lastIndexOf('/') + 1, url.lastIndexOf('?')));
        DownloadManager manager = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
        registerReceiver(onDownloadComplete, new IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE));
        manager.enqueue(request);
        Toast.makeText(this, "Download started. Please wait...", Toast.LENGTH_LONG).show();
    }

    BroadcastReceiver onDownloadComplete = new BroadcastReceiver(){
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle b = intent.getExtras();
            DownloadManager dm = (DownloadManager) getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(b.getLong(DownloadManager.EXTRA_DOWNLOAD_ID));
            Cursor cur = dm.query(query);
            boolean success = false;
            if (cur.moveToFirst()) {
                int columnIndex = cur.getColumnIndex(DownloadManager.COLUMN_STATUS);
                if (DownloadManager.STATUS_SUCCESSFUL == cur.getInt(columnIndex)) {
                    String uriString = cur.getString(cur.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    File myFile = new File(uriString.replace("file://", ""));
                    if (myFile.isFile()) {

                        Intent returnIntent = new Intent();
                        returnIntent.putExtra("installNewApp", myFile.toString());
                        PreferencesActivity.this.setResult(Activity.RESULT_OK, returnIntent);
                        PreferencesActivity.this.finish();
                        success = true;
                        context.unregisterReceiver(this);
                    }
                }
            }
            if (!success)
                Toast.makeText(getApplicationContext(), "Download file not found. Please try again!", Toast.LENGTH_LONG).show();
        }
    };



    public static class Preferences extends PreferenceFragment {
        private String TAG = this.getClass().getSimpleName();

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
            PreferencesActivity tmp = (PreferencesActivity)getActivity();
            findPreference("unsetDeviceAdmin").setEnabled(tmp.isDeviceOwner);
            findPreference("checkForUpdate").setEnabled(com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.exists());
            if (findPreference("checkForUpdate").isEnabled() == false)
                findPreference("checkForUpdate").setSummary(com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.getAbsolutePath() + " is missing!");
            //else if (isNetworkAvailable() == false)
            //    findPreference("checkForUpdate").setSummary("No active internet connection!");

            includeQuestList();
            includedAFExList();
            Preference deviceOwnerPref = (Preference) findPreference("unsetDeviceAdmin");
            deviceOwnerPref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    confirmUnsetDeviceOwner();
                    return true;
                }
            });
            Preference killAppAndServicePref = (Preference) findPreference("killAppAndService");
            killAppAndServicePref.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                @Override
                public boolean onPreferenceClick(Preference arg0) {
                    confirmKillAppAndService();
                    return true;
                }
            });
            Preference VersionPref = (Preference) findPreference("Version");
            VersionPref.setSummary(BuildConfig.VERSION_NAME);
            Preference button = (Preference) findPreference("checkForUpdate");
            if (button != null) {
                button.setOnPreferenceClickListener(new Preference.OnPreferenceClickListener() {
                    @Override
                    public boolean onPreferenceClick(Preference arg0) {
                        checkForUpdate();
                        return true;
                    }
                });
            }
        }

        private void checkForUpdate() {
            if (isNetworkAvailable()) {
                if (com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.isFile())
                {
                    AppUpdaterUtils appUpdater = new AppUpdaterUtils(getActivity())
                            //.setUpdateFrom(UpdateFrom.AMAZON)
                            //.setUpdateFrom(UpdateFrom.GITHUB)
                            //.setGitHubUserAndRepo("javiersantos", "AppUpdater")
                            //...
                            .withListener(new AppUpdaterUtils.UpdateListener() {
                                @Override
                                public void onSuccess(Update update, Boolean isUpdateAvailable) {
                                    if (isUpdateAvailable) {
                                        AlertDialog.Builder alertDialogBuilder = new AlertDialog.Builder(getContext());
                                        alertDialogBuilder.setTitle("New update available!");
                                        alertDialogBuilder
                                                .setMessage("Update " + update.getLatestVersion() + " available to download!\n" + update.getReleaseNotes())
                                                .setCancelable(false)
                                                .setPositiveButton("Update",new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,int id) {
                                                        PreferencesActivity tmp = (PreferencesActivity)getActivity();
                                                        tmp.DownloadUpdate(update.getUrlToDownload().toString() + "?" + System.currentTimeMillis());
                                                    }
                                                })
                                                .setNegativeButton("Cancel",new DialogInterface.OnClickListener() {
                                                    public void onClick(DialogInterface dialog,int id) {
                                                        dialog.cancel();
                                                    }
                                                });
                                        AlertDialog alertDialog = alertDialogBuilder.create();
                                        alertDialog.show();
                                        /*
                                        Log.d("Latest Version", update.getLatestVersion());
                                        Log.d("Latest Version Code", update.getLatestVersionCode().toString());
                                        Log.d("Release notes", update.getReleaseNotes());
                                        Log.d("URL", update.getUrlToDownload().toString());
                                        Log.d("Is update available?", Boolean.toString(isUpdateAvailable));
                                         */
                                    }
                                    else {
                                        Toast.makeText(getActivity(), "No update available!", Toast.LENGTH_LONG).show();
                                    }
                                }

                                @Override
                                public void onFailed(AppUpdaterError error) {
                                    Toast.makeText(getActivity(), "AppUpdater Error: Something went wrong", Toast.LENGTH_LONG).show();
                                    Log.d("AppUpdater Error", error.toString());
                                }
                            });
                    /*
                    AppUpdater appUpdater = new AppUpdater(getActivity())
                            .showAppUpdated(true)
                            .setCancelable(false)
                            .setButtonDoNotShowAgain(null)
                            .setButtonUpdateClickListener(new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialogInterface, int i) {
                                    StartUpdate();
                                }
                            })
                            .setDisplay(Display.DIALOG);
                     */
                    try {
                        Document doc = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings);
                        NodeList elements = doc.getElementsByTagName("Source");
                        for (int i = 0; i < elements.getLength(); i++) {
                            NamedNodeMap attributes = elements.item(i).getAttributes();
                            if (attributes.getNamedItem("type").getNodeValue().equals("XML")) {
                                appUpdater.setUpdateFrom(UpdateFrom.XML);
                                appUpdater.setUpdateXML(attributes.getNamedItem("URL").getNodeValue() + "?" + System.currentTimeMillis());
                            }
                            else if (attributes.getNamedItem("type").getNodeValue().equals("JSON")) {
                                appUpdater.setUpdateFrom(UpdateFrom.JSON);
                                appUpdater.setUpdateJSON(attributes.getNamedItem("URL").getNodeValue() + "?" + System.currentTimeMillis());
                            }
                        }
                        appUpdater.start();
                    } catch (Exception e) {
                        Toast.makeText(getActivity(), "'" + com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.getAbsoluteFile() + "' not valid!", Toast.LENGTH_SHORT).show();
                    }
                }
                else
                    Toast.makeText(getActivity(), "'" + com.iha.olmega_mobilesoftware_v2.Preferences.UdaterSettings.getAbsoluteFile() + "' does not exist!", Toast.LENGTH_SHORT).show();
            }
            else
                Toast.makeText(getActivity(), "No active internet connection!", Toast.LENGTH_SHORT).show();

        }

        private boolean isNetworkAvailable() {
            ConnectivityManager connectivityManager = (ConnectivityManager) getActivity().getSystemService(getActivity().CONNECTIVITY_SERVICE);
            NetworkInfo activeNetworkInfo = connectivityManager.getActiveNetworkInfo();
            return activeNetworkInfo != null && activeNetworkInfo.isConnected();
        }

        private void includeQuestList() {
            // Scan file system for available questionnaires
            FileIO fileIO = new FileIO();
            String[] fileList = fileIO.scanQuestOptions();

            ListPreference listPreferenceQuest = (ListPreference) findPreference("selectedQuest");
            // TODO: Isn't the second constraint enough?
            if ((fileList != null) && (fileList.length > 0)) {
                // Fill in menu contents
                listPreferenceQuest.setEntries(fileList);
                listPreferenceQuest.setEntryValues(fileList);
                listPreferenceQuest.setDefaultValue(fileList[0]);
            } else {
                listPreferenceQuest.setSummary(R.string.noQuestionnaires);
                listPreferenceQuest.setSelectable(false);
            }
        }

        private void includedAFExList() {
            File directory = SystemStatus.AFExConfigFolder;
            if (!directory.exists())
                directory.mkdirs();
            File[] files = directory.listFiles();
            String[] fileList = new String[files.length];
            for (int i = 0; i < files.length; i++)
                if (files[i].getName().substring(files[i].getName().lastIndexOf(".")).toLowerCase().equals(".xml"))
                    fileList[i] = files[i].getName();
            ListPreference listPreference= (ListPreference) findPreference("inputProfile");
            if ((fileList != null) && (fileList.length > 0)) {
                // Fill in menu contents
                listPreference.setEntries(fileList);
                listPreference.setEntryValues(fileList);
                listPreference.setDefaultValue(fileList[0]);
            } else {
                listPreference.setSummary("no AEFx-Settings in '" + SystemStatus.AFExConfigFolder + "'");
                listPreference.setSelectable(false);
            }
        }

        private void confirmUnsetDeviceOwner() {
            new AlertDialog.Builder(getActivity(), R.style.SwipeDialogTheme)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.deviceOwnerMessage)
                    .setPositiveButton(R.string.deviceOwnerYes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("killAppAndService", true);
                            returnIntent.putExtra("unsetDeviceAdmin", true);
                            getActivity().setResult(Activity.RESULT_OK, returnIntent);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(R.string.deviceOwnerNo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setCancelable(false)
                    .show();
        }

        private void confirmKillAppAndService() {
            new AlertDialog.Builder(getActivity(), R.style.SwipeDialogTheme)
                    .setTitle(R.string.app_name)
                    .setMessage(R.string.killAppAndServiceMessage)
                    .setPositiveButton(R.string.deviceOwnerYes, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent returnIntent = new Intent();
                            returnIntent.putExtra("killAppAndService", true);
                            getActivity().setResult(Activity.RESULT_OK, returnIntent);
                            getActivity().finish();
                        }
                    })
                    .setNegativeButton(R.string.deviceOwnerNo, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                        }
                    })
                    .setCancelable(false)
                    .show();
        }
    }
}
