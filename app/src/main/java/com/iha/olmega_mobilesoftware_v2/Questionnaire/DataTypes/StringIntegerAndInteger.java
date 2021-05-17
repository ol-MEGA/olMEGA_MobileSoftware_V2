package com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes;

/**
 * Created by ulrikkowalk on 31.03.17.
 */

public class StringIntegerAndInteger {

    private String Text;
    private int Id, Group;

    public StringIntegerAndInteger(String string, int id, int group) {
        Text = string;
        Id = id;
        Group = group;
    }

    public void setText(String text) { Text = text; }

    public String getText() { return Text; }

    public int getId() { return Id; }

    public int getGroup() { return Group; }
}
