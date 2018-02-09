package riot.data;

import java.sql.Connection;

/**
 * Created by marianne on 24/01/17.
 */
public abstract class Data {
    long mTime;
    String mDeviceID;
    int mSequence;

    public abstract void insertIntoDatabase(Connection connection);

    public int getSequence() {
        return mSequence;
    }
}
