package com.example.musicplayer;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.Manifest;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.pm.PackageManager;
import android.content.res.TypedArray;
import android.database.Cursor;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.IBinder;
import android.provider.MediaStore;
import android.util.Log;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ImageView;
import android.widget.MediaController.MediaPlayerControl;
import android.widget.Toast;

import com.example.musicplayer.DAO.Audio;
import com.example.musicplayer.DAO.MySharedPreference;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;

import static com.example.musicplayer.MyService.*;

public class MainActivity extends AppCompatActivity implements OnItemClickListener,MediaPlayerControl {
    private static final int REQUEST_READ_PERMISSION = 1;
    boolean isBound = false;
    private  MyService myService;
    ArrayList<Audio> audioArrayList;
    ImageView collapsingImageView;
    int imageIndex = 0;
    private MusicController controller;

    public static final String Broadcast_PLAY_NEW_AUDIO = "com.example.musicplayer.PlayNewAudio";
    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder iBinder) {
            MyService.LocalBinder myLocalBinder = (MyService.LocalBinder) iBinder;
            myService = myLocalBinder.getService();
            isBound = true;

            Toast.makeText(MainActivity.this, "Service Bound", Toast.LENGTH_SHORT).show();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar)findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        collapsingImageView = (ImageView)findViewById(R.id.collapsingImageView);
        loadCollapsingImage(imageIndex);
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED){
            requestPermissions();
        }else{
            loadAudio();
            intiRecyclerView();
//            setController();
        }
        createNotificationChannel();



        FloatingActionButton fab = (FloatingActionButton)findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(imageIndex == 4 ){
                    imageIndex = 0;
                    loadCollapsingImage(imageIndex);
                }else loadCollapsingImage(++imageIndex);
            }
        });
        ViewTreeObserver vto = recyclerView.getViewTreeObserver();
        vto.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                setController();
            }
        });
    }




//    @Override
//    protected void onStart() {
//        super.onStart();
//
//        Intent intent = new Intent(this,MyService.class);
//        bindService(intent,mConnection,Context.BIND_AUTO_CREATE);
//    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        outState.putBoolean("serviceState",isBound);
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
        isBound = savedInstanceState.getBoolean("serviceState");
    }
    private void intiRecyclerView() {
        if (audioArrayList.size() > 0){
            recyclerView = (RecyclerView)findViewById(R.id.recyclerList);
            MyRecyclerAdapter adapter = new MyRecyclerAdapter(this,audioArrayList);
            recyclerView.setAdapter(adapter);
            recyclerView.setLayoutManager(new LinearLayoutManager(this));
        }
    };

    private void loadCollapsingImage(int imageIndex) {
        TypedArray array = getResources().obtainTypedArray(R.array.images);
        collapsingImageView.setImageDrawable(array.getDrawable(imageIndex));
    }
    private void loadAudio(){
        ContentResolver contentResolver = getContentResolver();
        Uri uri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        String selection = MediaStore.Audio.Media.IS_MUSIC+ "!=0";
        String sortOrder = MediaStore.Audio.Media.TITLE+ " ASC ";
        Cursor cursor = contentResolver.query(uri,null,selection,null,sortOrder);

        if(cursor != null && cursor.getCount() > 0){
            audioArrayList = new ArrayList<>();
            while (cursor.moveToNext()){
                String data = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DATA));
                String title = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE));
                String album = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM));
                String artist = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST));

                audioArrayList.add(new Audio(data,title,album,artist));
                MySharedPreference preference = new MySharedPreference(MainActivity.this);
                preference.storeAudio(audioArrayList);
            }
        }
        cursor.close();
    }

    private void playAudio(int audioIndex){
        if(!isBound){
            MySharedPreference preference = new MySharedPreference(this);
//            preference.storeAudio(audioArrayList);
            preference.storeAudioIndex(audioIndex);
            Intent intent = new Intent(this,MyService.class);
            startService(intent);
            bindService(intent,mConnection, Context.BIND_AUTO_CREATE);
        }else {
            //Service is active
            //Send media with BroadcastReceiver
            //Store the new audioIndex to SharedPreferences
            MySharedPreference preference = new MySharedPreference(getApplicationContext());
            preference.storeAudioIndex(audioIndex);
            Intent broadcastIntent = new Intent(Broadcast_PLAY_NEW_AUDIO);
            sendBroadcast(broadcastIntent);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(isBound){
            unbindService(mConnection);
            myService.stopSelf();
        }
    }
    private void requestPermissions() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            requestPermissions(new String[]{
                    Manifest.permission.READ_EXTERNAL_STORAGE
            },REQUEST_READ_PERMISSION);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode){
            case REQUEST_READ_PERMISSION:
                if(grantResults.length > 0 && permissions[0].equals(Manifest.permission.READ_EXTERNAL_STORAGE)){
                    if (grantResults[0] == PackageManager.PERMISSION_GRANTED){
                        loadAudio();
                        intiRecyclerView();
                        setController();
                    }
                }
        }
    }

    @Override
    public void onItemClick(int position) {
        Log.d("POSITION", String.valueOf(position) + audioArrayList.size());
        playAudio(position);

    }
    public void createNotificationChannel(){
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID_1,
                    "FIRSTCHANNEL",
                    NotificationManager.IMPORTANCE_LOW
            );
            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
            channel.enableLights(true);
            NotificationManager manager  = getSystemService(NotificationManager.class);
            manager.createNotificationChannel(channel);
        }
    }
    private void setController(){
        controller = new MusicController(this);
        controller.setPrevNextListeners(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playNext();
            }
        }, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                playPrev();
            }
        });
        controller.setMediaPlayer(this);
        controller.setAnchorView(findViewById(R.id.recyclerList));
        controller.setEnabled(true);
    }

    private void playPrev() {
        myService.skipToPrevious();
        controller.show(0);

    }

    private void playNext() {
        myService.skipToNext();
        controller.show(0);

    }

    @Override
    public void start() {

    }

    @Override
    public void pause() {

    }

    @Override
    public int getDuration() {
        return 0;
    }

    @Override
    public int getCurrentPosition() {
        return 0;
    }

    @Override
    public void seekTo(int pos) {

    }

    @Override
    public boolean isPlaying() {
        return false;
    }

    @Override
    public int getBufferPercentage() {
        return 0;
    }

    @Override
    public boolean canPause() {
        return false;
    }

    @Override
    public boolean canSeekBackward() {
        return false;
    }

    @Override
    public boolean canSeekForward() {
        return false;
    }

    @Override
    public int getAudioSessionId() {
        return 0;
    }
}