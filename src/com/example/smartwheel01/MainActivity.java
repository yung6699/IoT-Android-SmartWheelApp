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

	// �޾ƿ� ������ �����ϴ� �ӽ� ����
	int count = 1;
	String speed, battery = null;

	private static final String TAG = "Main";

	// ����� ��ġ �̸� �����ϴ� ��Ʈ�� ����
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

	// ������� ������ ��û�� �����
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
				// mConversationArrayAdapter.add("��:  " + writeMessage);

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
		// ������� ��ġ ����Ʈ�� ���� ���� �̺�Ʈ ó��
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
		//ī�޶� �÷��ø� �Ѵ� �̺�Ʈ
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

	// ��� ��ư ���� �̺�Ʈ �޼ҵ� �̴�. isChecekd�� üũ �����̴�.
	// �̺�Ʈ�� �͸����� ���� ���� ������ stop���� �ٽ� �޼ҵ带 ȣ���ϱ� ���ؼ� �͸����� ������ �ʾҴ�.
	@Override
	public void onCheckedChanged(CompoundButton engine, boolean isChecked) {

		// TODO Auto-generated method stub

		// ����� ��ġ�� �ִ��� Ȯ��
		if (mConnectedDeviceName != null) {

			// ���� ��ư�� ������ �߻��ϴ� ���ǹ�
			if (engine.isChecked() == true) {
				String message = "1";
				sendMessage(message);
				engineBtn.setBackgroundResource(R.drawable.engine_stop);

			}

			// ������ ���� �߻��ϴ� ���ǹ�
			else {

				String message = "9";
				sendMessage(message);
				engineBtn.setBackgroundResource(R.drawable.engine_start);
			}
		}

		else {
			Toast.makeText(MainActivity.this, "������� ��ġ�� ���� �� �ּ���",
					Toast.LENGTH_SHORT).show();
			engineBtn.setBackgroundResource(R.drawable.engine_start);

		}

	}

	/***********************************************************************************************************/
	// ���ǵ� �� ��ư Ŭ���� �߻� �̺�Ʈ
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

				Toast.makeText(this, "�õ��� �� �ּ���.", Toast.LENGTH_SHORT).show();
			}

		}

		else {

			Toast.makeText(this, "������� ��ġ�� ���� �Ǿ� ���� �ʽ��ϴ�.", Toast.LENGTH_SHORT)
					.show();
		}

	}

	// ���� ������ ��ư ��Ŭ���� �߻��ϴ� �̺�Ʈ
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

						// ���� ��Ʈ�ѷ� �̹����� �ٲ��.
						rightWheel.setBackgroundResource(R.drawable.backward);
					}

					else if (event.getAction() == event.ACTION_UP) {
						String message = "1";
						sendMessage(message);
						// ���� ��Ʈ�ѷ� �̹����� �ٲ��.
						rightWheel.setBackgroundResource(R.drawable.forward);
					}
					break;

				case R.id.right_btn:

					if (event.getAction() == event.ACTION_DOWN) {

						String message1 = "6";
						sendMessage(message1);

						// ���� ��Ʈ�ѷ� �̹����� �ٲ��.
						leftWheel.setBackgroundResource(R.drawable.backward);
					}

					else if (event.getAction() == event.ACTION_UP) {
						String message = "1";
						sendMessage(message);

						// ���� ��Ʈ�ѷ� �̹����� �ٲ��.
						leftWheel.setBackgroundResource(R.drawable.forward);

					}

					break;

				}

			}

			else {

				Toast.makeText(this, "�õ��� �� �ּ���.", Toast.LENGTH_SHORT).show();
			}

		}

		else {

			Toast.makeText(this, "������� ��ġ�� ������ �Ǿ� ���� �ʽ��ϴ�.", Toast.LENGTH_SHORT)
					.show();
		}

		return false;
	}

	// ///////////////////////////////////////////////////////////////////////////////
	// ������� ���Ͽ� ����� ����� ��Ʈ���� ���� �޼����� ������ �޼ҵ�

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

	// �� ���۽� �߻��ϴ� �޼ҵ� �̴�. �ȵ���̵� �����ֱ� ã�ƺ���!!!!!!!!!!!!!!
	@Override
	protected void onStart() {
		// TODO Auto-generated method stub
		super.onStart();

		// BluetoothService Ŭ���� ����
		if (btService == null) {
			btService = new BluetoothService(this, mHandler);
		}

		if (btService != null) {
			if (btService.getDeviceState()) {
				// ��������� ���� ������ ����� ��
				btService.enableBluetooth();
			} else {
				finish();
			}
		}
	}

	// �ȵ���̵� �����ֱ� ���õǾ� ã�ƺ���!!!!!!!!!!!!!!!!

	@Override
	protected void onStop() {

		// �� ������ �ӵ� �� 0km �ְ� ���� ������ ����..
		super.onStop();

		String message = "9";
		sendMessage(message);

		// ���� ��۹�ư üũ ����
		onCheckedChanged(engineBtn, false);

		// ����� ������ ����
		btService.stop();

		// ��ġ���� ��ü ���;
		// �����Ҷ� �ٽ� ã�Ƽ� �����ϱ� ���� �ΰ���
		mConnectedDeviceName = null;

		// ������ư �̹��� stop���� �ٲ�
		engineBtn.setBackgroundResource(R.drawable.engine_start);
	}

	// �� �ٽ� �����Ҷ� start()�� �߻��ϱ� ������ �߻��ϴ� �޼ҵ��̴�. �����带 �ʱ�ȭ �ϴ� start�޼ҵ带 ȣ���Ѵ�.
	// ���� �ȵ���̵� �����ֱ� ã�ƺ���!!!
	@Override
	protected void onRestart() {
		// TODO Auto-generated method stub
		super.onRestart();
		btService.start();

	}

	// ����Ʈ�� �Ѿ�� ������� �޴� �޼ҵ��̴�.!!!!!!!!!!!
	public void onActivityResult(int requestCode, int resultCode, Intent data) {
		Log.d(TAG, "onActivityResult " + resultCode);

		switch (requestCode) {

		case REQUEST_CONNECT_DEVICE:
			if (resultCode == Activity.RESULT_OK) {
				// ����� ��ġ�� ������ �޾ƿ´�.

				btService.getDeviceInfo(data);
			}

			break;

		case REQUEST_ENABLE_BT:
			// When the request to enable Bluetooth returns
			if (resultCode == Activity.RESULT_OK) {

				Toast.makeText(this, "��������� �������ϴ�.", Toast.LENGTH_SHORT).show();

				// ��ġ �˻� ����
				btService.scanDevice();

			} else {

				Toast.makeText(this, "������� ������ �����ʾ� �����մϴ�.", Toast.LENGTH_LONG)
						.show();
				Log.d(TAG, "Bluetooth is not enabled");
				finish();
			}
			break;
		}
	}

}
