package onethreeseven.gpsstoptrack;

import android.Manifest;
import android.animation.ValueAnimator;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.PorterDuff;
import android.graphics.drawable.Drawable;
import android.location.Location;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.content.res.AppCompatResources;
import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.CompoundButton;
import android.widget.ImageButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.ToggleButton;

public class MainActivity extends AppCompatActivity {

    private UserFeedbackLog userFeedbackLog;
    private boolean permissionsGranted = false;

    private final ValueAnimator recordBtnAnim = ValueAnimator.ofFloat(0,1);
    private int endColor;
    private int endColorRed;
    private int startColor;
    private int startColorRed;

    private BroadcastReceiver movementChangedReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Log.i(MainActivity.class.getSimpleName(), "Broadcast to change movement toggle ui received");
            Bundle bundle = intent.getExtras();
            if(bundle == null){return;}
            setupMovementToggle(bundle);
        }
    };

    private BroadcastReceiver newLocationReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if(userFeedbackLog != null){
                String locationKey = getString(R.string.key_location);
                Location location = intent.getParcelableExtra(locationKey);
                if(location == null){return;}
                userFeedbackLog.logLocationChanged(location);
            }
        }
    };

    private static final int PERMISSIONS_REQUEST_CODE = 137;
    private static final String[] manifestPermissions = new String[]{
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Log.i(MainActivity.class.getSimpleName(), "Created activity");
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        userFeedbackLog = new UserFeedbackLog((TextView) findViewById(R.id.statusText));
        //acquire permission for fine location tracking
        acquirePermissions();
        //if they are already granted, do the setup
        if(permissionsGranted){
            setupApp(getIntent().getExtras());
        }else {
            userFeedbackLog.logPermissionsRequired();
        }
    }

    @Override
    protected void onPause() {
        Log.i(MainActivity.class.getSimpleName(),"Paused");
        super.onPause();
    }

    @Override
    protected void onStart() {
        Log.i(MainActivity.class.getSimpleName(), "onStart");
        super.onStart();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        //register receiver
        localBroadcastManager.registerReceiver(movementChangedReceiver,
                new IntentFilter(getString(R.string.receiver_activity_toggle_should_change)));
        localBroadcastManager.registerReceiver(newLocationReceiver,
                new IntentFilter(getString(R.string.receiver_new_location)));
    }

    @Override
    protected void onStop() {
        Log.i(MainActivity.class.getSimpleName(), "onStop");
        super.onStop();
        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(this);
        localBroadcastManager.unregisterReceiver(movementChangedReceiver);
        localBroadcastManager.unregisterReceiver(newLocationReceiver);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        Log.i(MainActivity.class.getSimpleName(), "New intent called.");
        super.onNewIntent(intent);
        setupApp(intent.getExtras());
    }

    private void setupApp(Bundle bundle){
        Log.i(MainActivity.class.getSimpleName(), "Setting up activity ui with bundle: "
                + (bundle == null ? "null" : bundle.toString()) );
        setupRecordingButton(bundle);
        setupMovementToggle(bundle);
        setupRecordingIntervalSpinner(bundle);
        this.userFeedbackLog.logReady();
    }

    private void setupRecordingIntervalSpinner(Bundle bundle){

        final Spinner intervalSpinner = (Spinner) findViewById(R.id.spinner);
        intervalSpinner.setOnItemSelectedListener(null);
        intervalSpinner.setSelection(0, false);

        final String keyRecordingInterval = getString(R.string.key_recording_interval);
        Log.i(MainActivity.class.getSimpleName(), "Bundle has recording interval key: " +
                (bundle != null && bundle.containsKey(keyRecordingInterval)));
        int recordingInterval = bundle != null ? bundle.getInt(keyRecordingInterval, 1) : 1;

        //set the current selected interval based on the tracker state
        for (int i = 0; i < intervalSpinner.getCount(); i++) {
            String intervalStr = intervalSpinner.getItemAtPosition(i).toString();
            int curInterval = getSecondsFromString(intervalStr);
            if(curInterval == recordingInterval){
                intervalSpinner.setSelection(i, false);
                break;
            }
        }

        //add selected item listener that updates the recording service
        intervalSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
                Object item = intervalSpinner.getItemAtPosition(i);
                String itemStr = item.toString();
                //strip the "s" off the end and set the recording interval
                changeRecordingInterval(getSecondsFromString(itemStr));
            }
            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {}
        });
    }
    
    private int getSecondsFromString(String secondsString){
        secondsString = secondsString.replaceAll("s","");
        return Integer.parseInt(secondsString);
    }
    

    private void setupMovementToggle(Bundle bundle){
        final ToggleButton movementToggle = (ToggleButton) findViewById(R.id.stopMoveToggle);
        final String keyIsStopped = getString(R.string.key_is_stopped);

        final boolean bundleContainsKeyIsStopped = bundle != null && bundle.containsKey(keyIsStopped);
        final boolean isStopped = bundle != null && bundle.getBoolean(keyIsStopped, false);
        Log.i(MainActivity.class.getSimpleName(), "Bundle has isStopped key: " + bundleContainsKeyIsStopped);
        if(bundleContainsKeyIsStopped){
            userFeedbackLog.logMovementChanged(isStopped);
        }

        movementToggle.setCompoundDrawablesWithIntrinsicBounds(null, null, getStopMoveIcon(isStopped), null);

        movementToggle.setOnCheckedChangeListener(null);
        movementToggle.setChecked(!isStopped);
        movementToggle.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton compoundButton, boolean isMoving) {
                if(isMoving){
                    movementToggle.setCompoundDrawablesWithIntrinsicBounds(null, null, getStopMoveIcon(false), null);
                }else{
                    movementToggle.setCompoundDrawablesWithIntrinsicBounds(null, null, getStopMoveIcon(true), null);
                }
                changeIsStopped(!isMoving);
            }
        });
    }

    private Drawable getStopMoveIcon(boolean isStopped){
        return isStopped ?
                AppCompatResources.getDrawable(this, R.drawable.ic_directions_walk_black_24dp) :
                AppCompatResources.getDrawable(this, R.drawable.ic_directions_run_black_24dp);
    }

    private void setupRecordBtnAnimator(){

        endColor = ContextCompat.getColor(getBaseContext(), R.color.recordBtnEndRed);
        endColorRed = Color.red(endColor);
        startColor = ContextCompat.getColor(getBaseContext(), R.color.recordBtnStartRed);
        startColorRed = Color.red(startColor);

        final ImageButton recordBtn = (ImageButton) findViewById(R.id.recordingBtn);
        //setup color tint animator (so it flashes red when button is pressed)
        recordBtnAnim.setRepeatMode(ValueAnimator.REVERSE);
        recordBtnAnim.setRepeatCount(ValueAnimator.INFINITE);
        recordBtnAnim.setDuration(500);

        recordBtnAnim.removeAllUpdateListeners();

        recordBtnAnim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                float t = (Float)valueAnimator.getAnimatedValue();
                int r = (int) (((endColorRed - startColorRed) * t) + startColorRed);
                int c = Color.argb(255, r, 0, 0);
                tintBtn(recordBtn, c);
            }
        });
    }

    private void setupRecordingButton(Bundle bundle){

        setupRecordBtnAnimator();

        final ImageButton recordBtn = (ImageButton) findViewById(R.id.recordingBtn);
        tintBtn(recordBtn, startColor);

        final String keyIsRecording = getString(R.string.key_is_recording);
        Log.i(MainActivity.class.getSimpleName(), "Bundle has isRecording key: " +
                (bundle != null && bundle.containsKey(keyIsRecording)));
        final boolean isRecording = bundle != null && bundle.getBoolean(keyIsRecording, false);
        Log.i(MainActivity.class.getSimpleName(), "Is recording: " + isRecording);

        //set the button animating and color based on the state in the bundle
        if(isRecording){
            recordBtnAnim.start();
        }else{
            recordBtnAnim.end();
            tintBtn(recordBtn, startColor);
        }

        //setup button press to record and stop recording
        recordBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if(recordBtnAnim.isRunning()){
                    endRecording();
                    //always set it the tint to black (as a starting or ending tint for the button)
                    tintBtn(recordBtn, startColor);
                }else{
                    startRecording();
                }
            }
        });
    }

    private void tintBtn(ImageButton btn, int color){
        btn.setColorFilter(color, PorterDuff.Mode.SRC_IN);
    }

    private void startRecording(){
        changeIsRecording(true);
        recordBtnAnim.start();
        //disable the spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setEnabled(false);
    }

    private void endRecording(){
        stopService(new Intent(this, RecordingService.class));
        recordBtnAnim.cancel();
        recordBtnAnim.end();
        //re-enable the spinner
        Spinner spinner = (Spinner) findViewById(R.id.spinner);
        spinner.setEnabled(true);
        //tell the user recording has finished
        userFeedbackLog.logRecordingChanged(false);
    }

    ////////////////////////////////
    //Permissions
    ////////////////////////////////

    private void acquirePermissions(){
        permissionsGranted = true;
        for (String manifestPermission : manifestPermissions) {
            if(ActivityCompat.checkSelfPermission(this, manifestPermission) != PackageManager.PERMISSION_GRANTED){
                permissionsGranted = false;
                userFeedbackLog.logPermissionsRequired();
                break;
            }
        }
        //try to request the permissions if they are not granted
        if(!permissionsGranted){
            ActivityCompat.requestPermissions(this, manifestPermissions, PERMISSIONS_REQUEST_CODE);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode == PERMISSIONS_REQUEST_CODE && grantResults.length > 0
                && grantResults[0] == PackageManager.PERMISSION_GRANTED){
            permissionsGranted = true;
            //restart this activity now we have permissions
            recreate();
        }
    }

    //////////////////////////////////////
    //Dealing with the recording service
    //////////////////////////////////////

    private void changeRecordingInterval(int recordingInterval){
        userFeedbackLog.logRecordingIntervalChanged(recordingInterval);
        Intent changeIntervalIntent = new Intent(this, RecordingService.class);
        changeIntervalIntent.putExtra(getString(R.string.key_recording_interval), recordingInterval);
        startService(changeIntervalIntent);
    }

    private void changeIsStopped(boolean isStopped){
        userFeedbackLog.logMovementChanged(isStopped);
        Intent changeIntervalIntent = new Intent(this, RecordingService.class);
        changeIntervalIntent.putExtra(getString(R.string.key_is_stopped), isStopped);
        startService(changeIntervalIntent);
    }

    private void changeIsRecording(boolean isRecording){
        userFeedbackLog.logRecordingChanged(isRecording);
        Intent changeIntervalIntent = new Intent(this, RecordingService.class);
        changeIntervalIntent.putExtra(getString(R.string.key_is_recording), isRecording);
        startService(changeIntervalIntent);
    }

}
