package onethreeseven.gpsstoptrack;


import android.content.Context;
import android.os.Bundle;

/**
 * The application (tracker) state.
 * I.e Whether the tracker is recording, whether the user is stopped or moving, and the recording
 * interval in second. The state is stored by wrapping a bundle.
 */
class TrackerState {

    private final String IS_RECORDING;
    private static final boolean DEFAULT_IS_RECORDING = false;

    private final String IS_STOPPED;
    private static final boolean DEFAULT_IS_STOPPED = false;

    private final String RECORDING_INTERVAL;
    private static final int DEFAULT_RECORDING_INTERVAL = 1;

    private final Bundle bundle;

    TrackerState(Context context){
        this.bundle = new Bundle();
        this.IS_RECORDING = context.getString(R.string.key_is_recording);
        this.IS_STOPPED = context.getString(R.string.key_is_stopped);
        this.RECORDING_INTERVAL = context.getString(R.string.key_recording_interval);
        setIsRecording(DEFAULT_IS_RECORDING);
        setIsStopped(DEFAULT_IS_STOPPED);
        setRecordingInterval(DEFAULT_RECORDING_INTERVAL);
    }

    public TrackerState(Context context, Bundle otherBundle){
        this(context);
        setFrom(otherBundle);
    }

    private void setFrom(Bundle otherBundle){
        if(otherBundle == null){return;}
        if(otherBundle.containsKey(IS_RECORDING)){
            this.bundle.putBoolean(IS_RECORDING, otherBundle.getBoolean(IS_RECORDING));
        }
        if(otherBundle.containsKey(IS_STOPPED)){
            this.bundle.putBoolean(IS_STOPPED, otherBundle.getBoolean(IS_STOPPED));
        }
        if(otherBundle.containsKey(RECORDING_INTERVAL)){
            this.bundle.putInt(RECORDING_INTERVAL, otherBundle.getInt(RECORDING_INTERVAL));
        }
    }

    void setIsRecording(boolean isRecording){
        this.bundle.putBoolean(IS_RECORDING, isRecording);
    }

    boolean isRecording(){
        return this.bundle.getBoolean(IS_RECORDING, DEFAULT_IS_RECORDING);
    }

    boolean isStopped(){
        return this.bundle.getBoolean(IS_STOPPED, DEFAULT_IS_STOPPED);
    }

    void setIsStopped(boolean isStopped){
        this.bundle.putBoolean(IS_STOPPED, isStopped);
    }

    int getRecordingInterval(){
        return this.bundle.getInt(RECORDING_INTERVAL, DEFAULT_RECORDING_INTERVAL);
    }

    void setRecordingInterval(int seconds){
        this.bundle.putInt(RECORDING_INTERVAL, seconds);
    }

    public Bundle getBundle(){
        return this.bundle;
    }

}
