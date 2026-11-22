package com.example.evotingapp.ui;

import android.app.Application;

public class EvotingApp extends Application {
    private AppContainer container;

    @Override
    public void onCreate() {
        super.onCreate();
        container = new AppContainer(this);
    }

    public AppContainer container() {
        return container;
    }
}
