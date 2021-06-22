package com.iha.olmega_mobilesoftware_v2.AFEx.AcousticFeatureExtraction;

import java.util.HashMap;

public class StageIdle extends Stage {

    public StageIdle(HashMap parameter) {
        super(parameter);
        hasInput = false;
    }

    @Override
    protected void process(float[][] buffer) {
    }

}
