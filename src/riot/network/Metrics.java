package riot.network;

import java.util.ArrayList;

/**
 * TODO: Implement metrics class.
 * Created by marianne on 8/04/17.
 */
public class Metrics {
    class DeviceMetrics {
        String mDeviceName;

        // When no packets have been received, is -1.
        int mMostRecentSeq = -1;
        int mMissedPackets = 0;
        int mDuplicatedPackets = 0;
        int mOutOfOrderPackets = 0;
        int mTotalPackets = 0;
        ArrayList<Integer> mSeqNums = new ArrayList<Integer>();
    }
}
