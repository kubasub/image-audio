package imajadio;

import android.graphics.Color;

/**
 * @author Jakub Subczynski
 * @date April 12, 2014
 */
public class Pixel {
    private final short VERTICAL_OFFSET;
    private final int RED;
    private final int GREEN;
    private final int BLUE;

    public Pixel(short vertical_offset, int color) {
        VERTICAL_OFFSET = vertical_offset;
        RED = Color.red(color);
        GREEN = Color.green(color);
        BLUE = Color.blue(color);
    }
}
