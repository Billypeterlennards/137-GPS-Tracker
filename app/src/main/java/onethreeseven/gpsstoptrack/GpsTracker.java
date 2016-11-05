package onethreeseven.gpsstoptrack;


import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.util.Log;
import java.util.ArrayList;

/**
 * The GPS tracker tracking lat,lon,time,movement state
 */
public class GpsTracker implements LocationListener {

    private final LocationManager locationManager;
    private final ArrayList<LocationConsumer> locationConsumers;

    private Location lastProcessedLocation = null;
    private Location curBestLocation = null;
    private long reportingInterval = 1000L;

    public GpsTracker(LocationManager locationManager) {
        this.locationConsumers = new ArrayList<>();
        this.locationManager = locationManager;
    }

    /**
     * Start tracking using the given recording interval.
     * In practice we try to query for a location roughly 3x more often than this interval.
     * However, it is not guaranteed we can poll that fast so don't worry about specifying too small a time.
     * The one thing that is guaranteed though is that if the "reportingInterval" amount of time has
     * elapsed between the last recording and the current one then the location is sent to the listeners.
     * @param reportingInterval the required amount of time between reporting a location (millis)
     */
    public void startTracking(long reportingInterval) {
        this.reportingInterval = reportingInterval;
        //we try to record at 3x the interval to get better sampling
        //(though is is not guaranteed we can poll that fast)
        long samplingInterval = (long) (reportingInterval * 0.3);

        try{
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, samplingInterval, 0, this);
            locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, samplingInterval, 0, this);
        }catch (SecurityException ex){
            ex.printStackTrace();
        }
    }

    public void stopTracking(){
        this.lastProcessedLocation = null;
        this.curBestLocation = null;
        try{
            locationManager.removeUpdates(this);
        }catch (SecurityException ex){
            ex.printStackTrace();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        Log.i(GpsTracker.class.getSimpleName(), location.toString());
        if(lastProcessedLocation == null){
            this.lastProcessedLocation =  location;
        }
        if(curBestLocation == null){
            this.curBestLocation = location;
        }
        this.curBestLocation = pickBetter(this.curBestLocation, location);
        //check whether the recording interval has elapsed
        long timeDelta = this.curBestLocation.getTime() - this.lastProcessedLocation.getTime();
        if(timeDelta >= reportingInterval){
            processLocation();
        }
    }

    /**
     * Adds a location consumer that will be updated once the tracker has been started.
     * Locations will be sent to the consumer if there is a valid location and the specified
     * amount of time has elapsed since the last location update.
     * The amount of time (seconds) is specified in {@link TrackerState#setRecordingInterval(int)}.
     * @param locationConsumer The location consumer to add.
     */
    public void addLocationConsumer(LocationConsumer locationConsumer){
        this.locationConsumers.add(locationConsumer);
    }

    @Override
    public void onStatusChanged(String s, int i, Bundle bundle) {

    }

    @Override
    public void onProviderEnabled(String s) {

    }

    @Override
    public void onProviderDisabled(String s) {

    }

    /////////////////////
    //private methods
    /////////////////////

    private Location pickBetter(Location a, Location b){
        double scoreA = scoreRecording(a);
        double scoreB = scoreRecording(b);
        return (scoreA < scoreB) ? a : b;
    }

    private void processLocation(){
        this.lastProcessedLocation = curBestLocation;
        for (LocationConsumer locationConsumer : locationConsumers) {
            locationConsumer.consume(this.curBestLocation);
        }
    }

    /**
     * Score a recorded location.
     * @param location A recorded location.
     * @return A score (lower is better).
     */
    private double scoreRecording(Location location){
        long timeDelta = location.getTime() - this.lastProcessedLocation.getTime();
        double tScore = 1 - timeDelta / reportingInterval;
        //weight the location accuracy (meters, bigger is worse) with how far away from interval it is
        return location.getAccuracy() * tScore;
    }

}
