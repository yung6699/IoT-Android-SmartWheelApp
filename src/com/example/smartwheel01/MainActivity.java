package com.example.smartwheel01;

import android.app.Activity;
import android.content.Intent;
import android.hardware.Camera;
import android.hardware.Camera.Parameters;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.View.OnTouchListener;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

public class MainActivity extends Activity implements OnClickListener,
		OnTouchListener, OnCheckedChangeListener {

	TextView connectionView, batteryView, speedView;
	ToggleButton engineBtn, lightBtn;
	Button leftWheel, rightWheel;

	// 받아온 내용을 저장하는 임시 변수
	int count = 1;
	String speed, battery = null;

	private static final String TAG = "Main";

	// 연결된 장치 이름 저장하는 스트링 변수
	private String mConnectedDeviceName = null;

	// Message types sent from the BluetoothChatService Handler
	public static final int MESSAGE_STATE_CHANGE = 1;
	public static final int MESSAGE_READ = 2;
	public static final int MESSAGE_WRITE = 3;
	public static final int MESSAGE_DEVICE_NAME = 4;
	public static final int MESSAGE_TOAST = 5;

	// Key names received from the BluetoothChatService Handler
	public static final String DEVICE_NAME = "device_name";
	public static final String TOAST = "toast";

	// 블루투스 연결을 요청할 상수값
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	private BluetoothService btService = null;
	
	private Camera camera;
	private Parameters p;

	private final Handler mHandler = new Handler() {

		public void handleMessage(Message msg) {
			switch (msg.what) {

			case MESSAGE_STATE_CHANGE:

				switch (msg.arg1) {
				case BluetoothService.STATE_CONNECTED:

					connectionView.setText(mConnectedDeviceName);

					break;
				case BluetoothService.STATE_CONNECTING:
					connectionView.setText(R.string.title_connecting);
					break;
				case BluetoothService.STATE_LISTEN:
				case BluetoothService.STATE_NONE:
					connectionView.setText(R.string.title_not_connected);
					break;
				}
				break;

			case MESSAGE_WRITE:

				byte[] writeBuf = (byte[]) msg.obj;
				// construct a string from the buffer
				String writeMessage = new String(writeBuf);
				// mConversationArrayAdapter.add("나:  " + writeMessage);

				break;
			case MESSAGE_READ:
				byte[] readBuf = (byte[]) msg.obj;
				// construct a string from the valid bytes in the buffer

				String readMessage = new String(readBuf, 0, msg.arg1);

				speed = readMessage;
				speedView.setText(speed + " KM");

				break;

			case MESSAGE_DEVICE_NAME:
				// save the connected device's name
				mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
				Toast.makeText(getApplicationContext(),
						"Connected to " + mConnectedDeviceName,
						Toast.LENGTH_SHORT).show();
				break;

			case MESSAGE_TOAST:
				Toast.makeText(getApplicationContext(),
						msg.getData().getString(TOAST), Toast.LENGTH_SHORT)
						.show();
				break;
			}
		}

	};

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		connectionView = (TextView) findViewById(R.id.connection_display);

		speedView = (TextView) findViewById(R.id.speed_display);

		Button speedUpBtn = (Button) findViewById(R.id.speed_up_btn);
		Button speedDownBtn = (Button) findViewById(R.id.speed_down_btn);
		Button leftBtn = (Button) findViewById(R.id.left_btn);
		Button rightBtn = (Button) findViewById(R.id.right_btn);
		Button stopBtn = (Button) findViewById(R.id.centerView);
		Button bluetoothBtn = (Button) findViewById(R.id.bluetooth_btn);
		leftWheel = (Button) findViewById(R.id.left_wheel);
		rightWheel = (Button) findViewById(R.id.right_wheel);

		speedUpBtn.setOnClickListener(this);
		speedDownBtn.setOnClickListener(this);
		stopBtn.setOnClickListener(this);
		
		/////////////////////////////////////////////////////////
		// 블루투스 장치 리스트를 띄우기 위한 이벤트 처리
		bluetoothBtn.setOnClickListener(new OnClickListener() {
			
			@Override
			public void onClick(View v) {
				// TODO Auto-generated method stub
				Intent serverIntent = new Intent(MainActivity.this, DeviceListActivity.class);
				startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
				
			}
		});
		
		leftBtn.setOnTouchListener(this);
		rightBtn.setOnTouchListener(this);

		engineBtn = (ToggleButton) findViewById(R.id.engine_Btn);
		lightBtn = (ToggleButton) findViewById(R.id.light_btn);

		engineBtn.setOnCheckedChangeListener(this);
		
		//////////////////////////////////////////////////////////////////////////////////
		//카메라 플래시를 켜는 이벤트
		lightBtn.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton arg0, boolean arg1) {
				// TODO Auto-generated method stub
				
				if(lightBtn.isChecked()==true){
					
					lightBtn.setBackgroundResource(R.drawable.light_on);
					
					camera = Camera.open(); 
					p = camera.getParameters();
					p.setFlashMode(Parameters.FLASH_MODE_TORCH);
					camera.setParameters(p); 
					camera.startPreview();
					
				}
				
				else{
					
					lightBtn.setBackgroundResource(R.drawable.light_off);
					
					p.setFlashMode(Parameters.FLASH_MODE_OFF);
					camera.setParameters(p);
					camera.stopPreview();
					camera.release();
					
				}
			}
		});

	}

	// ////////////////////////////////////////////////////////////////////////////////////////////////////////

	// 토글 버튼 관련 이벤트 메소드 이다. isChecekd는 체크 여부이다.
	// 이벤트를 익명으로 하지 않은 이유는 stop에서 다시 메소드를 호출하기 위해서 익명으로 만들지 않았다.
	@Override
	public void onCheckedChanged(CompoundButton engine, boolean isChecked) {

		// TODO Auto-generated method stub

		// 연결된 장치가 있는지 확인
		if (mConnectedDeviceName != null) {

			// 엔진 버튼이 켜지면 발생하는 조건문
			if (engine.isChecked() == true) {
				String message = "1";
				sendMessage(message);
				engineBtn.setBackgroundResource(R.drawable.engine_stop);

			}

			// 엔진을 끌때 발생하는 조건문
			else {

				String message = "9";
				sendMessage(message);
				engineBtn.setBackgroundResource(R.drawable.engine_start);
			}
		}

		else {
			Toast.makeText(MainActivity.this, "블루투스 장치에 연결 해 주세요",
					Toast.LENGTH_SHORT).show();
			engineBtn.setBackgroundResource(R.drawable.engine_start);

		}

	}

	/***********************************************************************************************************/
	// 스피드 업 버튼 클릭시 발생 이벤트
	@Override
	public void onClick(View v) {
		// TODO Auto-generated method stub

		if (mConnectedDeviceName != null) {

			if (engineBtn.isChecked() == true) {

				switch (v.getId()) {

				case R.id.speed_up_btn:

					String message1 = "2";
					sendMessage(message1);

					break;

				case R.id.speed_down_btn:

					String message2 = "8";
					sendMessage(message2);

					break;

				case R.id.centerView:

					String message3 = "5";
					sendMessage(message3);

					break;

			
				}
			}

			else {

				Toast.makeText(this, "시동을 켜 주세요.", Toast.LENGTH_SHORT).show();
			}

		}

		else {

			Toast.makeText(this, "블루투스 장치에 연결 되어 있지 않습니다.", Toast.LENGTH_SHORT)
					.show();
		}

	}

	// 왼쪽 오른쪽 버튼 롱클릭시 발생하는 이벤트
	@Override
	public boolean onTouch(View v, MotionEvent event) {
		// TODO Auto-generated method stub

		if (mConnectedDeviceName != null) {

			if (engineBtn.isChecked()) {

				switch (v.getId()) {

				case R.id.left_btn:

					if (event.getAction() == event.ACTION_DOWN) {

						String message = "4";
						sendMessage(message);

						// 바퀴 컨트롤러 이미지가 바뀐다.
						rightWheel.setBackgroundResource(R.drawable.backward);
					}

					else if (event.getAction() == event.ACTION_UP) {
						String message = "1";
						sendMessage(message);
						// 바퀴 컨트롤러 이미지가 바뀐다.
						rightWheel.setBackgroundResource(R.drawable.forward);
					}
					break;

				case R.id.right_btn:

					if (event.getAction() == event.ACTION_DOWN) {

						String message1 = "6";
						sendMessage(message1);

						// 바퀴 컨트롤러 이미지가 바뀐다.
						leftWheel.setBackgroundResource(R.drawable.backward);
					}

					else if (event.getAction() == event.ACTION_UP) {
						String message = "1";
						sendMessage(message);

						// 바퀴 컨트롤러 이미지가 바뀐다.
						leftWheel.setBackgroundResource(R.drawable.forward);

					}

					break;

				}

			}

			else {

				Toast.makeText(this, "시동을 켜 주세요.", Toast.LENGTH_SHORT).show();
			}

		}

		else {

			Toast.makeText(this, "블루투스 장치에 연결이 되어 있지 않습니다.", Toast.LENGTH_SHORT)
					.show();
		}

		return false;
	}

	// ///////////////////////////////////////////////////////////////////////////////
	// 블루투스 소켓에 연결된 입출력 스트림을 통해 메세지를 보내는 메소드

	private void sendMessage(String message) {
		// Check that we're actually connected before trying anything
		if (btService.getState() != btService.STATE_CONNECTED) {
			Toast.makeText(this, R.string.not_connected, Toast.LENGTH_SHORT)
					.show();
			return;
		}

		// Check that there's actually something to send
		if (message.length() > 0) {
			// Get the message bytes and tell the BluetoothChatService to write
			byte[] send = message.getBytes();
			btService.write(send);

			// Reset out string buffer to zero and clear the edit text field

		}
	}

	/***************************************************************************************/

	// 앱 시작시 발생하는 메소드 이다. 안드로이드 생명주기 찾아볼것!!!!!!!!!!!!!!
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		// BluetoothService 클래스 생성
		if (btService == null) {
			btService = new BluetoothService(this, mHandler);
		}

		if (btService != null) {
			if (btService.getDeviceState()) {
				// 블루투스가 지원 가능한 기기일 때
				btService.enableBluetooth();
			} else {
				finish();
			}
		}
	}

	// 안드로이드 생명주기 관련되어 찾아볼것!!!!!!!!!!!!!!!!

	@Override
	protected void onStop() {

		// 앱 중지시 속도 값 0km 주고 연결 스레드 정지..
		super.onStop();

		String message = "9";
		sendMessage(message);

		// 엔진 토글버튼 체크 해제
		onCheckedChanged(engineBtn, false);

		// 연결된 스레드 종료
		btService.stop();

		// 장치연결 객체 비움;
		// 시작할때 다시 찾아서 연결하기 위해 널값줌
		mConnectedDeviceName = null;

		// 엔진버튼 이미지 stop으로 바꿈
		engineBtn.setBackgroundResource(R.drawable.engine_start);
	}

	// 앱 다시 시작할때 start()가 발생하기 직전에 발생하는 메소드이다. 스레드를 초기화 하는 start메소드를 호출한다.
	// 역시 안드로이드 생명주기 찾아볼것!!!
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		btService.start();

	}

	// 인텐트로 넘어온 결과값을 받는 메소드이다.!!!!!!!!!!!
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {

		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				// 연결된 장치의 정보를 받아온다.

				btService.getDeviceInfo(data);
			}

			break;

		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {

				Toast.makeText(this, "블루투스가 켜졌습니다.", Toast.LENGTH_SHORT).show();

				// 장치 검색 시작
				btService.scanDevice();

			} else {

				Toast.makeText(this, "블루투스 연결이 되지않아 종료합니다.", Toast.LENGTH_LONG)
						.show();
				Log.d(TAG, "Bluetooth is not enabled");
				finish();
			}
			break;
		}
	}

}
