package org.apache.bookkeeper.my;

import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.Assertions;

public class CalculatorTest {
    private static Calculator calculator;
    
    @Before
    public void setUp(){
        calculator = new Calculator();
    }
    @Test
    public void addSimple(){
        double result = calculator.calculate(1,1,'+');
        Assertions.assertEquals(2,result);
    }
    @Test
    public void multiplySimple(){
        double result = calculator.calculate(1,1,'*');
        Assertions.assertEquals(1,result);
    }
    @Test
    public void subtractSimple(){
        double result = calculator.calculate(1,1,'-');
        Assertions.assertEquals(0,result);
    }
    @Test
    public void divideSimple(){
        double result = calculator.calculate(1,1,':');
        Assertions.assertEquals(1,result);
    }
}
