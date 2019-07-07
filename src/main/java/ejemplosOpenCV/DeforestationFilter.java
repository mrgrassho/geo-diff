package ejemplosOpenCV;

import org.opencv.core.Core;
import org.opencv.core.Mat;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;

public class DeforestationFilter {
	public static void main(String[] args) {
		System.out.println("Processing img");
		System.loadLibrary(Core.NATIVE_LIBRARY_NAME);
		Mat img = Imgcodecs.imread("NCAP-000-000-249-123.jpg");
		Mat img2 = Imgcodecs.imread("NCAP-000-000-249-123.jpg");
		Mat hsv = new Mat();
		Mat blurredImage = new Mat();
		Imgproc.blur(img, blurredImage, new Size(5,5));
		Imgproc.cvtColor(blurredImage, hsv, Imgproc.COLOR_RGB2HSV);

		// Filtra gama de verdes
		Core.inRange(hsv, new Scalar(40, 40, 40), new Scalar(155, 199, 121), img2);

		Imgcodecs.imwrite("NCAP-000-000-249-123-000.jpg", img2);
		System.out.println("Done!");
	}
}
