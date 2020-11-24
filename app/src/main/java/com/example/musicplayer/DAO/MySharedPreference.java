package com.example.musicplayer.DAO;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;

public class MySharedPreference {
    private final String STORAGE = "com.example.musicplayer.DAO.STORAGE";
    private SharedPreferences preferences;
    private Context mContext;

    public MySharedPreference( Context mContext) {
        this.mContext = mContext;
    }
    public void storeAudio(ArrayList<Audio> audioArrayList){
        preferences = mContext.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        Gson gson = new Gson();
        String json = gson.toJson(audioArrayList);
        editor.putString("audioArrayList",json);
        editor.apply();
    }
    public ArrayList<Audio> loadAudio() {
        preferences = mContext.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        Gson gson = new Gson();
        String json = preferences.getString("audioArrayList", null);
        Type type = new TypeToken<ArrayList<Audio>>() {
        }.getType();
        return gson.fromJson(json, type);
    }
    public void storeAudioIndex(int index){
        preferences = mContext.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.putInt("audioIndex",index);
        editor.apply();
    }
    public int loadAudioIndex() {
        preferences = mContext.getSharedPreferences(STORAGE, Context.MODE_PRIVATE);
        return preferences.getInt("audioIndex", -1);//return -1 if no data found
    }
    public void clearCachedAudioPlaylist(){
        preferences = mContext.getSharedPreferences(STORAGE,Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = preferences.edit();
        editor.clear();
        editor.commit();
    };
}
