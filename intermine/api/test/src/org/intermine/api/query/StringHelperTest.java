package org.intermine.api.query;

import java.util.Arrays;

import org.intermine.model.testmodel.Employee;

public class StringHelperTest extends IntHelperTest {

    @Override
    public void setRange() {
        this.ranges = Arrays.asList("B .. C");
    }
    
    @Override
    public void setPath() {
        this.path = "Employee.end";
    }
    
    @Override
    public void showEmployee(Employee e) {
        System.out.printf("%s: %s\n", e.getName(), e.getEnd());
    }
    
    @Override
    public void setExpectations() {
        this.withinExp = 144; // 12 ** 2
    }

}
