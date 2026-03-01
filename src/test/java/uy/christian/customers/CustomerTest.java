package uy.christian.customers;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class CustomerTest {

    @Test
    void testEquals() {
        Customer c1 = new Customer();
        c1.setId(1L);
        Customer c2 = new Customer();
        c2.setId(1L);
        Customer c3 = new Customer();
        c3.setId(2L);
        
        assertEquals(c1, c2);
        assertNotEquals(c1, c3);
        assertEquals(c1, c1);
        assertNotEquals(null, c1);
    }

    @Test
    void testEqualsWithNullId() {
        Customer c1 = new Customer();
        Customer c2 = new Customer();
        
        assertEquals(c1, c1);
        assertNotEquals(c1, c2);
    }
}
