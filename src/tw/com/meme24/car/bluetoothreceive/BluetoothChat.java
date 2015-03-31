package tw.com.meme24.car.bluetoothreceive;


import tw.com.meme24.car.bluetoothreceive.R;
import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.View.OnClickListener;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

/**
 * This is the main Activity that displays the current chat session.
 */
public class BluetoothChat extends Activity {
    // Debugging
    private static final String TAG = "BluetoothCar";
    private static final boolean D = true;
    private static boolean auto_mode_on = false;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_READ = 2;
    public static final int MESSAGE_WRITE = 3;
    public static final int MESSAGE_DEVICE_NAME = 4;
    public static final int MESSAGE_TOAST = 5;

    // Key names received from the BluetoothChatService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;

    // Layout Views
    private TextView mTitle;
    //private ListView mConversationView;
    //private EditText mOutEditText;
    //private String ReStr = "";
    private TextView tv_dir;
    private TextView tv_steps;
    private TextView tv_dt;
    private Button exit_btn;
    private Button start_btn;
    private Button stop_btn;
    private Button reset_btn;
    
    private EditText et_b;
    private EditText et_l;
    private EditText et_r;
    private EditText et_delay;
    
    private TextView mText;
    private static int r_fun=-1;
    private static int l_fun=-1;
    public int fd2,idir=0,isteps=0,count=0;
	
    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Array adapter for the conversation thread
    //private ArrayAdapter<String> mConversationArrayAdapter;
    // String buffer for outgoing messages
    private StringBuffer mOutStringBuffer;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothChatService mChatService = null;
    
    private Integer delay_time;
    private Integer back_steps;
    private Integer left_steps;
    private Integer right_steps;
    int SONIC_1=21, SONIC_2=22, SONIC_3=23, INFRARED=31;
    Handler auto_handler=new Handler();
    LinearLayout layout;
    private Drawfunc draw;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(D) Log.e(TAG, "+++ ON CREATE +++");
        
        
        // Set up the window layout
        requestWindowFeature(Window.FEATURE_CUSTOM_TITLE);
        setContentView(R.layout.main);
        getWindow().setFeatureInt(Window.FEATURE_CUSTOM_TITLE, R.layout.custom_title);
        
        fd2=Linuxctomotor.opendriver();
        Log.d(TAG, "motor fd :"+String.valueOf(fd2));
        mText = (TextView) findViewById(R.id.textView1);
        // Set up the custom title
        mTitle = (TextView) findViewById(R.id.title_left_text);
        mTitle.setText(R.string.app_name);
        mTitle = (TextView) findViewById(R.id.title_right_text);
        
        //Bluetooth receive layout
        tv_dir = (TextView)findViewById(R.id.tv_dir);
        tv_steps = (TextView)findViewById(R.id.tv_steps);
        tv_dt = (TextView)findViewById(R.id.tv_dt);
        exit_btn = (Button)findViewById(R.id.btn_exit);
        exit_btn.setOnClickListener(motor_close);
        
        //Track map layout
        draw=new Drawfunc(this);
        layout=(LinearLayout) findViewById(R.id.root);
        layout.setBackgroundColor(Color.BLACK);
        layout.addView(draw);
		
        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        findview();
        setlistener();
        
    }

	public void findview(){
		exit_btn = (Button)findViewById(R.id.btn_exit);
		start_btn = (Button)findViewById(R.id.btn_start);
		stop_btn = (Button)findViewById(R.id.btn_stop);
		reset_btn= (Button)findViewById(R.id.btn_reset);
		et_delay = (EditText)findViewById(R.id.et_delay);
		et_b = (EditText)findViewById(R.id.et_back);
		et_l = (EditText)findViewById(R.id.et_left);
		et_r = (EditText)findViewById(R.id.et_right);
	}
	private void setlistener(){
		start_btn.setOnClickListener(start_set);
		stop_btn.setOnClickListener(stop_set);
		reset_btn.setOnClickListener(reset_set);
	}
    private OnClickListener motor_close = new OnClickListener(){
    	@Override
    	public void onClick(View v) {
    		// TODO Auto-generated method stub
    		
    		finish();
    	  }
        };
    
    @Override
    public void onStart() {
        super.onStart();
        if(D) Log.e(TAG, "++ ON START ++");

        // If BT is not on, request that it be enabled.
        // setupChat() will then be called during onActivityResult
        if (!mBluetoothAdapter.isEnabled()) {
            Intent enableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
            startActivityForResult(enableIntent, REQUEST_ENABLE_BT);
        // Otherwise, setup the chat session
        } else {
            if (mChatService == null) setupChat();
        }
    }

    @Override
    public synchronized void onResume() {
        super.onResume();
        if(D) Log.e(TAG, "+ ON RESUME +");

        // Performing this check in onResume() covers the case in which BT was
        // not enabled during onStart(), so we were paused to enable it...
        // onResume() will be called when ACTION_REQUEST_ENABLE activity returns.
        if (mChatService != null) {
            // Only if the state is STATE_NONE, do we know that we haven't started already
            if (mChatService.getState() == BluetoothChatService.STATE_NONE) {
              // Start the Bluetooth chat services
              mChatService.start();
            }
        }
    }

    private void setupChat() {
        Log.d(TAG, "setupChat()");

        // Initialize the array adapter for the conversation thread
        //mConversationArrayAdapter = new ArrayAdapter<String>(this, R.layout.message);
        //mConversationView = (ListView) findViewById(R.id.in);
        //mConversationView.setAdapter(mConversationArrayAdapter);

        // Initialize the compose field with a listener for the return key
        //mOutEditText = (EditText) findViewById(R.id.edit_text_out);
        //mOutEditText.setOnEditorActionListener(mWriteListener);

        // Initialize the send button with a listener that for click events
        
        
        // Initialize the BluetoothChatService to perform bluetooth connections
        mChatService = new BluetoothChatService(this, mHandler);

        // Initialize the buffer for outgoing messages
        mOutStringBuffer = new StringBuffer("");
    }

    @Override
    public synchronized void onPause() {
        super.onPause();
        if(D) Log.e(TAG, "- ON PAUSE -");
    }

    @Override
    public void onStop() {
        super.onStop();
        if(D) Log.e(TAG, "-- ON STOP --");
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        // Stop the Bluetooth chat services
        if (mChatService != null) mChatService.stop();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }

    private void ensureDiscoverable() {
        if(D) Log.d(TAG, "ensure discoverable");
        if (mBluetoothAdapter.getScanMode() !=
            BluetoothAdapter.SCAN_MODE_CONNECTABLE_DISCOVERABLE) {
            Intent discoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
            discoverableIntent.putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 300);
            startActivity(discoverableIntent);
        }
    }

    /**
     * Sends a message.
     * @param message  A string of text to send.
     */
    /*private void sendMessage(String message) {
        // Check that we're actually connected before trying anything
        if (mChatService.getState() != BluetoothChatService.STATE_CONNECTED) {
            Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT).show();
            return;
        }

        // Check that there's actually something to send
        if (message.length() > 0) {
            // Get the message bytes and tell the BluetoothChatService to write
            byte[] send = message.getBytes();
            mChatService.write(send);

            // Reset out string buffer to zero and clear the edit text field
            //mOutStringBuffer.setLength(0);
            //mOutEditText.setText(mOutStringBuffer);
        }
    }*/

    // The action listener for the EditText widget, to listen for the return key
    /*private TextView.OnEditorActionListener mWriteListener =
        new TextView.OnEditorActionListener() {
        public boolean onEditorAction(TextView view, int actionId, KeyEvent event) {
            // If the action is a key-up event on the return key, send the message
            if (actionId == EditorInfo.IME_NULL && event.getAction() == KeyEvent.ACTION_UP) {
                String message = view.getText().toString();
                sendMessage(message);
            }
            if(D) Log.i(TAG, "END onEditorAction");
            return true;
        }
    };*/

    // The Handler that gets information back from the BluetoothChatService
    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
            case MESSAGE_STATE_CHANGE:
                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
                switch (msg.arg1) {
                case BluetoothChatService.STATE_CONNECTED:
                    mTitle.setText(R.string.title_connected_to);
                    mTitle.append(mConnectedDeviceName);
                    //mConversationArrayAdapter.clear();
                    break;
                case BluetoothChatService.STATE_CONNECTING:
                    mTitle.setText(R.string.title_connecting);
                    break;
                case BluetoothChatService.STATE_LISTEN:
                case BluetoothChatService.STATE_NONE:
                    mTitle.setText(R.string.title_not_connected);
                    break;
                  }
                break;
            case MESSAGE_WRITE:
                byte[] writeBuf = (byte[]) msg.obj;
                // construct a string from the buffer
                String writeMessage = new String(writeBuf);
                //mConversationArrayAdapter.add("Me:  " + writeMessage);
                break;
            case MESSAGE_READ:
                byte[] readBuf = (byte[]) msg.obj;
                // construct a string from the valid bytes in the buffer
                String readMessage = new String(readBuf, 0, msg.arg1);
               // mText.append(readMessage);
                if(readMessage!=null){
                	Log.d(TAG, readMessage);
					int dt=Integer.parseInt(readMessage.toString())%1000;
					int ft=(Integer.parseInt(readMessage.toString())/1000)%10000;
					int dir=Integer.parseInt(readMessage.toString())/(10000*1000);
					// dir=5 => auto mode/remote mode
					if(dir==5){
						if(auto_mode_on)auto_mode_on=false;
						else {
							delay_time=Integer.parseInt(et_delay.getText().toString());
							back_steps=Integer.parseInt(et_b.getText().toString());
							left_steps=Integer.parseInt(et_l.getText().toString());
							right_steps=Integer.parseInt(et_r.getText().toString());
							auto_mode_on=true;
							if(auto_handler!=null)auto_handler.removeCallbacks(f_run);
							auto_handler.post(f_run);
						}
					}else if(dir==6){
						/*send track map*/
					}else {	
						Log.d(TAG, "dt:"+dt+" ft:"+ft+" dir:"+dir);
						
//						if(dir==1)tv_dir.setText(getString(R.string.front));
						if(dir==2)	idir=(idir-+1)%4;
						else if(dir==3) idir=(idir-1+4)%4;
						else if(dir==4) idir=(idir+2)%4;
						set_dir(idir);
						
					
//					mText.append(String.valueOf(dir));
						tv_steps.setText(String.valueOf(ft));
						tv_dt.setText(String.valueOf(dt));
						readMessage=null;
			   
						for(int i=0; i<ft ; i++){
							if(dir==1){
								if(Linuxctomotor.send(SONIC_1, 0)==1){
									Log.d(TAG,"Hit something!!!!");
									break;
								}else if(Linuxctomotor.send(INFRARED, 0)==1){
									Log.d(TAG,"Stop due to no signal(INFRARED)!");
									break;
								}
								count++;
								Linuxctomotor.send(right_function(1), 0);
								Linuxctomotor.send(left_function(1)+10, 0);
							}else if(dir==3){

								Linuxctomotor.send(right_function(-1), 0);
								Linuxctomotor.send(left_function(1)+10, 0);
							}else{
								Linuxctomotor.send(right_function(1), 0);
								Linuxctomotor.send(left_function(-1)+10, 0);
							}
							mdelay(dt);
						}
						if(dir==1)draw.drawTrack(idir, count/5);
						count=0;
					}
                  }
                //mConversationArrayAdapter.add(mConnectedDeviceName+":  " + readMessage);
              break;
            case MESSAGE_DEVICE_NAME:
                // save the connected device's name
                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
                Toast.makeText(getApplicationContext(), "Connected to "
                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
                break;
            case MESSAGE_TOAST:
                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
                               Toast.LENGTH_SHORT).show();
                break;
            }
        }
    };

    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == Activity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mChatService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode == Activity.RESULT_OK) {
                // Bluetooth is now enabled, so set up a chat session
                setupChat();
            } else {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

    
    //function area
    private void set_dir(int dir){
    	if(dir==0)tv_dir.setText(getString(R.string.front));
		else if(dir==1)tv_dir.setText(getString(R.string.left));
		else if(dir==2)tv_dir.setText(getString(R.string.back));
		else if(dir==3)tv_dir.setText(getString(R.string.right));
    }
    private void rec_dir(int dir){
    	switch (dir){
    	case 2:
    		idir=(idir+1)%4;
    		break;
    	case 3:
    		idir=(idir+2)%4;
    		break;
    	case 4:
    		idir=(idir-1+4)%4;
    		break;
    	default :
    		break;
    	}
    }
    private void mdelay (int sec){
		try	{
			Thread.sleep(sec); // do nothing for 1000 miliseconds (1 second)
		}catch(InterruptedException e){
			e.printStackTrace();}
	}
    
    public int left_function(int interval){
		if(l_fun == -1){
			l_fun=1;
		}else{
			l_fun+=interval;
			if(l_fun<1)l_fun+=4;
			if(l_fun>4)l_fun-=4;
		}
		return l_fun;
	}
	
	public int right_function(int interval){
		if(r_fun == -1){
			r_fun=1;
		}else{
			r_fun+=interval;
			if(r_fun<1)r_fun+=4;
			if(r_fun>4)r_fun-=4;
		}
		return r_fun;
	}

    
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        //getMenuInflater().inflate(R.menu.driver_control, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.scan:
            // Launch the DeviceListActivity to see devices and do scan
            Intent serverIntent = new Intent(this, DeviceListActivity.class);
            startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
            return true;
        case R.id.discoverable:
            // Ensure this device is discoverable by others
            ensureDiscoverable();
            return true;
        }
        return false;
    }
    
    /*######################## RUNNABLE AREA #########################*/
	final Runnable f_run = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			if(true){
				if(!auto_mode_on){
					Log.d(TAG,"#### Car stop ####");
				}else if( Linuxctomotor.send(INFRARED, 0)==1 ){
					Log.d(TAG,"#### It's end!!!! ####");
					draw.drawTrack(idir, isteps/5);
					draw.saveBitmap();
					isteps=0;count=0;idir=0;
				}else {
					// every 4 steps check block
//					if(count%4 == 1){
						if(Linuxctomotor.send(SONIC_1, 0)==1){
							if(Linuxctomotor.send(SONIC_2, 0)==1){
								if(Linuxctomotor.send(SONIC_3, 0)==1){
							// turn back
									draw.drawTrack(idir, isteps/5);
									count=0;isteps=0;
									idir=(idir+2)%4;
									set_dir(idir);
									Log.d(TAG,"#### Car is going to turn back for steps: "+back_steps);
									auto_handler.post(b_run);
								}else{
							// turn right
									draw.drawTrack(idir, isteps/5);
									count=0;isteps=0;
									idir=(idir-1+4)%4;
									set_dir(idir);
									Log.d(TAG,"#### Car is going to turn right for steps: "+right_steps);
									auto_handler.post(r_run);
								}
							}else{
							// turn left
								draw.drawTrack(idir, isteps/5);
								count=0;isteps=0;
								idir=(idir+1)%4;
								set_dir(idir);
								Log.d(TAG,"#### Car is going to turn left for steps: "+left_steps);
								auto_handler.post(l_run);
							}							
						}else{
							//go forward
							Log.d(TAG,"#### Car is going! step: "+count);
							Linuxctomotor.send(right_function(1), 0);
							Linuxctomotor.send(left_function(1)+10, 0);
							tv_steps.setText(String.valueOf(count));
							
							count++;
							isteps++;
							if(isteps > 10){
								draw.drawTrack(idir, isteps/5);
								isteps=0;
							}
//							mdelay(delay_time-5);
//							motor_handler.post(f_run);
							auto_handler.postDelayed(f_run, delay_time-5);						
						}
						
					//}	
				}
											
			}			
		}
	};
	
	final Runnable b_run = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i=0; i<back_steps ; i++){
				Linuxctomotor.send(right_function(1), 0);
				Linuxctomotor.send(left_function(-1)+10, 0);
				mdelay(delay_time);
			}
			auto_handler.post(f_run);
		}
	};
	final Runnable l_run = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i=0; i<left_steps ; i++){
				Linuxctomotor.send(right_function(1), 0);
				Linuxctomotor.send(left_function(-1)+10, 0);
				mdelay(delay_time);
			}
			auto_handler.post(f_run);
		}
	};
	final Runnable r_run = new Runnable(){
		@Override
		public void run() {
			// TODO Auto-generated method stub
			for(int i=0; i<right_steps ; i++){
				Linuxctomotor.send(right_function(-1), 0);
				Linuxctomotor.send(left_function(1)+10, 0);
				mdelay(delay_time);
			}
			auto_handler.post(f_run);
		}
	};
	
	/*######################### ONCLICK AREA #########################*/
	private OnClickListener start_set = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			delay_time=Integer.parseInt(et_delay.getText().toString());
			back_steps=Integer.parseInt(et_b.getText().toString());
			left_steps=Integer.parseInt(et_l.getText().toString());
			right_steps=Integer.parseInt(et_r.getText().toString());
			auto_mode_on=true;
			if(auto_handler!=null)auto_handler.removeCallbacks(f_run);
			set_dir(idir);
			tv_dt.setText(String.valueOf(delay_time));
			auto_handler.post(f_run);		
		}		
	};
	
	private OnClickListener stop_set = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			auto_mode_on=false;
			count=0;
			if(auto_handler !=null)auto_handler.removeCallbacks(f_run);
		}		
	};
	
	private OnClickListener reset_set = new OnClickListener(){

		@Override
		public void onClick(View v) {
			// TODO Auto-generated method stub
			auto_mode_on=false;
			count=0;
			idir=0;
			draw.newMap();
		}	
	};


}