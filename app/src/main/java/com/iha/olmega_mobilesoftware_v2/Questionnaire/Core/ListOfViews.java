package com.iha.olmega_mobilesoftware_v2.Questionnaire.Core;

import com.iha.olmega_mobilesoftware_v2.Questionnaire.Questionnaire.QuestionView;

import java.util.ArrayList;

/**
 * ListOfViews carries all QuestionView objects that are part of the current active Questionnaire
 */

public class ListOfViews extends ArrayList<QuestionView> {

    public ListOfViews() {

    }

    public QuestionView getFromId(int id) {
        for (int iItem = 0; iItem < this.size(); iItem++) {
            if (this.get(iItem).getId() == id) {
                return this.get(iItem);
            }
        }
        return null;
    }

    public void removeFromId(int id) {
        for (int iItem = this.size() - 1; iItem >= 0; iItem--) {
            if (this.get(iItem).getId() == id) {
                this.remove(iItem);
            }
        }
    }

    public int getPosFromId(int id) {
        for (int iItem = 0; iItem < this.size(); iItem++) {
            if (this.get(iItem).getId() == id) {
                return iItem;
            }
        }
        return -1;
    }

    public int indexOf(Object object) {
        for (int iItem = 0; iItem < this.size(); iItem++) {
            if (this.get(iItem) == object) {
                return iItem;
            }
        }
        return -1;
    }

}
