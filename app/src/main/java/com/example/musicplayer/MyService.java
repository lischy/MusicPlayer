package com.example.musicplayer;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.session.MediaSessionManager;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.os.RemoteException;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

import com.example.musicplayer.DAO.Audio;
import com.example.musicplayer.DAO.MySharedPreference;
import com.example.musicplayer.DAO.PlaybackStatus;

import java.util.ArrayList;

public class MyService extends Service implements MediaPlayer.OnPreparedListener,
MediaPlayer.OnCompletionListener,MediaPlayer.OnBufferingUpdateListener,MediaPlayer.OnSeekCompleteListener,
MediaPlayer.OnInfoListener,MediaPlayer.OnErrorListener,
        AudioManager.OnAudioFocusChangeListener {
    public static final String CHANNEL_ID_1 = "channel1";
    private  LocalBinder MyLocalBinder = new LocalBinder();
    private MediaPlayer mediaPlayer;
    private AudioManager audioManager;
//    private String mediaFile;


    private ArrayList<Audio> audioArrayList;
    private int audioIndex = -1;
    private Audio activeAudio;

    private int resumePosition;
    private boolean focusgranted = false;

    public static final String ACTION_PLAY = "com.example.musicplayer.MainActivity.ACTION_PLAY";
    public static final String ACTION_PAUSE = "com.example.musicplayer.MainActivity.ACTION_PAUSE";
    public static final String ACTION_PREVIOUS = "com.example.musicplayer.MainActivity.ACTION_PREVIOUS";
    public static final String ACTION_NEXT = "com.example.musicplayer.MainActivity.ACTION_NEXT";
    public static final String ACTION_STOP = "com.example.musicplayer.MainActivity.ACTION_STOP";

    private MediaSessionManager mediaSessionManager;
    private MediaSessionCompat sessionCompat;
    private MediaControllerCompat.TransportControls transportControls;
    private static final int NOTIFICATION_ID = 100;

    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        try{
//            mediaFile = intent.getExtras().getString("media");
        MySharedPreference preference = new MySharedPreference(getApplicationContext());
        audioArrayList = preference.loadAudio();
        audioIndex = preference.loadAudioIndex();
        if (audioIndex != -1 && audioIndex < audioArrayList.size()){
            activeAudio = audioArrayList.get(audioIndex);

        }else{
            stopSelf();
            }
        }catch (NullPointerException e){
            stopSelf();
        }
        if(requestAudioFocus() == false){
            stopSelf();
        }
        if(mediaSessionManager == null ) {
            try {
                initMediaSession();
                initMediaPlayer();
            }catch (RemoteException e){
                e.printStackTrace();
                stopSelf();
            }
            buildNotification(PlaybackStatus.PLAYING);
        }
        handleIncomingActions(intent);
        return super.onStartCommand(intent, flags, startId);
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return MyLocalBinder;
    }

    @Override
    public void onCreate() {
        super.onCreate();
        register_playNewAudio();
    }

    @Override
    public boolean onUnbind(Intent intent) {
        sessionCompat.release();
        removeNotification();
        return super.onUnbind(intent);
    }

    private void initMediaPlayer(){
        mediaPlayer = new MediaPlayer();
        mediaPlayer.setOnCompletionListener(this);
        mediaPlayer.setOnErrorListener(this);
        mediaPlayer.setOnPreparedListener(this);
        mediaPlayer.setOnBufferingUpdateListener(this);
        mediaPlayer.setOnSeekCompleteListener(this);
        mediaPlayer.setOnInfoListener(this);
        mediaPlayer.reset();
        mediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC);
        try{
            mediaPlayer.setDataSource(activeAudio.getData());
        }catch (Exception e){
            e.printStackTrace();
            stopSelf();
        }
        mediaPlayer.prepareAsync();
    }
    @RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
    private void initMediaSession() throws RemoteException{
        if (mediaSessionManager != null) return;
            mediaSessionManager = (MediaSessionManager) getSystemService(Context.MEDIA_SESSION_SERVICE);
            sessionCompat = new MediaSessionCompat(getApplicationContext(),"AudioPlayer");
            transportControls = sessionCompat.getController().getTransportControls();
            sessionCompat.setActive(true);
            sessionCompat.setFlags(MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS| MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS);
            updateMetaData();
            sessionCompat.setCallback(new MediaSessionCompat.Callback() {
                @Override
                public void onPlay() {
                    super.onPlay();
                    resumeMedia();
                    buildNotification(PlaybackStatus.PLAYING);
                }

                @Override
                public void onPause() {
                    super.onPause();
                    pauseMedia();
                    buildNotification(PlaybackStatus.PAUSED);
                }

                @Override
                public void onSkipToNext() {
                    super.onSkipToNext();
                    skipToNext();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                }

                @Override
                public void onSkipToPrevious() {
                    super.onSkipToPrevious();
                    skipToPrevious();
                    updateMetaData();
                    buildNotification(PlaybackStatus.PLAYING);
                }

                @Override
                public void onStop() {
                    super.onStop();
                    removeNotification();
                    //Stop the service
                    stopSelf();
                }

                @Override
                public void onSeekTo(long pos) {
                    super.onSeekTo(pos);
                }
            });

    }
 public int getPosn(){
        return mediaPlayer.getCurrentPosition();
 }
 public int getDur(){
        return mediaPlayer.getDuration();
 }

    private void playMedia(){
        if(!mediaPlayer.isPlaying()){
            mediaPlayer.start();
        }
    }
    private void stopMedia(){
        if(mediaPlayer == null) return;
        if(mediaPlayer.isPlaying()){
            mediaPlayer.stop();
        }
    }
    private void pauseMedia(){
        if(mediaPlayer.isPlaying()){
            mediaPlayer.pause();
            resumePosition = mediaPlayer.getCurrentPosition();
        }
    }
    private void resumeMedia(){
        if(!mediaPlayer.isPlaying()){
//            mediaPlayer.seekTo(resumePosition);
            mediaPlayer.start();
        }
    }
    public void skipToPrevious() {
        if (audioIndex == 0 ){
            audioIndex = audioArrayList.size() - 1;
            activeAudio = audioArrayList.get(audioIndex);
        }else {
            activeAudio =audioArrayList.get(--audioIndex);
        }
        new MySharedPreference(getApplicationContext()).storeAudioIndex(audioIndex);
        stopMedia();
        mediaPlayer.reset();
        initMediaPlayer();
    }
    public void skipToNext() {
        if (audioIndex == audioArrayList.size() - 1 ){
            audioIndex = 0;
            activeAudio = audioArrayList.get(audioIndex);
        }else {
            activeAudio = audioArrayList.get(++audioIndex);
        }
        new MySharedPreference(getApplicationContext()).storeAudioIndex(audioIndex);
        stopSelf();
        mediaPlayer.reset();
        initMediaPlayer();
    }
    private void removeNotification() {
        NotificationManager notificationManager = (NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.cancel(NOTIFICATION_ID);
    }
    private boolean requestAudioFocus(){
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
        int result = audioManager.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);
        if(result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED){
            focusgranted = true;
            return true;

        }
        return false;
    }
    private boolean removeAudioFocus(){
        if(!focusgranted){
            focusgranted = false;
//            return AudioManager.AUDIOFOCUS_REQUEST_GRANTED == audioManager.abandonAudioFocus(this);

        }
        return false;
    }
    private BroadcastReceiver playNewAudio = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            audioArrayList = new MySharedPreference(getApplicationContext()).loadAudio();
            audioIndex = new MySharedPreference(getApplicationContext()).loadAudioIndex();
            if (audioIndex != -1 && audioIndex < audioArrayList.size()){
                activeAudio = audioArrayList.get(audioIndex);
            }else {
                stopSelf();
            }
            stopMedia();
            if (mediaPlayer != null)
            mediaPlayer.reset();
            initMediaPlayer();
            updateMetaData();
            buildNotification(PlaybackStatus.PLAYING);
        }
    };
    private void register_playNewAudio() {
        //Register playNewMedia receiver
        IntentFilter filter = new IntentFilter(MainActivity.Broadcast_PLAY_NEW_AUDIO);
        registerReceiver(playNewAudio, filter);
    }
    private PendingIntent playbackAction(int actionNumber){
        Intent playbackAction = new Intent(this,MyService.class);
        switch (actionNumber){
            case 0:
                playbackAction.setAction(ACTION_PLAY);
                return PendingIntent.getService(this,actionNumber,playbackAction,0);
            case 1:
                // Pause
                playbackAction.setAction(ACTION_PAUSE);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 2:
                // Next track
                playbackAction.setAction(ACTION_NEXT);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            case 3:
                // Previous track
                playbackAction.setAction(ACTION_PREVIOUS);
                return PendingIntent.getService(this, actionNumber, playbackAction, 0);
            default:
                break;
        }
        return null;
    }

//    public void createNotificationChannel(){
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O){
//            NotificationChannel channel = new NotificationChannel(
//                    CHANNEL_ID_1,
//                    "FIRSTCHANNEL",
//                    NotificationManager.IMPORTANCE_HIGH
//            );
//            channel.setLockscreenVisibility(Notification.VISIBILITY_PRIVATE);
//            channel.enableLights(true);
//            NotificationManager manager  = getSystemService(NotificationManager.class);
//            manager.createNotificationChannel(channel);
//        }
//    }
    private void buildNotification(PlaybackStatus playbackStatus) {
        int notificationAction = R.drawable.ic_baseline_pause_24;
        PendingIntent play_pauseAction = null;
        if (playbackStatus == PlaybackStatus.PLAYING){
            notificationAction = R.drawable.ic_baseline_pause_24;
            play_pauseAction = playbackAction(1);
        }else if (playbackStatus == PlaybackStatus.PAUSED){
            notificationAction = R.drawable.ic_baseline_play_arrow_24;
            play_pauseAction = playbackAction(0);
        }

        Bitmap largeIcon = BitmapFactory.decodeResource(getResources(),
                R.drawable.image1);
        NotificationCompat.Builder notificationBuilder = (NotificationCompat.Builder) new NotificationCompat.Builder(this,CHANNEL_ID_1);
        notificationBuilder.setShowWhen(false);
        notificationBuilder.setColor(getResources().getColor(R.color.colorPrimary));
        notificationBuilder.setLargeIcon(largeIcon);
        notificationBuilder.setSmallIcon(android.R.drawable.stat_sys_headset);
        notificationBuilder.setContentText(activeAudio.getArtist());
        notificationBuilder.setContentTitle(activeAudio.getAlbum());
        notificationBuilder.setContentInfo(activeAudio.getTitle());
        notificationBuilder.addAction(R.drawable.ic_baseline_skip_previous_24,"previous",playbackAction(3));
        notificationBuilder.addAction(notificationAction, "pause", play_pauseAction);
        notificationBuilder.addAction(R.drawable.ic_baseline_skip_next_24,"next",playbackAction(2));
        NotificationManagerCompat notificationManagerCompat = NotificationManagerCompat.from(getApplicationContext());
        notificationBuilder.setStyle(new androidx.media.app.NotificationCompat.MediaStyle().setMediaSession(sessionCompat.getSessionToken()).setShowActionsInCompactView(0,1,2));
        notificationManagerCompat.notify(NOTIFICATION_ID,notificationBuilder.build());
//        ((NotificationManager)getSystemService(Context.NOTIFICATION_SERVICE)).notify(NOTIFICATION_ID,notificationBuilder.build());
    }

    private void updateMetaData() {
        Bitmap albumArt = BitmapFactory.decodeResource(getResources(),R.drawable.image1);
        sessionCompat.setMetadata(new MediaMetadataCompat.Builder()
        .putBitmap(MediaMetadataCompat.METADATA_KEY_ALBUM_ART,albumArt)
        .putString(MediaMetadataCompat.METADATA_KEY_ARTIST,activeAudio.getArtist())
        .putString(MediaMetadataCompat.METADATA_KEY_ALBUM,activeAudio.getAlbum())
        .putString(MediaMetadataCompat.METADATA_KEY_TITLE,activeAudio.getTitle())
        .build());
    }

    private void handleIncomingActions(Intent playbackAction){
        if (playbackAction == null || playbackAction.getAction() == null ) return;
        String actionString = playbackAction.getAction();
        if (actionString.equalsIgnoreCase(ACTION_PLAY)){
            transportControls.play();
        }else if (actionString.equalsIgnoreCase(ACTION_PAUSE)) {
            transportControls.pause();
        } else if (actionString.equalsIgnoreCase(ACTION_NEXT)) {
            transportControls.skipToNext();
        } else if (actionString.equalsIgnoreCase(ACTION_PREVIOUS)) {
            transportControls.skipToPrevious();
        } else if (actionString.equalsIgnoreCase(ACTION_STOP)) {
            transportControls.stop();
        }
    }

    @Override
    public void onPrepared(MediaPlayer mp) {
        playMedia();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if(mediaPlayer != null){
            stopMedia();
            mediaPlayer.release();
        }
        removeAudioFocus();
        removeNotification();
        unregisterReceiver(playNewAudio);
        new MySharedPreference(getApplicationContext()).clearCachedAudioPlaylist();
    }

    @Override
    public void onBufferingUpdate(MediaPlayer mp, int percent) {

    }

    @Override
    public void onCompletion(MediaPlayer mp) {
        stopMedia();
        removeNotification();
        stopSelf();
    }

    @Override
    public boolean onError(MediaPlayer mp, int what, int extra) {
        switch (what){
            case MediaPlayer.MEDIA_ERROR_NOT_VALID_FOR_PROGRESSIVE_PLAYBACK:
                Log.d("MediaPlayer Error", "MEDIA ERROR NOT VALID FOR PROGRESSIVE PLAYBACK " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_SERVER_DIED:
                Log.d("MediaPlayer Error", "MEDIA ERROR SERVER DIED " + extra);
                break;
            case MediaPlayer.MEDIA_ERROR_UNKNOWN:
                Log.d("MediaPlayer Error", "MEDIA ERROR UNKNOWN " + extra);
                break;
        }
        return false;
    }

    @Override
    public boolean onInfo(MediaPlayer mp, int what, int extra) {
        return false;
    }

    @Override
    public void onSeekComplete(MediaPlayer mp) {
        stopMedia();
        stopSelf();
    }

    @Override
    public void onAudioFocusChange(int focusChange) {
        switch (focusChange){
            case AudioManager.AUDIOFOCUS_GAIN:
                // resume playback
                if (mediaPlayer == null) initMediaPlayer();
                else if (!mediaPlayer.isPlaying()) mediaPlayer.start();
                mediaPlayer.setVolume(1.0f, 1.0f);
                break;
            case AudioManager.AUDIOFOCUS_LOSS:
                // Lost focus for an unbounded amount of time: stop playback and release media player
                if (mediaPlayer.isPlaying()) mediaPlayer.stop();
                mediaPlayer.release();
                mediaPlayer = null;
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                // Lost focus for a short time, but we have to stop
                // playback. We don't release the media player because playback
                // is likely to resume
                if (mediaPlayer.isPlaying()) mediaPlayer.pause();
                break;
            case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                // Lost focus for a short time, but it's ok to keep playing
                // at an attenuated level
                if (mediaPlayer.isPlaying()) mediaPlayer.setVolume(0.1f, 0.1f);
                break;
        }

    }


    public class LocalBinder extends Binder {
        MyService getService(){
            return MyService.this;
        }
    }
}
