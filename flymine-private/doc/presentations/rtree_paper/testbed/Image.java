import java.io.BufferedOutputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

public class Image
{
    private int left;
    private List<int[]> pixels;
    
    public Image(int left, List<int[]> pixels) {
        this.left = left;
        this.pixels = pixels;
    }

    public int getLeft() {
        return left;
    }

    public List<int[]> getPixels() {
        return pixels;
    }

    public int getWidth() {
        return pixels.get(0).length;
    }

    public int getHeight() {
        return pixels.size();
    }

    public void writeImage(String fileName) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 262144);
        bos.write(("P6 " + getWidth() + " " + getHeight() + " 255\n").getBytes());
        for (int[] row : pixels) {
            for (int pixel : row) {
                bos.write((pixel / 65536) & 0xFF);
                bos.write((pixel / 256) & 0xFF);
                bos.write(pixel & 0xFF);
            }
        }
        bos.flush();
        bos.close();
    }

    public void writeImage(String fileName, int width) throws IOException {
        FileOutputStream fos = new FileOutputStream(fileName);
        BufferedOutputStream bos = new BufferedOutputStream(fos, 262144);
        bos.write(("P6 " + width + " " + getHeight() + " 255\n").getBytes());
        for (int[] row : pixels) {
            for (int i = 0; i < left; i++) {
                bos.write(0xFF);
                bos.write(0xFF);
                bos.write(0xFF);
            }
            for (int pixel : row) {
                bos.write((pixel / 65536) & 0xFF);
                bos.write((pixel / 256) & 0xFF);
                bos.write(pixel & 0xFF);
            }
            for (int i = left + getWidth(); i < width; i++) {
                bos.write(0xFF);
                bos.write(0xFF);
                bos.write(0xFF);
            }
        }
        bos.flush();
        bos.close();
    }
}
