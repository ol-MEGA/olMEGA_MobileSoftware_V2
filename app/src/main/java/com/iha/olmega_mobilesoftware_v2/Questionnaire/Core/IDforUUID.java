package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;

import android.util.Log;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StringAndInteger;
import com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes.StringAndString;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


public final class IDforUUID {

    private final String LOG = "IDforUUID";
    private static ArrayList<StringAndString> mIDs;
    private static ArrayList<String> mQuestionList;

    public IDforUUID() {
        mIDs = new ArrayList<>();

    }

    public ArrayList<String> exchange(ArrayList<String> questionList) {

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

    public String getId(String uuid) {
        for (int i=0; i<mIDs.size(); i++) {
            if (mIDs.get(i).getText().equals(uuid)) {
                //Log.e(LOG, "I've seen this id before!");
                return mIDs.get(i).getId();
            } else if (mIDs.get(i).getText().equals(uuid.substring(1))) {
                //Log.e(LOG, "I've seen this id before! - but now negative");
                return mIDs.get(i).getId();
            }
        }

        mIDs.add(new StringAndString(uuid, "" + (mIDs.size() + 10001)));
        return mIDs.get(mIDs.size()-1).getId();
    }

    public String getUUID(String id) {
        for (int i=0; i<mIDs.size(); i++) {
            if (mIDs.get(i).getId().equals(id)) {
                return mIDs.get(i).getText();
            }
        }
        return "";
    }

}




