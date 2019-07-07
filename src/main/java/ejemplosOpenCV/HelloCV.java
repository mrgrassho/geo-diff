package ejemplosOpenCV

import org.opencv.imgproc.Imgproc;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Point;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;

public class HelloCV {
    public static void main(String[] args) throws IOException{
    		System.out.println("Processing img");
    		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
	    	Mat img = Imgcodecs.imread("NCAP-000-000-249-123.jpg");
	    	Mat hsv = new Mat();
	    	Mat img2 = new Mat();
	    	Mat img3 = new Mat();
	    	Mat img4 = new Mat();
	    	Mat blurredImage = new Mat();
	    	int erosion_size = 5;
	    	Imgproc.blur(img, blurredImage, new Size(5,5));
	    	Imgproc.cvtColor(blurredImage, hsv, Imgproc.COLOR_RGB2HSV);

	    Core.inRange(hsv, new Scalar(40, 40, 40), new Scalar(155, 199, 121), img3);

	    	Imgproc.applyColorMap(img3, img2, Imgproc.COLORMAP_COOL);
	    	Imgcodecs.imwrite("NCAP-000-000-249-123-000.jpg", img2);
	    	Imgcodecs.imwrite("NCAP-000-000-249-123-007.jpg", img3);
	    	Imgcodecs.imwrite("NCAP-000-000-249-123-002.jpg", blurredImage);
	    	System.out.println("Done!");
    }
}
