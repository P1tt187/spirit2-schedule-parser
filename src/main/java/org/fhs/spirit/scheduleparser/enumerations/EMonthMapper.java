package org.fhs.spirit.scheduleparser.enumerations;

import java.util.Arrays;
import java.util.Calendar;
import java.util.Optional;

/**
 * @author fabian
 *         on 02.09.15.
 */
public enum EMonthMapper {

    JANUARY("Januar", Calendar.JANUARY),

    FEBRUARY("Februar", Calendar.FEBRUARY),

    MARCH("März", Calendar.MARCH),

    APRIL("April", Calendar.APRIL),

    MAY("Mai", Calendar.MAY),

    JUNE("Juni", Calendar.JUNE),

    JULY("Juli", Calendar.JULY),

    AUGUST("August", Calendar.AUGUST),

    SEPTEMBER("September", Calendar.SEPTEMBER),

    OCTOBER("Oktober", Calendar.OCTOBER),

    NOVEMBER("November", Calendar.NOVEMBER),

    DECEMBER("Dezember", Calendar.DECEMBER)
    ;


    private String germanName;

    private Integer monthconstant;

    public String getGermanName() {
        return germanName;
    }

    public Integer getMonthconstant() {
        return monthconstant;
    }

    EMonthMapper(String germanName, Integer monthconstant) {
        this.germanName = germanName;
        this.monthconstant = monthconstant;
    }

    public static Optional<EMonthMapper> findByName(String name){
      return  Arrays.asList(EMonthMapper.values()).stream().filter(m -> name.equalsIgnoreCase(m.germanName)).findFirst();
    }
}
