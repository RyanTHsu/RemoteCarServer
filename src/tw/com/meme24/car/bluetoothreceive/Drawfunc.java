package tw.com.meme24.car.bluetoothreceive;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Paint.Style;

import android.graphics.drawable.BitmapDrawable;
import android.view.View;
import android.widget.GridView;
import android.widget.ImageView;

public class Drawfunc extends View{
	private int mov_x=20;//聲明起點座標
	private int mov_y=220;
	private static int index=0;
	private Paint paint;//聲明畫筆
	private Canvas canvas;//畫布
	private Bitmap bitmap;//點陣圖
	private Context mContext;
	//private int blcolor;
	public static int amove=10;
	public Drawfunc(Context context) {
		super(context);
		paint=new Paint(Paint.DITHER_FLAG);//創建一個畫筆
		bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888); //設置點陣圖的寬高
		canvas=new Canvas();
		canvas.setBitmap(bitmap);

		paint.setStyle(Style.STROKE);//設置非填充
		paint.setStrokeWidth(3);//筆寬3圖元
		paint.setColor(Color.RED);//設置為紅筆
		paint.setAntiAlias(true);//鋸齒不顯示

		}
	@Override
	public View findFocus() {
		// TODO Auto-generated method stub
		return new View(mContext);
	}
	@Override
	public View focusSearch(int direction) {
		// TODO Auto-generated method stub
		return new View(mContext);
	}
	protected void onDraw(Canvas canvas) {  
		//super.onDraw(canvas); 
		canvas.drawBitmap(bitmap,0,0,null);
		invalidate();
	}

	public void drawTrack(int idir, int steps){
		if(idir==0){
			canvas.drawLine(mov_x, mov_y, mov_x, mov_y-steps, paint);//畫線
			mov_y=(int) mov_y-steps;
		}else if(idir==1){
			canvas.drawLine(mov_x, mov_y, mov_x-steps, mov_y, paint);//畫線
			mov_x=(int) mov_x-steps;
		}else if(idir==2){
			canvas.drawLine(mov_x, mov_y, mov_x, mov_y+steps, paint);//畫線
			mov_y=(int) mov_y+steps;
		}else if(idir==3){
			canvas.drawLine(mov_x, mov_y, mov_x+steps, mov_y, paint);//畫線
			mov_x=(int) mov_x+steps;
		}
	}
	public void newMap(){
		//index++;//新編號
		mov_x=20;//聲明起點座標
		mov_y=220;
		bitmap = Bitmap.createBitmap(240, 240, Bitmap.Config.ARGB_8888); //設置點陣圖的寬高
		canvas=new Canvas();
		canvas.setBitmap(bitmap);
		paint.setStyle(Style.STROKE);//設置非填充
		paint.setStrokeWidth(3);//筆寬3圖元
		paint.setColor(Color.RED);//設置為紅筆
		paint.setAntiAlias(true);//鋸齒不顯示
	}
	public void saveBitmap(){
		try {
				//index++;
            // 輸出的圖檔位置
//            FileOutputStream fos = new FileOutputStream( "/data/data/tw.com.meme24.usecanvas/drawmap"+index+".png" );
			FileOutputStream fos = new FileOutputStream( "/sdcard/drawmap"+index+".png" );
            // 將 Bitmap 儲存成 PNG / JPEG 檔案格式
            bitmap.compress( Bitmap.CompressFormat.PNG, 100, fos );
            // 釋放
            fos.close();
        }
        catch (IOException e)
        {}
	}
	public View getView(){
		//LayoutInflater factory = LayoutInflater.from(mContext);
		//final View alertDialog= factory.inflate(R.layout.alert_dialog, null);
		ImageView bView = new ImageView(mContext);
		BitmapFactory.Options options = new BitmapFactory.Options();
		//options.inJustDecodeBounds = true;
		// will results in a much smaller image than the original
		//options.inSampleSize = 1;
		//options.outWidth = 120;
		//options.outHeight = 120;
		//options.inJustDecodeBounds = false;
		Bitmap bm = BitmapFactory.decodeFile("/sdcard/drawmap"+index+".png", options);
		BitmapDrawable bitmapDrawable = new BitmapDrawable(bm);
		//bView.setImageBitmap(null);
		bView.setImageDrawable(bitmapDrawable);
		//bView.invalidate();
		//bm.recycle();
		//bView.setScaleType(ImageView.ScaleType.FIT_XY);
		bView.setLayoutParams(new GridView.LayoutParams(120, 120));
		bView.setScaleType(ImageView.ScaleType.CENTER_CROP);
		bView.setPadding(8, 8, 8, 8);
		bView.setBackgroundColor(Color.GREEN);
		return bView;
	}
}
