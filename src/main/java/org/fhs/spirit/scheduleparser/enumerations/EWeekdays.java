package org.fhs.spirit.scheduleparser.enumerations;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

/**
 * @author fabian
 *         on 31.08.15.
 *         Mapps Weekays
 */
public enum EWeekdays {
    /**
     * Enum Constant
     */
    MONDAY("Montag"),
    /**
     * Enum Constant
     */
    TUESDAY("Dienstag"),
    /**
     * Enum Constant
     */
    WEDNESDAY("Mittwoch"),
    /**
     * Enum Constant
     */
    THURSDAY("Donnerstag"),
    /**
     * Enum Constant
     */
    FRIDAY("Freitag"),
    /**
     * Enum Constant
     */
    SATURDAY("Sonnabend"),
    /**
     * Enum Constant
     */
    SUNDAY("Sonntag");

    private String germanName;

    EWeekdays(String germanName) {
        this.germanName = germanName;
    }

    public String getGermanName() {
        return germanName;
    }

    public static Optional<EWeekdays> findConstantByName(String name) {
        List<EWeekdays> constants = Arrays.asList(EWeekdays.values());
        return constants.stream().filter(weekday -> name.equalsIgnoreCase(weekday.germanName) || name.equalsIgnoreCase(weekday.name())).findFirst();
    }

    public int indexOf(){
        EWeekdays[] values = EWeekdays.values();
        for(int i=0;i<values.length;i++){
            if(values[i] == this){
                return i;
            }
        }
        return -1;
    }
}
