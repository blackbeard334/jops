package neo.idlib.math.Matrix;

import neo.idlib.math.Math_h;
import neo.idlib.math.Simd;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;

import static org.junit.Assert.assertEquals;

public class BlaTest {
    private idMatX a;
    private idMatX b;

    @Before
    public void setUp() {
        Simd.idSIMD.Init();
        Math_h.idMath.Init();

        a = new idMatX(2, 2, new float[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4});
        b = new idMatX(2, 2, new float[]{1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4, 1, 2, 3, 4});
    }

    @Test
    public void bla() {
        idMatX c = a + b;
        assertEquals(2, c.oGet(0, 0), 0);
        assertEquals(4, c.oGet(0, 1), 0);
        assertEquals(6, c.oGet(1, 0), 0);
        assertEquals(8, c.oGet(1, 1), 0);
    }

    @Test
    public void blaParens() {
        idMatX c = (a) + (b);
        assertEquals(2, c.oGet(0, 0), 0);
        assertEquals(4, c.oGet(0, 1), 0);
        assertEquals(6, c.oGet(1, 0), 0);
        assertEquals(8, c.oGet(1, 1), 0);
    }

    @Test
    public void blaMethodOfOverloadedResult() {
        idMatX c = (a + b).Transpose();
        assertEquals(2, c.oGet(0, 0), 0);
        assertEquals(6, c.oGet(0, 1), 0);
        assertEquals(4, c.oGet(1, 0), 0);
        assertEquals(8, c.oGet(1, 1), 0);
    }
}
