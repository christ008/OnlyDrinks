package uy.christian.inventory;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class ProductTest {

    @Test
    void testEquals() {
        Product p1 = new Product();
        p1.setId(1L);
        Product p2 = new Product();
        p2.setId(1L);
        Product p3 = new Product();
        p3.setId(2L);
        
        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1, p1);
        assertNotEquals(null, p1);
        assertNotEquals(new Object(), p1);
    }

    @Test
    void testEqualsWithNullId() {
        Product p1 = new Product();
        Product p2 = new Product();
        
        assertEquals(p1, p1);
        assertNotEquals(p1, p2);
    }

    @Test
    void testHashCode() {
        Product p1 = new Product();
        p1.setId(1L);
        Product p2 = new Product();
        p2.setId(1L);
        
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
