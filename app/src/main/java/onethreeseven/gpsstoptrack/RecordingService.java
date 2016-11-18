package onethreeseven.gpsstoptrack;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.support.annotation.Nullable;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.NotificationCompat;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.widget.RemoteViews;

import java.io.File;

/**
 * Service that tracks location while it is recording
 */
public class RecordingService extends Service {

    private static final int LOCATION_FOREGROUND_SERVICE = 99;
    private static final int MOVEMENT_CHANGED_INTENT_CODE = 13;

    private final BroadcastReceiver movementChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(trackerState == null){return;}
            //toggle whatever the tracker state is
            handleIsStoppedChanged(!trackerState.isStopped());

            Intent toggleMovementIntent = new Intent(getString(R.string.receiver_activity_toggle_should_change));
            toggleMovementIntent.putExtras(trackerState.getBundle());
            LocalBroadcastManager.getInstance(context).sendBroadcast(toggleMovementIntent);
            //RecordingService.this.sendBroadcast(toggleMovementIntent);
        }
    };

    private boolean initialised = false;
    private TrackerState trackerState = null;
    private GpsTracker gpsTracker = null;
    private TrailWriter trailWriter = null;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(RecordingService.class.getSimpleName(), "Started service with bundle " +
                ((intent.getExtras() == null) ? "null" : intent.getExtras().toString()));
        super.onStartCommand(intent, flags, startId);
        if(!initialised){
            init();
        }
        //parse parameters passed in as extras
        Bundle extras = intent.getExtras();
        if(extras != null){
            handleIsRecordingChanged(extras);
            handleIsStoppedChanged(extras);
            handleRecordingIntervalChanged(extras);
        }
        return START_NOT_STICKY;
    }

    private void init(){
        trackerState = new TrackerState(this);
        LocationManager lm = (LocationManager) this.getSystemService(Context.LOCATION_SERVICE);
        gpsTracker = new GpsTracker(lm);
        //listen for changes in state from the custom notification
        registerReceiver(movementChangedReceiver,
                new IntentFilter(getString(R.string.receiver_movement_changed)));
        trailWriter = new TrailWriter(Environment.getExternalStorageDirectory());
        initialised = true;
    }

    private void handleIsRecordingChanged(boolean shouldStartRecording){
        boolean isCurrentlyRecording = trackerState.isRecording();
        if(shouldStartRecording != isCurrentlyRecording){
            trackerState.setIsRecording(shouldStartRecording);
            if(shouldStartRecording){
                startRecording();
            }else{
                stopRecording();
            }
        }
    }

    private void handleIsRecordingChanged(Bundle extras){
        String isRecordingKey = getString(R.string.key_is_recording);
        //change whether we are recording or not
        if(extras.containsKey(isRecordingKey)){
            boolean shouldStartRecording = extras.getBoolean(isRecordingKey);
            handleIsRecordingChanged(shouldStartRecording);
        }
    }

    private void handleIsStoppedChanged(boolean isStopped){
        trackerState.setIsStopped(isStopped);
        Log.i(RecordingService.class.getSimpleName(),
                "Changed to " + (isStopped ? "stopped" : "moving"));
        //update the notification if movement state is changed
        if(trackerState.isRecording()){
            updateNotification();
        }
    }

    private void handleIsStoppedChanged(Bundle extras){
        String isStoppedKey = getString(R.string.key_is_stopped);
        //change stopped or moving state
        if(extras.containsKey(isStoppedKey)){
            boolean isStopped = extras.getBoolean(isStoppedKey);
            handleIsStoppedChanged(isStopped);
        }
    }

    private void handleRecordingIntervalChanged(Bundle extras){
        String recordingIntervalKey = getString(R.string.key_recording_interval);
        //change the recording interval (seconds) state
        if(extras.containsKey(recordingIntervalKey)){
            int recordingInterval = extras.getInt(recordingIntervalKey);
            handleRecordingIntervalChanged(recordingInterval);
        }
    }

    private void handleRecordingIntervalChanged(int recordingInterval){
        trackerState.setRecordingInterval(recordingInterval);
        Log.i(RecordingService.class.getSimpleName(),
                "Recording interval set to " + recordingInterval);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(movementChangedReceiver);
        stopRecording();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void updateNotification(){
        Log.i(RecordingService.class.getSimpleName(), "Updating notification");
        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        notificationManager.notify(LOCATION_FOREGROUND_SERVICE, makeStatusBarNotification());
    }

    private Notification makeStatusBarNotification(){

        //make custom notification remote view
        RemoteViews customNotificationView = new RemoteViews(getPackageName(), R.layout.notification);

        customNotificationView.setImageViewResource(R.id.notificationIcon, R.mipmap.ic_launcher);
        //set different icon based on whether stopped or not
        if(trackerState.isStopped()){
            //customNotificationView.setIm
            customNotificationView.setImageViewResource(R.id.notificationMovementModeBtn, R.drawable.ic_walk_guy);
            customNotificationView.setTextViewText(R.id.notificationTitle, getString(R.string.notification_title_stopped));
        }else{
            customNotificationView.setImageViewResource(R.id.notificationMovementModeBtn, R.drawable.ic_run_guy);
            customNotificationView.setTextViewText(R.id.notificationTitle, getString(R.string.notification_title_moving));
        }
        //setup button onClick
        Intent movementToggleIntent = new Intent(getString(R.string.receiver_movement_changed));
        PendingIntent movementClickedIntent = PendingIntent.getBroadcast(this, MOVEMENT_CHANGED_INTENT_CODE,
               movementToggleIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        customNotificationView.setOnClickPendingIntent(R.id.notificationMovementModeBtn, movementClickedIntent);

        Intent notificationIntent = new Intent(this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        //pass back the current tracker state (so we can restore correctly to the activity)
        notificationIntent.putExtras(trackerState.getBundle());

        PendingIntent pendingIntent = PendingIntent.getActivity(
                this, LOCATION_FOREGROUND_SERVICE, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);



        Notification notification = new NotificationCompat.Builder(this)
                .setContent(customNotificationView)
                .setSmallIcon(R.drawable.ic_gps_icon)
                .setContentIntent(pendingIntent)
                .setCategory(NotificationCompat.CATEGORY_SERVICE)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .build();
        notification.flags |= Notification.FLAG_NO_CLEAR |
                Notification.FLAG_FOREGROUND_SERVICE |
                Notification.FLAG_ONGOING_EVENT;

        return notification;
    }

    private void startRecording(){
        Log.i(RecordingService.class.getSimpleName(), "Recording service started");
        startForeground(LOCATION_FOREGROUND_SERVICE, makeStatusBarNotification());
        //make a new file to write the trail to
        trailWriter.newTrailFile();
        //add a consumer to write each new location
        final String newLocationFoundAction = getString(R.string.receiver_new_location);
        final String locationKey = getString(R.string.key_location);
        final LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);

        gpsTracker.addLocationConsumer(new LocationConsumer() {
            @Override
            public void consume(Location location) {
                //broadcast the new location back to the main activity (to update the gui)
                Intent newLocationFoundIntent = new Intent(newLocationFoundAction);
                newLocationFoundIntent.putExtra(locationKey, location);
                localBroadcastManager.sendBroadcast(newLocationFoundIntent);
                //write the new location to the trail file
                trailWriter.write(location, trackerState.isStopped());
            }
        });
        gpsTracker.startTracking(trackerState.getRecordingInterval() * 1000L);
    }

    private void stopRecording(){
        Log.i(RecordingService.class.getSimpleName(), "Recording service stopped");

        //end recording from gps tracker
        gpsTracker.stopTracking();
        //write the trail file
        File trailFile = trailWriter.closeTrailFile();
        //make file discoverable by android
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(trailFile));
        this.getApplicationContext().sendBroadcast(intent);

        //stop this service
        stopForeground(true);
    }

}
