package ca.chris_macleod.passiverecorder;

import org.junit.Assert;
import org.junit.jupiter.api.Test;

public class TimeStampTest {
    @org.junit.jupiter.api.Test
    public void correctConversion_HoursMinutes() {
        TimeStamp timestamp = new TimeStamp("11:09");
        Assert.assertEquals(40140000, timestamp.toMilliseconds());
    }

    @Test
    public void correctConversion_HoursMinutesSeconds() {
        TimeStamp timestamp = new TimeStamp("11:09:31");
        Assert.assertEquals(40171000, timestamp.toMilliseconds());
    }

    @org.junit.jupiter.api.Test
    public void correctConversion_HoursMinutesSecondsMilliseconds() {
        TimeStamp timestamp = new TimeStamp("11:09:31.120");
        Assert.assertEquals(40171120, timestamp.toMilliseconds());
    }
}
