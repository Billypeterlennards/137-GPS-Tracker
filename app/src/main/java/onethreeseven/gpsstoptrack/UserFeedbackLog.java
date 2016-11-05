package onethreeseven.gpsstoptrack;


import android.location.Location;
import android.widget.TextView;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

/**
 * A log that indicates to the user what the app is doing.
 * I.e "Recording started..."
 */
class UserFeedbackLog {

    private final TextView uiWidget;
    private final StringBuilder sb;
    private static final DateFormat dateFmt = new SimpleDateFormat(" (HH:mm:ss)", Locale.getDefault());

    UserFeedbackLog(TextView uiWidget){
        this.uiWidget = uiWidget;
        this.sb = new StringBuilder();
    }

    void logMovementChanged(boolean isStopped){
        log("You are " + (isStopped ? "stopped" : "moving"));
    }

    void logRecordingChanged(boolean started){
        log((started ? "Started" : "Stopped") + " recording");
    }

    void logRecordingIntervalChanged(int recordingInterval){
        log("Recording interval set to " + recordingInterval + "s");
    }

    void logLocationChanged(Location loc){
        log("Got location, accuracy: " + loc.getAccuracy() + "m");
    }

    void logPermissionsRequired(){
        log("You need to accept that permissions pop-up");
    }

    void logReady(){
        log("Ready");
    }

    void logAcquiringLocation(){
        log("Acquiring location");
    }

    private void log(String msg){
        sb.append("Log: ");
        sb.append(msg);
        sb.append(dateFmt.format(Calendar.getInstance().getTime()));
        uiWidget.setText(sb.toString());
        sb.setLength(0);
    }

}
