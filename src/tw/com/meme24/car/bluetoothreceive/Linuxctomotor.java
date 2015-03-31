package tw.com.meme24.car.bluetoothreceive;
import android.util.Log;

public class Linuxctomotor {

	static {
        try {
            Log.i("JNI", "Trying to load librmotor.so");
            /* 調用libled.so 庫 */
            System.loadLibrary("rmotor"); 
        }
        catch (UnsatisfiedLinkError ule) {
            Log.e("JNI", "WARNING: Could not load librmotor.so");
        }}
 
	public static native int opendriver();

	public static native int closedriver();
  
//	public static native int send(int led_num, int on_off);
	public static native int send(int d_com, int test);

}
