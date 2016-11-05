package onethreeseven.gpsstoptrack;

import android.location.Location;

/**
 * Consumes {@link android.location.Location}.
 * @author luke
 */
public interface LocationConsumer {

    void consume(Location location);

}
