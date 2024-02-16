package mekanism.common.base.holiday;

import java.util.Arrays;
import net.minecraft.network.chat.Component;

record HolidayMessage(Component themedLines, Component... lines) {

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        return o instanceof HolidayMessage other && themedLines.equals(other.themedLines) && Arrays.equals(lines, other.lines);
    }

    @Override
    public int hashCode() {
        int result = themedLines.hashCode();
        result = 31 * result + Arrays.hashCode(lines);
        return result;
    }
}