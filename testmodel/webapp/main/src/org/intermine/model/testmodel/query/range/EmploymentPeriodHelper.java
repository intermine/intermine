package org.intermine.model.testmodel.query.range;

import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.TimeZone;

import org.intermine.api.query.range.AbstractHelper;
import org.intermine.api.query.range.ConstraintOptions;
import org.intermine.api.query.range.Range;
import org.intermine.objectstore.query.ConstraintOp;
import org.intermine.objectstore.query.QueryValue;
import org.intermine.util.TypeUtil;

public class EmploymentPeriodHelper extends AbstractHelper {

    @Override
    protected ConstraintOptions getWithinOptions() {
        return new ConstraintOptions(
            ConstraintOp.OR,
            ConstraintOp.AND,
            ConstraintOp.GREATER_THAN_EQUALS,
            ConstraintOp.LESS_THAN,
            "start",
            "end"
        );
    }
    
    @Override
    protected Range parseRange(String range) {
        return new DateRange(range);
    }
    
    private class DateRange implements Range
    {
        
        private Date start, end;
        
        DateRange(String range) {
            if (range == null) {
                throw new NullPointerException("range may not be null");
            }
            String[] parts = range.split("\\.\\.");
            String startStr, endStr;
            if (parts.length == 1) {
                startStr = parts[0];
                endStr = parts[0];
            } else if (parts.length == 2) {
                startStr = parts[0];
                endStr = parts[1];
            } else {
                throw new IllegalArgumentException("Illegal range: " + range);
            }
            Date startDate = (Date) TypeUtil.stringToObject(Date.class, startStr);
            Calendar startOfPeriod = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
            startOfPeriod.setTime(startDate);
            startOfPeriod.set(Calendar.HOUR_OF_DAY, 0);
            startOfPeriod.set(Calendar.MINUTE, 0);
            startOfPeriod.set(Calendar.SECOND, 0);
            startOfPeriod.set(Calendar.MILLISECOND, 0);
            this.start = startOfPeriod.getTime();
            
            Date endDate = (Date) TypeUtil.stringToObject(Date.class, endStr);
            Calendar endOfPeriod = GregorianCalendar.getInstance(TimeZone.getTimeZone("GMT"));
            endOfPeriod.setTime(endDate);
            endOfPeriod.set(Calendar.HOUR_OF_DAY, 0);
            endOfPeriod.set(Calendar.MINUTE, 0);
            endOfPeriod.set(Calendar.SECOND, 0);
            endOfPeriod.set(Calendar.MILLISECOND, 0);
            endOfPeriod.add(Calendar.DATE, 1);
            this.end = endOfPeriod.getTime();
        }
        
        @Override
        public Date getStart() {
            return start; 
        }
        
        @Override
        public Date getEnd() {
            return end;
        }
    }
    

}
