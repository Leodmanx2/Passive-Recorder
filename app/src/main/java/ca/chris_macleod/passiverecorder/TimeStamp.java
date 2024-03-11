package ca.chris_macleod.passiverecorder;

import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

class TimeStamp {
    private final Matcher matcher;

    TimeStamp(CharSequence sequence) {
        // For the humans among us: does the string look roughly like HH:MM:SS.mmm?
        Pattern pattern = Pattern.compile("^(0[0-9]|1[0-9]|2[0-3]):([0-5][0-9])(?::([0-5][0-9]))?(?:\\.([0-9]{1,3}))?$");
        matcher = pattern.matcher(sequence);
    }

    boolean valid() {
        return matcher.matches();
    }

    @SuppressWarnings("ConstantConditions")
    long toMilliseconds() {
        final String hours = matcher.group(1);
        final String minutes = matcher.group(2);
        final String seconds = matcher.group(3);
        final String milliseconds = matcher.group(4);
        long sum = TimeUnit.HOURS.toMillis(Long.parseLong(hours));
        sum += TimeUnit.MINUTES.toMillis(Long.parseLong(minutes));
        if (seconds != null) {
            sum += TimeUnit.SECONDS.toMillis(Long.parseLong(seconds));
        }
        if (milliseconds != null) {
            sum += Long.parseLong(milliseconds);
        }
        return sum;
    }
}
