package py.monitor.task;

import java.io.Serializable;
import py.common.Utils;

import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * This class is use to create a a struct map to the thrift interface struct {@code TTimeSpan}
 * 
 * @author sxl
 * 
 */
public class TimeSpan implements Serializable {
    @JsonIgnore
    private static final long serialVersionUID = 1015193438021285786L;

    private long startTime;
    private long stopTime;

    public TimeSpan(long startTime, long stopTime) {
        super();
        this.startTime = startTime;
        this.stopTime = stopTime;
    }

    public TimeSpan() {
        super();
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getStopTime() {
        return stopTime;
    }

    public void setStopTime(long stopTime) {
        this.stopTime = stopTime;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + (int) (startTime ^ (startTime >>> 32));
        result = prime * result + (int) (stopTime ^ (stopTime >>> 32));
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        TimeSpan other = (TimeSpan) obj;
        if (startTime != other.startTime)
            return false;
        if (stopTime != other.stopTime)
            return false;
        return true;
    }

    @Override
    public String toString() {
        return "TimeSpan [startTime=" + "(" + startTime + ")" + Utils.millsecondToString(startTime) + ", stopTime="
                + "(" + stopTime + ")" + Utils.millsecondToString(stopTime) + "]";
    }

}
