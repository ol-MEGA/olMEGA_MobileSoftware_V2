package com.iha.olmega_mobilesoftware_v2.Questionnaire.DataTypes;

/**
 * Created by ulrikkowalk on 01.02.22.
 */

public class StringAndString {

    private String Text;
    private String Id;

    public StringAndString(String string, String id) {
        Text = string;
        Id = id;
    }

    public void setText(String text) { Text = text; }

    public String getText() { return Text; }

    public String getId() { return Id; }

}
