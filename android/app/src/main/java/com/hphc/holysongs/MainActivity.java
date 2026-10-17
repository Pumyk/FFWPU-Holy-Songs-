package com.hphc.holysongs;

import android.os.Bundle;
import com.getcapacitor.BridgeActivity;

public class MainActivity extends BridgeActivity {
    @Override
    public void onCreate(Bundle savedInstanceState) {
        registerPlugin(MaterialYouPlugin.class);
        super.onCreate(savedInstanceState);
    }
}
