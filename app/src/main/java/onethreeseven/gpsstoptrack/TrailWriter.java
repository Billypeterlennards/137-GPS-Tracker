package onethreeseven.gpsstoptrack;

import android.location.Location;
import android.util.Log;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Locale;

/**
 * Write the location data to a file
 */
class TrailWriter  {

    private final File baseDir;
    private static final String entryFmt = "%f,%f,%d,%s";

    TrailWriter(File baseDir){
        this.baseDir = new File(baseDir, "trails");
        if(!this.baseDir.exists()){
            this.baseDir.mkdir();
        }
    }

    private File trailFile;
    private BufferedWriter bw;

    void newTrailFile(){
        trailFile = new File(baseDir, "trail_" + System.currentTimeMillis() + ".txt");
        Log.i(TrailWriter.class.getSimpleName(), "Made new file at: " + trailFile.getAbsolutePath());
        try {
            bw = new BufferedWriter(new FileWriter(trailFile));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    void write(Location location, boolean isStopped){
        if(bw != null){
            String entry = String.format(
                    Locale.ENGLISH,
                    entryFmt,
                    location.getLatitude(),
                    location.getLongitude(),
                    location.getTime(),
                    isStopped ? "Stopped" : "Moving");
            try {
                bw.write(entry);
                bw.newLine();
                Log.i(TrailWriter.class.getSimpleName(), "Wrote:" + entry);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    File closeTrailFile(){
        if(bw != null){
            try {
                bw.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return trailFile;
    }


}
