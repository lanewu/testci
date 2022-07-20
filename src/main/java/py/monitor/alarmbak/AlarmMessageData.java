package py.monitor.alarmbak;

import java.io.Serializable;

/**
 * 
 * @author sxl
 * 
 */
public class AlarmMessageData implements Serializable {
    private static final long serialVersionUID = 1L;

    public enum AlarmOper {
        APPEAR, DISAPPEAR
    }

    String alarmObject;
    String alarmName;
    AlarmLevel alarmLevel;
    private AlarmOper oper;
    String alarmDescription;

    public String getAlarmName() {
        return alarmName;
    }

    public void setAlarmName(String alarmName) {
        this.alarmName = alarmName;
    }

    public AlarmLevel getAlarmLevel() {
        return alarmLevel;
    }

    public void setAlarmLevel(AlarmLevel alarmLevel) {
        this.alarmLevel = alarmLevel;
    }

    public String getAlarmDescription() {
        return alarmDescription;
    }

    public void setAlarmDescription(String alarmDescription) {
        this.alarmDescription = alarmDescription;
    }

    public String getAlarmObject() {
        return alarmObject;
    }

    public void setAlarmObject(String alarmObject) {
        this.alarmObject = alarmObject;
    }

    public AlarmOper getOper() {
        return oper;
    }

    public void setOper(AlarmOper oper) {
        this.oper = oper;
    }

    @Override
    public String toString() {
        return "AlarmMessageData [alarmObject=" + alarmObject + ", alarmName=" + alarmName + ", alarmLevel="
                + alarmLevel + ", oper=" + oper + ", alarmDescription=" + alarmDescription + "]";
    }

}
