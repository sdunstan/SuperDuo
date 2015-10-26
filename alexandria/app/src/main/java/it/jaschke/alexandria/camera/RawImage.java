package it.jaschke.alexandria.camera;

public final class RawImage {

    private final byte[] data;
    private final int height;
    private final int width;

    public RawImage(byte[] theData, int aWidth, int aHeight) {
        data = theData;
        width = aWidth;
        height = aHeight;
    }

    /**
     * We need to rotate the image 90 degrees when in portriat mode. Rotate code from:
     * http://stackoverflow.com/a/15775173/3745787
     */
    public RawImage rotate() {
        int imgSz = width*height;
        byte[] yuv = new byte[imgSz*3/2];

        int i = 0;
        for (int x = 0; x < width; x++) {
            for (int y = height-1; y >= 0; y--) {
                yuv[i] = data[y*width+x];
                i++;
            }
        }

        i = imgSz*3/2-1;
        for (int x = width-1; x > 0; x=x-2) {
            for(int y = 0; y < height/2; y++) {
                yuv[i] = data[imgSz+(y*width)+x];
                i--;
                yuv[i] = data[imgSz+(y*width)+(x-1)];
                i--;
            }
        }

        return new RawImage(yuv, width, height);
    }

    public byte[] getData() {
        return data;
    }

    public int getHeight() {
        return height;
    }

    public int getWidth() {
        return width;
    }

}
