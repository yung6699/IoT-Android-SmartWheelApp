package com.example.smartwheel01;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.UUID;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class BluetoothService {

	// Debugging
	private static final String TAG = "BluetoothService";

	// Intent request code
	private static final int REQUEST_CONNECT_DEVICE = 1;
	private static final int REQUEST_ENABLE_BT = 2;

	// Constants that indicate the current connection state
	public static final int STATE_NONE = 0; // we're doing nothing
	public static final int STATE_LISTEN = 1; // now listening for incoming
												// connections
	public static final int STATE_CONNECTING = 2; // now initiating an outgoing
													// connection
	public static final int STATE_CONNECTED = 3; // now connected to a remote
													// device

	// device

	private static final UUID MY_UUID = UUID
			.fromString("00001101-0000-1000-8000-00805F9B34FB");

	private ConnectThread mConnectThread;
	private ConnectedThread mConnectedThread;

	private int mState;

	private BluetoothAdapter btAdapter;

	private Activity mActivity;
	private Handler mHandler;

	// 생성자에서 컨텍스트와 핸들러 객체를 받아온다.
	public BluetoothService(Activity ac, Handler h) {
		mActivity = ac;
		mHandler = h;

		// BluetoothAdapter 얻기 , 블루투스를 연결하기 위해 필요
		btAdapter = BluetoothAdapter.getDefaultAdapter();
	}

	/***********************************************/
	// 장치가 블루투스를 지원하는지 확인하는 메소드
	public boolean getDeviceState() {
		Log.d(TAG, "Check the Bluetooth support");

		if (btAdapter == null) {
			Log.d(TAG, "Bluetooth is not available");

			return false;

		} else {
			Log.d(TAG, "Bluetooth is available");

			return true;
		}
	}

	/************************************************/
	public void enableBluetooth() {
		Log.i(TAG, "Check the enabled Bluetooth");

		if (btAdapter.isEnabled()) {
			// 기기의 블루투스 상태가 On인 경우
			Log.d(TAG, "Bluetooth Enable Now");
			scanDevice();
			// Next Step
		} else {
			// 기기의 블루투스 상태가 Off인 경우
			Log.d(TAG, "Bluetooth Enable Request");

			Intent i = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
			mActivity.startActivityForResult(i, REQUEST_ENABLE_BT);

		}
	}

	/*********************************************************/
	// 블루투스 장치 검색 메소드

	public void scanDevice() {

		Log.d(TAG, "Scan Device");

		Intent serverIntent = new Intent(mActivity, DeviceListActivity.class);
		mActivity.startActivityForResult(serverIntent, REQUEST_CONNECT_DEVICE);
	}

	/********************************************************/

	public void getDeviceInfo(Intent data) {
		// 연결된 장치의 맥 주소를 받는다.
		String address = data.getExtras().getString(
				DeviceListActivity.EXTRA_DEVICE_ADDRESS);

		// 블루투스 장치를 얻는다 위에 얻은 주소로
		BluetoothDevice device = btAdapter.getRemoteDevice(address);

		Log.d(TAG, "Get Device Info \n" + "address : " + address);

		// 장치 객채를 커넥스 메소드로 넘긴다.
		connect(device);
	}

	/************************************************************/
	/*
	 * 장치를 검색하고 연결하기 위한 클래스이다. 여기서 장치를 검색하고 블루투스 소켓 객체를 생성하여장치와 연결한다. 즉 이 클래스의
	 * 스레드는 장치 검색을 위한 스레드이다.
	 */

	/******************************************************/
	private class ConnectThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final BluetoothDevice mmDevice;

		public ConnectThread(BluetoothDevice device) {
			mmDevice = device;
			BluetoothSocket tmp = null;

			// 디바이스 정보를 얻어서 BluetoothSocket 생성
			try {
				tmp = device.createRfcommSocketToServiceRecord(MY_UUID);
			} catch (IOException e) {
				Log.e(TAG, "create() failed", e);
			}
			mmSocket = tmp;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectThread");
			setName("ConnectThread");

			// 연결을 시도하기 전에는 항상 기기 검색을 중지한다.
			// 기기 검색이 계속되면 연결속도가 느려지기 때문이다.
			btAdapter.cancelDiscovery();

			// BluetoothSocket 연결 시도
			try {
				// BluetoothSocket 연결 시도에 대한 return 값은 succes 또는 exception이다.
				mmSocket.connect();
				Log.d(TAG, "Connect Success");

			} catch (IOException e) {
				connectionFailed(); // 연결 실패시 불러오는 메소드
				Log.d(TAG, "Connect Fail");

				// socket을 닫는다.
				try {
					mmSocket.close();
				} catch (IOException e2) {
					Log.e(TAG,
							"unable to close() socket during connection failure",
							e2);
				}
				// 연결중? 혹은 연결 대기상태인 메소드를 호출한다.
				BluetoothService.this.start();
				return;
			}

			// ConnectThread 클래스를 reset한다.
			synchronized (BluetoothService.this) {
				mConnectThread = null;
			}

			// ConnectThread를 시작한다.
			connected(mmSocket, mmDevice);
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/*****************************************************************************/
	private class ConnectedThread extends Thread {
		private final BluetoothSocket mmSocket;
		private final InputStream mmInStream;
		private final OutputStream mmOutStream;

		public ConnectedThread(BluetoothSocket socket) {
			Log.d(TAG, "create ConnectedThread");
			mmSocket = socket;
			InputStream tmpIn = null;
			OutputStream tmpOut = null;

			// BluetoothSocket의 inputstream 과 outputstream을 얻는다.
			try {
				tmpIn = socket.getInputStream();
				tmpOut = socket.getOutputStream();
			} catch (IOException e) {
				Log.e(TAG, "temp sockets not created", e);
			}

			mmInStream = tmpIn;
			mmOutStream = tmpOut;
		}

		public void run() {
			Log.i(TAG, "BEGIN mConnectedThread");
			byte[] buffer = new byte[1024];
			int bytes;

			// Keep listening to the InputStream while connected
			while (true) {
				try {
					// InputStream으로부터 값을 받는 읽는 부분(값을 받는다)
					bytes = mmInStream.read(buffer);
					
				     mHandler.obtainMessage(MainActivity.MESSAGE_READ, bytes, -1, buffer)
                     .sendToTarget();

				} catch (IOException e) {
					Log.e(TAG, "disconnected", e);
					connectionLost();
					break;
				}
			}
		}

		/**
		 * Write to the connected OutStream.
		 * 
		 * @param buffer
		 *            The bytes to write
		 */
		public void write(byte[] buffer) {
			try {
				// 값을 쓰는 부분(값을 보낸다)
				mmOutStream.write(buffer);

			} catch (IOException e) {
				Log.e(TAG, "Exception during write", e);
			}
		}

		public void cancel() {
			try {
				mmSocket.close();
			} catch (IOException e) {
				Log.e(TAG, "close() of connect socket failed", e);
			}
		}
	}

	/*****************************************************************************/
	// Bluetooth 상태 set
	private synchronized void setState(int state) {
		Log.d(TAG, "setState() " + mState + " -> " + state);
		mState = state;

		mHandler.obtainMessage(MainActivity.MESSAGE_STATE_CHANGE, state, -1)
				.sendToTarget();
	}

	// Bluetooth 상태 get
	public synchronized int getState() {
		return mState;
	}

	public synchronized void start() {
		Log.d(TAG, "start");

		// 연결을 시도 즉 장치를 찾는 스레드를 종료시킨다.
		if (mConnectThread == null) {

		} else {
			// 소켓을 닫는 메소드 실행
			mConnectThread.cancel();

			// 객체를 널값으로 넣는다.
			mConnectThread = null;
		}

		// 현재 연결되어 입출력으로 통신하는 스레드를 종료난다.
		if (mConnectedThread == null) {

		} else {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}
	}

	// ConnectThread 초기화 device의 모든 연결 제거
	public synchronized void connect(BluetoothDevice device) {
		Log.d(TAG, "connect to: " + device);

		// Cancel any thread attempting to make a connection
		if (mState == STATE_CONNECTING) {
			if (mConnectThread == null) {

			} else {
				mConnectThread.cancel();
				mConnectThread = null;
			}
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread == null) {

		} else {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to connect with the given device
		mConnectThread = new ConnectThread(device);

		mConnectThread.start();
		setState(STATE_CONNECTING);
	}

	// ConnectedThread 초기화
	public synchronized void connected(BluetoothSocket socket,
			BluetoothDevice device) {
		Log.d(TAG, "connected");

		// 연결이 완료되면 장치를 찾는 스레드를 종료한다.
		if (mConnectThread == null) {

		} else {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		// Cancel any thread currently running a connection
		if (mConnectedThread == null) {

		} else {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		// Start the thread to manage the connection and perform transmissions
		mConnectedThread = new ConnectedThread(socket);
		mConnectedThread.start();

		// Send the name of the connected device back to the UI Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_DEVICE_NAME);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.DEVICE_NAME, device.getName());
		msg.setData(bundle);
		mHandler.sendMessage(msg);

		setState(STATE_CONNECTED);
	}

	// 모든 thread stop
	public synchronized void stop() {
		Log.d(TAG, "stop");

		if (mConnectThread != null) {
			mConnectThread.cancel();
			mConnectThread = null;
		}

		if (mConnectedThread != null) {
			mConnectedThread.cancel();
			mConnectedThread = null;
		}

		setState(STATE_NONE);
	}

	// 값을 쓰는 부분(보내는 부분)
	public void write(byte[] out) { // Create temporary object
		ConnectedThread r; // Synchronize a copy of the ConnectedThread
		synchronized (this) {
			if (mState != STATE_CONNECTED)
				return;
			r = mConnectedThread;
		} // Perform the write unsynchronized
		r.write(out);
	}

	// 연결 실패했을때
	private void connectionFailed() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "장치 연결 실패");
		msg.setData(bundle);
		mHandler.sendMessage(msg);
	}

	// 연결을 잃었을 때
	private void connectionLost() {
		setState(STATE_LISTEN);

		// Send a failure message back to the Activity
		Message msg = mHandler.obtainMessage(MainActivity.MESSAGE_TOAST);
		Bundle bundle = new Bundle();
		bundle.putString(MainActivity.TOAST, "장치와 연결 끊어짐");
		msg.setData(bundle);
		mHandler.sendMessage(msg);

	}

}
