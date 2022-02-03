package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;
import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StringAndString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class IDforUUID {

    private static final String LOG = "IDforUUID";
    private static ArrayList<StringAndString> mIDs;
    private static ArrayList<String> mQuestionList;
    private static IDforUUID instance = null;

    public static IDforUUID getInstance() {
        if (instance == null) {
            instance = new IDforUUID();
        }
        return instance;
    }

    public IDforUUID() {
        mIDs = new ArrayList<>();
    }

    public static ArrayList<String> exchange(ArrayList<String> questionList) {

        mQuestionList = questionList;

        for (int quest=0; quest<mQuestionList.size(); quest++) {

            String[] lines_list = mQuestionList.get(quest).split("\n");
            for (int line=0; line<lines_list.length; line++) {

                // split line into words/items
                String[] line_split = lines_list[line].split(" ");
                for (int item=0; item<line_split.length; item++) {

                    // Search for question and option id and replace
                    if (line_split[item].contains("id=")) {
                        String[] id_list = line_split[item].split("id=\"");
                        String uuid = id_list[1].split(" ")[0].replace("\">", "").replace("\"", "");
                        String newId = getId(uuid);
                        if (line_split[item].endsWith("\">")) {
                            line_split[item] = "id=\"" + newId + "\">";
                        } else if (line_split[item].endsWith("\"")) {
                            line_split[item] = "id=\"" + newId + "\"";
                        }

                    }

                    // Search for filter ids
                    if (line_split[item].contains("filter=")) {
                        //Log.e(LOG, line_split[item]);
                        String[] id_list = line_split[item].split("filter=\"");
                        String uuids = id_list[1].split(" ")[0].replace("\">", "").replace("\"", "");
                        //Log.e(LOG, " " + id_list[1].split(" ")[0]);
                        String[] uuid_list = uuids.split(",");
                        String sNewId = "filter=\"";
                        for (int id=0; id<uuid_list.length; id++) {
                            String uuid = uuid_list[id];
                            String newId = "";
                            if (uuid_list[id].charAt(0) == '!') {
                                newId = "!" + getId(uuid);
                            } else {
                                newId = getId(uuid);
                            }
                            //Log.e(LOG, "FILTER: " + uuid);

                            sNewId = sNewId + newId;
                            if (uuid_list.length > 1 && id<uuid_list.length-1)  {
                                sNewId = sNewId + ",";
                            }
                            //Log.e(LOG, "Replaced with: " + newId);
                            //Log.e(LOG, "Number of entries: " + mIDs.size());
                        }
                        if (id_list[1].split(" ")[0].endsWith("\">")) {
                            sNewId = sNewId + "\">";
                        } else if (id_list[1].split(" ")[0].endsWith("\"")) {
                            sNewId = sNewId + "\"";
                        }
                        line_split[item] = sNewId;
                    }

                }

                List<String> line_new = Arrays.asList(line_split);
                String result = String.join(" ", line_new);
                lines_list[line] = result;
                //Log.e(LOG, "Old: " + lines_list[line]);
                //Log.e(LOG, "New: " + result);
            }

            List<String> quest_new = Arrays.asList(lines_list);
            String quest_result = String.join("\n", quest_new);
            //Log.e(LOG, "Old: " + mQuestionList.get(quest));
            //Log.e(LOG, "New: " + quest_result);
            mQuestionList.set(quest, quest_result);

        }
        return mQuestionList;
    }

    public static String getId(String uuid) {
        for (int i=0; i<IDforUUID.mIDs.size(); i++) {
            if (IDforUUID.mIDs.get(i).getText().equals(uuid)) {
                //Log.e(LOG, "I've seen this id before!");
                return IDforUUID.mIDs.get(i).getId();
            } else if (IDforUUID.mIDs.get(i).getText().equals(uuid.substring(1))) {
                //Log.e(LOG, "I've seen this id before! - but now negative");
                return IDforUUID.mIDs.get(i).getId();
            }
        }

        IDforUUID.mIDs.add(new StringAndString(uuid, "" + (mIDs.size() + 10001)));
        Log.e(LOG, "Size: " + IDforUUID.mIDs.size());
        return IDforUUID.mIDs.get(IDforUUID.mIDs.size()-1).getId();
    }

    public static String getUUID(String id) {
        Log.e(LOG, "Requesting UUID for: " + id);
        for (int i=0; i<IDforUUID.mIDs.size(); i++) {
            //Log.e(LOG, "UUID: " + IDforUUID.mIDs.get(i));
            if (IDforUUID.mIDs.get(i).getId().equals(id)) {
                Log.e(LOG, "Found: " + IDforUUID.mIDs.get(i).getText());
                return mIDs.get(i).getText();
                //return "TestID";
            }
        }
        if (id.equals("999999999")) {
            return "Finish";
        }
        return "";
    }

    public static int getTableLen() {
        return mIDs.size();
    }


}




