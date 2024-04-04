package org.ktximageio;

public enum Orientation {
    LEFT(0, new float[] { -1, 0, 0 }),
    RIGHT(1, new float[] { 1, 0, 0 }),
    TOP(2, new float[] { 0, 1, 0 }),
    BOTTOM(3, new float[] { 0, -1, 0 }),
    FRONT(4, new float[] { 0, 0, -1 }),
    BACK(5, new float[] { 0, 0, 1 });

    public final int face;
    public final float[] axis;

    Orientation(int f, float[] a) {
        face = f;
        axis = a;
    }

    public static Orientation get(int face) {
        for (Orientation o : values()) {
            if (o.face == face) {
                return o;
            }
        }
        return null;
    }

    /**
     * Returns the orientation values as Strings - this is useful for debugging purposes
     * 
     * @return An array containing the names of the orientations
     */
    public static String[] getOrientations() {
        Orientation[] values = values();
        String[] result = new String[values.length];
        int index = 0;
        for (Orientation o : values) {
            result[index++] = o.name();
        }
        return result;
    }

    private static final float[] DEFAULT_AXIS = new float[] { 0, 0, -1 };

    private static final int[][] AXIS_ALIGNMENT = new int[][] { { -1, 1, 1 }, { 1, 1, -1, }, { 1, 1, -1 }, { 1, -1, 1 },
            { 1, 1, 1 }, { -1, 1, -1 } };

    private static final int[][] INDEXES = new int[][] { { 2, 1, 0 }, { 2, 1, 0 }, { 0, 2, 1 }, { 0, 2, 1 },
            { 0, 1, 2 },
            { 0, 1, 2 } };

    /**
     * Returns the normal pointing to the specified x,y coordinate in the orientation using the specified
     * size as width and height.
     * 
     * @param x
     * @param y
     * @param size Dimension of square where the normal is projected to point toward x,y
     * @param xyz The normal pointing towards x,y in the orientation
     */
    public void getNormal(int x, int y, int size, float[] xyz) {
        int[] indexes = getIndexes();
        int[] alignment = getAxisAlignment();
        float u = (float) ((x - size / 2.0) / (size / 2.0)); /* u ranges from -1 to 1 */
        float v = (float) ((size / 2.0 - y) / (size / 2.0)); /* v ranges from -1 to 1 */
        float xAngle = (float) ((u * -(Math.PI / 4)));
        float yAngle = (float) ((v * (Math.PI / 4)));
        float[] rotated = new float[3];
        // Y axis rotation - this is the horizontal position
        rotated[0] = (float) (DEFAULT_AXIS[0] * Math.cos(xAngle) + DEFAULT_AXIS[2] * Math.sin(xAngle));
        rotated[1] = DEFAULT_AXIS[1];
        rotated[2] = (float) (-DEFAULT_AXIS[0] * Math.sin(xAngle) + DEFAULT_AXIS[2] * Math.cos(xAngle));

        // X axis rotation - this is the vertical position
        xyz[indexes[0]] = rotated[0] * alignment[0];
        xyz[indexes[1]] = (float) (rotated[1] * Math.cos(yAngle) - rotated[2] * Math.sin(yAngle)) * alignment[1];
        xyz[indexes[2]] = (float) (rotated[1] * Math.sin(yAngle) + rotated[2] * Math.cos(yAngle)) * alignment[2];
    }

    /**
     * Returns the axis alignment (positive or negative) for the orientation
     * 
     * @return
     */
    public int[] getAxisAlignment() {
        return AXIS_ALIGNMENT[face];
    }

    public int[] getIndexes() {
        return INDEXES[face];
    }

}
