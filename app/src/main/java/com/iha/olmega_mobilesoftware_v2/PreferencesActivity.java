package com.iha.olmega_mobilesoftware_v2;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.widget.Toast;

import com.github.javiersantos.appupdater.AppUpdater;
import com.github.javiersantos.appupdater.enums.Display;
import com.github.javiersantos.appupdater.enums.UpdateFrom;
import com.iha.olmega_mobilesoftware_v2.Core.FileIO;

import org.w3c.dom.Document;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.NodeList;

import java.io.File;

import javax.xml.parsers.DocumentBuilderFactory;

public class PreferencesActivity extends PreferenceActivity {
    private String TAG = this.getClass().getSimpleName();

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(null);
        getFragmentManager().beginTransaction().replace(android.R.id.content, new Preferences()).commit();
    }

    public static class Preferences extends PreferenceFragment {

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.preferences);
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
                    AppUpdater appUpdater = new AppUpdater(getActivity())
                            .showAppUpdated(true)
                            .setCancelable(false)
                            .setButtonDoNotShowAgain(null)
                            .setDisplay(Display.DIALOG);
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
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                            prefs.edit().putBoolean("unsetDeviceAdmin", true).commit();
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
                            SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(getActivity().getApplicationContext());
                            prefs.edit().putBoolean("killAppAndService", true).commit();
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
