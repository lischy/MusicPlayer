package com.example.musicplayer;

import android.app.Activity;
import android.content.Context;
import android.view.KeyEvent;
import android.widget.MediaController;

public class MusicController extends MediaController {
    public MusicController(Context context) {
        super(context);
    }
    public void hide(){}
    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        if(event.getKeyCode() == KeyEvent.KEYCODE_BACK) {
            Activity a = (Activity)getContext();
            a.finish();
        }
        return true;
    }

}
