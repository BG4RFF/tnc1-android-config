/*
 * Copyright (C) 2013 Mobilinkd LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.mobilinkd.tncconfig;

import android.annotation.TargetApi;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.support.v7.app.ActionBarActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;

public class TncConfig extends FragmentActivity
	implements AudioOutputFragment.Listener, AudioInputFragment.Listener,
		PowerFragment.Listener, KissFragment.Listener,
		ModemFragment.Listener {
	
    // Debugging
    private static final String TAG = "TncConfig";
    private static final boolean D = true;

    // Message types sent from the BluetoothChatService Handler
    public static final int MESSAGE_STATE_CHANGE = 1;
    public static final int MESSAGE_INPUT_VOLUME = 2;
    public static final int MESSAGE_OUTPUT_VOLUME = 3;
    public static final int MESSAGE_OTHER = 4;
    public static final int MESSAGE_DEVICE_NAME = 5;
    public static final int MESSAGE_TOAST = 6;
    public static final int MESSAGE_TX_DELAY = 7;
    public static final int MESSAGE_PERSISTENCE = 8;
    public static final int MESSAGE_SLOT_TIME = 9;
    public static final int MESSAGE_TX_TAIL = 10;
    public static final int MESSAGE_DUPLEX = 11;
    public static final int MESSAGE_SQUELCH_LEVEL = 12;
    public static final int MESSAGE_HW_VERSION = 13;
    public static final int MESSAGE_FW_VERSION = 14;
    public static final int MESSAGE_INPUT_ATTEN = 15;
    public static final int MESSAGE_VERBOSITY = 16;
    public static final int MESSAGE_BATTERY_LEVEL = 17;
    public static final int MESSAGE_BT_CONN_TRACK = 18;
    public static final int MESSAGE_USB_POWER_ON = 19;
    public static final int MESSAGE_USB_POWER_OFF = 20;
    public static final int MESSAGE_PTT_STYLE = 21;
    public static final int MESSAGE_CAPABILITIES = 22;
    
    
    // Key names received from the BluetoothTncService Handler
    public static final String DEVICE_NAME = "device_name";
    public static final String TOAST = "toast";

    // Intent request codes
    private static final int REQUEST_CONNECT_DEVICE = 1;
    private static final int REQUEST_ENABLE_BT = 2;
    
    public static final int TONE_NONE = 0;
    public static final int TONE_SPACE = 1;
    public static final int TONE_MARK = 2;
    public static final int TONE_BOTH = 3;

    // Name of the connected device
    private String mConnectedDeviceName = null;
    // Local Bluetooth adapter
    private BluetoothAdapter mBluetoothAdapter = null;
    // Member object for the chat services
    private BluetoothTncService mTncService = null;
    private Handler mHandler = null;
    
    private ToggleButton mConnectButton;
    private TextView mBluetoothDeviceView;
    private TextView mFirmwareVersionView;
	private String mFirmwareVersion = "282";
    @SuppressWarnings("unused")
	private String hwVersion = "1.12";
    
    private Button mAudioOutputButton = null;
    private boolean mHasPttStyle = false;
    private int mPttStyle = AudioOutputFragment.PTT_STYLE_SIMPLEX;;
    private int mOutputVolume = 0;
    private int mTone = TONE_NONE;
    private boolean mPtt = false;
    
    private AudioInputFragment mAudioInputFragment = null;
    private Button mAudioInputButton = null;
    private boolean mHasInputAtten = false;
    private boolean mInputAtten = true;
    
    private PowerFragment mPowerFragment = null;
    private Button mPowerButton = null;
    private int mBatteryLevel = 0;
    private boolean mPowerControl = false;
    private boolean mPowerOn = false;
    private boolean mPowerOff = false;
    
    private KissFragment mKissFragment = null;
    private Button mKissButton = null;
    private int mTxDelay = 0;
    private int mPersistence = 0;
    private int mSlotTime = 0;
    private int mTxTail = 0;
    private boolean mDuplex = false;

    private ModemFragment mModemFragment = null;
    private Button mModemButton = null;
    private boolean mDcd = false;
    private boolean mHasConnTrack = false;
    private boolean mConnTrack = false;
    private boolean mVerbose = false;
    
    private Button mSaveButton = null;
    private boolean mHasEeprom = false;
    private boolean mNeedsSave = false;

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}
	
	private void onBluetoothConnected() {
        mBluetoothDeviceView.setText(mConnectedDeviceName);
        mFirmwareVersionView.setText(mFirmwareVersion);
        mConnectButton.setChecked(true);
        mAudioOutputButton.setEnabled(true);
        mAudioInputButton.setEnabled(true);
        mKissButton.setEnabled(true);
        mModemButton.setEnabled(true);
        mTncService.getAllValues();
        mNeedsSave = false;
	}
	
	private void onBluetoothConnecting() {
		mBluetoothDeviceView.setText(R.string.title_connecting);
        mFirmwareVersionView.setText("");
        mConnectButton.setChecked(false);
        mAudioOutputButton.setEnabled(false);
        mAudioInputButton.setEnabled(false);
        mPowerButton.setEnabled(false);
        mKissButton.setEnabled(false);
        mModemButton.setEnabled(false);
        mHasConnTrack = false;
        mHasInputAtten = false;
        mHasPttStyle = false;
        mPowerControl = false;
        mSaveButton.setVisibility(Button.GONE);
        mHasEeprom = false;
        mNeedsSave = false;
	}
	
	private void onBluetoothDisconnected() {
    	mBluetoothDeviceView.setText(R.string.title_not_connected);
        mFirmwareVersionView.setText("");
        mConnectButton.setChecked(false);
        mAudioOutputButton.setEnabled(false);
        mAudioInputButton.setEnabled(false);
        mPowerButton.setEnabled(false);
        mKissButton.setEnabled(false);
        mModemButton.setEnabled(false);
        mHasConnTrack = false;
        mHasInputAtten = false;
        mHasPttStyle = false;
        mPowerControl = false;
        mSaveButton.setVisibility(Button.GONE);
        mHasEeprom = false;
        mNeedsSave = false;
	}
	
	private void settingsUpdated() {
		if (mHasEeprom && mNeedsSave == false) {
			mNeedsSave = true;
			mSaveButton.setEnabled(mNeedsSave);
		}
	}
	
	private void createHandler() {
	    // The Handler that gets information back from the BluetoothTncService
		mHandler = new Handler() {
	        @Override
	        public void handleMessage(Message msg) {
	        	byte[] buffer = null;
	            switch (msg.what) {
	            case MESSAGE_STATE_CHANGE:
	                if(D) Log.i(TAG, "MESSAGE_STATE_CHANGE: " + msg.arg1);
	                switch (msg.arg1) {
	                case BluetoothTncService.STATE_CONNECTED:
	                    onBluetoothConnected();
	                    break;
	                case BluetoothTncService.STATE_CONNECTING:
	                	onBluetoothConnecting();
	                    break;
	                case BluetoothTncService.STATE_NONE:
	                	onBluetoothDisconnected();
	                    break;
	                }
	                break;
	            case MESSAGE_OUTPUT_VOLUME:
	            	mOutputVolume = msg.arg1;
	                if(D) Log.d(TAG, "output volume: " + mOutputVolume);
	                break;
	            case MESSAGE_TX_DELAY:
	                TncConfig.this.mTxDelay = msg.arg1;
	                if(D) Log.d(TAG, "tx delay: " + msg.arg1);
	                break;
	            case MESSAGE_PERSISTENCE:
	                TncConfig.this.mPersistence = msg.arg1;
	                if(D) Log.d(TAG, "persistence: " + msg.arg1);
	                break;
	            case MESSAGE_SLOT_TIME:
	                TncConfig.this.mSlotTime = msg.arg1;
	                if(D) Log.d(TAG, "slot time: " + msg.arg1);
	                break;
	            case MESSAGE_TX_TAIL:
	                TncConfig.this.mTxTail = msg.arg1;
	                if(D) Log.d(TAG, "tx tail: " + msg.arg1);
	                break;
	            case MESSAGE_DUPLEX:
	                TncConfig.this.mDuplex = (msg.arg1 != 0);
	                if(D) Log.d(TAG, "duplex: " + TncConfig.this.mDuplex);
	                break;
	            case MESSAGE_SQUELCH_LEVEL:
	                TncConfig.this.mDcd = (msg.arg1 == 0);
	                if(D) Log.d(TAG, "DCD: " + TncConfig.this.mDcd);
	                break;
	            case MESSAGE_VERBOSITY:
	            	TncConfig.this.mVerbose = (msg.arg1 != 0);
	                if(D) Log.d(TAG, "info: " + TncConfig.this.mVerbose);
	                break;
	            case MESSAGE_BT_CONN_TRACK:
	            	TncConfig.this.mHasConnTrack = true;
	                TncConfig.this.mConnTrack = (msg.arg1 != 0);
	                if(D) Log.d(TAG, "bt conn track: " + TncConfig.this.mConnTrack);
	                break;
	            case MESSAGE_INPUT_ATTEN:
	            	TncConfig.this.mHasInputAtten = true;
	                TncConfig.this.mInputAtten = (msg.arg1 != 0);
	                if(D) Log.d(TAG, "input atten: " + msg.arg1);
	                break;
	            case MESSAGE_BATTERY_LEVEL:
	            	TncConfig.this.mPowerButton.setEnabled(true);
	            	buffer = (byte[]) msg.obj;
	            	TncConfig.this.mBatteryLevel = (0xFF & buffer[0]) * 256 + (0xFF & buffer[1]);
	                if(D) Log.d(TAG, "battery level: " + TncConfig.this.mBatteryLevel + "mV");
	                break;
	            case MESSAGE_HW_VERSION:
	            	buffer = (byte[]) msg.obj;
	                String hwVersion = new String(buffer);
	                TncConfig.this.hwVersion = hwVersion;
	                if(D) Log.d(TAG, "hw version: " + hwVersion);
	                break;
	            case MESSAGE_FW_VERSION:
	            	buffer = (byte[]) msg.obj;
	                String fwVersion = new String(buffer);
	                TncConfig.this.mFirmwareVersion = fwVersion;
	                TncConfig.this.mFirmwareVersionView.setText(TncConfig.this.mFirmwareVersion);
	                if(D) Log.d(TAG, "fw version: " + fwVersion);
	                break;
	            case MESSAGE_INPUT_VOLUME:
	            	synchronized (this) {
		            	if (TncConfig.this.mAudioInputFragment != null) {
			                int inputVolume = msg.arg1;
			                double level = (Math.log((double)inputVolume) / Math.log(2.0)) / 8.0;
			                TncConfig.this.mAudioInputFragment.setInputVolume(level);
			                // if(D) Log.d(TAG, "input volume: " + level);
		            	}
	            	}
	                break;
	            case MESSAGE_DEVICE_NAME:
	                // save the connected device's name
	                mConnectedDeviceName = msg.getData().getString(DEVICE_NAME);
	                Toast.makeText(getApplicationContext(), "Connected to "
	                               + mConnectedDeviceName, Toast.LENGTH_SHORT).show();
	                break;
	            case MESSAGE_USB_POWER_ON:
	            	mPowerControl = true;
	            	TncConfig.this.mPowerOn = (msg.arg1 == 1);
	                if(D) Log.d(TAG, "power on: " + TncConfig.this.mPowerOn);
	            	break;
	            case MESSAGE_USB_POWER_OFF:
	            	mPowerControl = true;
	            	TncConfig.this.mPowerOff = (msg.arg1 == 1);
	                if(D) Log.d(TAG, "power off: " + TncConfig.this.mPowerOff);
	            	break;
	            case MESSAGE_PTT_STYLE:
	            	TncConfig.this.mHasPttStyle = true;
	            	TncConfig.this.mPttStyle = msg.arg1;
	                if(D) Log.d(TAG, "ptt style: " + TncConfig.this.mPttStyle);
	            	break;
	            case MESSAGE_CAPABILITIES:
	            	buffer = (byte[]) msg.obj;
	            	if (buffer.length > 1) {
		            	TncConfig.this.mHasEeprom = ((buffer[1] & 0x02) == 0x02);
		            	TncConfig.this.mSaveButton.setVisibility(TncConfig.this.mHasEeprom ? Button.VISIBLE : Button.GONE);
		                if(D) Log.d(TAG, "eeprom save:" + TncConfig.this.mHasEeprom);
	            	}
	            	break;
	            case MESSAGE_TOAST:
	                Toast.makeText(getApplicationContext(), msg.getData().getString(TOAST),
	                               Toast.LENGTH_SHORT).show();
	                break;
	            }
	        }
	    };
	}


	@Override
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getWindow().requestFeature(Window.FEATURE_ACTION_BAR);
		}
		setContentView(R.layout.activity_main);

		if(D) Log.e(TAG, "+++ ON CREATE +++");

        // Get local Bluetooth adapter
        mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // If the adapter is null, then Bluetooth is not supported
        if (mBluetoothAdapter == null) {
            Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        
        mBluetoothDeviceView = (TextView) findViewById(R.id.bluetooth_device_text);
        mFirmwareVersionView = (TextView) findViewById(R.id.firmware_version_text);
	}

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
            if (mTncService == null) setupConfig();
        }
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
        // Stop the Bluetooth TNC services
        // if (mTncService != null) mTncService.stop();
        if (mTncService != null) mTncService.resetHandler();
        if(D) Log.e(TAG, "--- ON DESTROY ---");
    }


    private void setupConfig() {
        Log.d(TAG, "setupConfig()");

        // Initialize the Connect/Disconnect button.
        // Initialize the 1200Hz button
        // Initialize the 2200Hz button
        // Initialize the Listen button
        // Initialize the Volume control
        // Initialize the Volume meter
        mConnectButton = (ToggleButton) findViewById(R.id.toggleButtonConnect);
        mConnectButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
                // Is the toggle on?
                boolean on = ((ToggleButton) view).isChecked();
                if (on) {
                    // Launch the DeviceListActivity to see devices and do scan
                    Intent selectDeviceIntent = new Intent(
                    		TncConfig.this, DeviceListActivity.class);
                    startActivityForResult(
                    		selectDeviceIntent, REQUEST_CONNECT_DEVICE);
                } else {
                    // Disconnect Bluetooth
                	mTncService.stop();
                }
            }
        });

        mAudioOutputButton = (Button) findViewById(R.id.audioOutputButton);
        mAudioOutputButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {

        		FragmentManager fragmentManager = getSupportFragmentManager();
        		AudioOutputFragment audioOutputFragment = (AudioOutputFragment) fragmentManager.findFragmentByTag("AudioOutputFragment");
        		if (audioOutputFragment == null) {
        			audioOutputFragment = new AudioOutputFragment();
        		}
               	if (mHasPttStyle) {
            		audioOutputFragment.setPttStyle(mPttStyle);
            	}

            	audioOutputFragment.setVolume(mOutputVolume);

            	FrameLayout fragmentView = (FrameLayout) findViewById(R.id.fragment_view);
            	if (fragmentView != null) {
            		audioOutputFragment.setShowsDialog(false);
	            	audioOutputFragment.setRetainInstance(false);
            		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
	            	fragmentTransaction.replace(R.id.fragment_view, audioOutputFragment, "AudioOutputFragment");
                   	fragmentTransaction.commit();
            	} else {
            		audioOutputFragment.setShowsDialog(true);
	            	audioOutputFragment.setRetainInstance(false);
            		Fragment fragment = fragmentManager.findFragmentByTag("AudioOutputFragment");
            		if (fragment != null) {
            			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            			fragmentTransaction.remove(fragment);
            			fragmentTransaction.commit();
            		}
	            	audioOutputFragment.show(fragmentManager, "AudioOutputFragment");
            	}
            }
        });
        mAudioOutputButton.getBackground().setAlpha(64);


        mAudioInputButton = (Button) findViewById(R.id.audioInputButton);
        mAudioInputButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	FrameLayout fragmentView = (FrameLayout) findViewById(R.id.fragment_view);

        		FragmentManager fragmentManager = getSupportFragmentManager();
        		AudioInputFragment audioInputFragment = (AudioInputFragment) fragmentManager.findFragmentByTag("AudioInputFragment");
        		if (audioInputFragment == null) {
        			audioInputFragment = new AudioInputFragment();
        		}
        		mAudioInputFragment = audioInputFragment;
        		
            	if (mHasInputAtten) {
            		mAudioInputFragment.setInputAtten(mInputAtten);
            	}

            	if (fragmentView != null) {
            		mAudioInputFragment.setShowsDialog(false);
            		mAudioInputFragment.setRetainInstance(false);
            		
            		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            		fragmentTransaction.replace(R.id.fragment_view, mAudioInputFragment, "AudioInputFragment");
                   	fragmentTransaction.commit();
            	} else {
            		mAudioInputFragment.setShowsDialog(true);
            		mAudioInputFragment.setRetainInstance(false);
            		
            		Fragment fragment = fragmentManager.findFragmentByTag("AudioInputFragment");
            		if (fragment != null) {
            			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            			fragmentTransaction.remove(fragment);
            			fragmentTransaction.commit();
            		}
	            	mAudioInputFragment.show(fragmentManager, "AudioInputFragment");
            	}
            }
        });
        mAudioInputButton.getBackground().setAlpha(64);


        mPowerButton = (Button) findViewById(R.id.powerButton);
        mPowerButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	
            	mTncService.getBatteryLevel();
            	
            	FrameLayout fragmentView = (FrameLayout) findViewById(R.id.fragment_view);

        		FragmentManager fragmentManager = getSupportFragmentManager();
        		PowerFragment powerFragment = (PowerFragment) fragmentManager.findFragmentByTag("PowerFragment");
        		if (powerFragment == null) {
        			powerFragment = new PowerFragment();
        		}
        		mPowerFragment = powerFragment;
        		
            	if (mPowerControl) {
            		mPowerFragment.setPowerOn(mPowerOn);
            		mPowerFragment.setPowerOff(mPowerOff);
            	}

            	mPowerFragment.setBatteryLevel(mBatteryLevel);
 
                if (fragmentView != null) {
            		mPowerFragment.setShowsDialog(false);
            		mPowerFragment.setRetainInstance(false);
            		
            		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            		fragmentTransaction.replace(R.id.fragment_view, mPowerFragment, "PowerFragment");
                   	fragmentTransaction.commit();
            	} else {
            		mPowerFragment.setShowsDialog(true);
            		mPowerFragment.setRetainInstance(false);
            		
            		Fragment fragment = fragmentManager.findFragmentByTag("PowerFragment");
            		if (fragment != null) {
            			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            			fragmentTransaction.remove(fragment);
            			fragmentTransaction.commit();
            		}
            		mPowerFragment.show(fragmentManager, "PowerFragment");
            	}
            }
        });
        mPowerButton.getBackground().setAlpha(64);


        mKissButton = (Button) findViewById(R.id.kissButton);
        mKissButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	FrameLayout fragmentView = (FrameLayout) findViewById(R.id.fragment_view);

        		FragmentManager fragmentManager = getSupportFragmentManager();
        		KissFragment kissFragment = (KissFragment) fragmentManager.findFragmentByTag("KissFragment");
        		if (kissFragment == null) {
        			kissFragment = new KissFragment();
        		}
        		mKissFragment = kissFragment;
        		
        		mKissFragment.setTxDelay(mTxDelay);
        		mKissFragment.setPersistence(mPersistence);
        		mKissFragment.setSlotTime(mSlotTime);
        		mKissFragment.setDuplex(mDuplex);

                if (fragmentView != null) {
                	mKissFragment.setShowsDialog(false);
                	mKissFragment.setRetainInstance(false);
            		
            		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            		fragmentTransaction.replace(R.id.fragment_view, mKissFragment, "KissFragment");
                   	fragmentTransaction.commit();
            	} else {
            		mKissFragment.setShowsDialog(true);
            		mKissFragment.setRetainInstance(false);
            		
            		Fragment fragment = fragmentManager.findFragmentByTag("KissFragment");
            		if (fragment != null) {
            			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            			fragmentTransaction.remove(fragment);
            			fragmentTransaction.commit();
            		}
            		mKissFragment.show(fragmentManager, "KissFragment");
            	}
            }
        });
        mKissButton.getBackground().setAlpha(64);


        mModemButton = (Button) findViewById(R.id.modemButton);
        mModemButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	FrameLayout fragmentView = (FrameLayout) findViewById(R.id.fragment_view);

        		FragmentManager fragmentManager = getSupportFragmentManager();
        		ModemFragment modemFragment = (ModemFragment) fragmentManager.findFragmentByTag("ModemFragment");
        		if (modemFragment == null) {
        			modemFragment = new ModemFragment();
        		}
        		mModemFragment = modemFragment;
        		
        		mModemFragment.setDcd(mDcd);
            	if (mHasConnTrack) {
            		mModemFragment.setConnTrack(mConnTrack);
            	}
            	mModemFragment.setVerbose(mVerbose);

                if (fragmentView != null) {
                	mModemFragment.setShowsDialog(false);
                	mModemFragment.setRetainInstance(false);
            		
            		FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            		fragmentTransaction.replace(R.id.fragment_view, mModemFragment, "ModemFragment");
                   	fragmentTransaction.commit();
            	} else {
            		mModemFragment.setShowsDialog(true);
            		mModemFragment.setRetainInstance(false);
            		
            		Fragment fragment = fragmentManager.findFragmentByTag("ModemFragment");
            		if (fragment != null) {
            			FragmentTransaction fragmentTransaction = fragmentManager.beginTransaction();
            			fragmentTransaction.remove(fragment);
            			fragmentTransaction.commit();
            		}
            		mModemFragment.show(fragmentManager, "ModemFragment");
            	}
            }
        });
        mModemButton.getBackground().setAlpha(64);
       
        // Initialize the BluetoothChatService to perform bluetooth connections
        TncConfigApplication app = (TncConfigApplication) getApplication();
        mTncService = app.getBluetoothTncService();
        createHandler();
        if (mTncService == null) {
            mTncService = new BluetoothTncService(this, mHandler);
            app.setBluetoothTncService(mTncService);
        } else {
        	mTncService.setHandler(mHandler);
        	if (mTncService.isConnected()) {
            	mConnectedDeviceName = mTncService.getDeviceName();
        		onBluetoothConnected();
        		mTncService.getAllValues();
        	}
        }
        
        mSaveButton = (Button) findViewById(R.id.saveButton);
        mSaveButton.setOnClickListener(new OnClickListener() {
            public void onClick(View view) {
            	mTncService.saveEeprom();
            	mNeedsSave = false;
            	mSaveButton.setEnabled(mNeedsSave);
            }
        });
        mSaveButton.getBackground().setAlpha(64);
       
    }


    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
    	super.onPrepareOptionsMenu(menu);
    	return true;
    }
    
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
        return true;
	}

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
        case R.id.action_bluetooth_settings:
            // Launch the DeviceListActivity to see devices and do scan
            Intent bluetoothSettingsIntent = new Intent();
            bluetoothSettingsIntent.setAction(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS);
            startActivity(bluetoothSettingsIntent);
            return true;
        case R.id.action_firmware_update:
        	// Stop the TNC service so the firmware service can connect.
            if (mTncService != null) {
            	mTncService.stop();
            	mTncService = null;
            }
            
            // Launch the browser.
            Uri uri = Uri.parse(getString(R.string.firmware_url));
        	startActivity(new Intent(Intent.ACTION_VIEW, uri));  
            return true;
        case R.id.action_about:
            // Launch the AboutActivity to learn about the program.
            Intent aboutIntent = new Intent(this, AboutActivity.class);
            startActivity(aboutIntent);
            return true;
        }
        return false;
    }
    

	@Override
	public void onAudioOutputDialogClose(AudioOutputFragment dialog) {
		mTncService.ptt(TONE_NONE);
	}

	@Override
	public void onAudioOutputDialogPttStyleChanged(AudioOutputFragment dialog) {
		mPttStyle = dialog.getPttStyle();
		mTncService.setPttChannel(mPttStyle);
		settingsUpdated();
	}

	@Override
	public void onAudioOutputDialogLevelChanged(AudioOutputFragment dialog) {
		mOutputVolume = dialog.getVolume();
		mTncService.volume(mOutputVolume);
		settingsUpdated();
	}

	@Override
	public void onAudioOutputDialogToneChanged(AudioOutputFragment dialog) {
		mPtt = dialog.getPtt();
		mTone = dialog.getTone();
		if (mPtt) {
			mTncService.ptt(mTone);
		} else {
			mTncService.ptt(TONE_NONE);
		}	
	}
	

	@Override
    public void onAudioInputDialogClose(AudioInputFragment dialog) {
    	if (mInputAtten != dialog.getInputAtten()) {
    		mInputAtten = dialog.getInputAtten();
    		mTncService.setInputAtten(mInputAtten);
    	}
    	synchronized (mHandler) {
	    	mTncService.ptt(TONE_NONE);
    	}
    }
    
	@Override
    public void onAudioInputDialogPause(AudioInputFragment dialog) {
    	if (mInputAtten != dialog.getInputAtten()) {
    		mInputAtten = dialog.getInputAtten();
    		mTncService.setInputAtten(mInputAtten);
    	}
    	synchronized (mHandler) {
	    	mTncService.ptt(TONE_NONE);
    	}
    }
    
	@Override
    public void onAudioInputDialogResume(AudioInputFragment dialog) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		AudioInputFragment audioInputFragment = (AudioInputFragment) fragmentManager.findFragmentByTag("AudioInputFragment");
		if (audioInputFragment != mAudioInputFragment) {
			mAudioInputFragment = audioInputFragment;
		}
		mTncService.listen();
    }
    
    @Override
    public void onAudioInputDialogChanged(AudioInputFragment dialog) {
    	if (mInputAtten != dialog.getInputAtten()) {
    		mInputAtten = dialog.getInputAtten();
    		mTncService.setInputAtten(mInputAtten);
    		mTncService.listen();
    		settingsUpdated();
    	}
    }
    
	@Override
    public void onPowerDialogUpdate(PowerFragment dialog) {
    	if (mPowerOn != dialog.getPowerOn()) {
    		mPowerOn = dialog.getPowerOn();
    		mTncService.setUsbPowerOn(mPowerOn);
    		settingsUpdated();
    	}
    	if (mPowerOff != dialog.getPowerOff()) {
    		mPowerOff = dialog.getPowerOff();
    		mTncService.setUsbPowerOff(mPowerOff);
    		settingsUpdated();
    	}
    }
    
	@Override
    public void onPowerDialogResume(PowerFragment dialog) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		PowerFragment powerFragment = (PowerFragment) fragmentManager.findFragmentByTag("PowerFragment");
		if (powerFragment != mPowerFragment) {
			mPowerFragment = powerFragment;
		}
    }
    
    @Override
    public void onKissDialogUpdate(KissFragment dialog) {
    	
        if(D) Log.i(TAG, "onKissDialogPause()");
        if(D) Log.i(TAG, "TxDelay: " + dialog.getTxDelay());
        if(D) Log.i(TAG, "Persistence: " + dialog.getPersistence());
        if(D) Log.i(TAG, "SlotTime: " + dialog.getSlotTime());
        if(D) Log.i(TAG, "Duplex: " + dialog.getDuplex());

        if (mTxDelay != dialog.getTxDelay()) {
    		mTxDelay = dialog.getTxDelay();
    		mTncService.setTxDelay(mTxDelay);
    		settingsUpdated();
    	}
    	if (mPersistence != dialog.getPersistence()) {
    		mPersistence = dialog.getPersistence();
    		mTncService.setPersistence(mPersistence);
    		settingsUpdated();
    	}
    	if (mSlotTime != dialog.getSlotTime()) {
    		mSlotTime = dialog.getSlotTime();
    		mTncService.setSlotTime(mSlotTime);
    		settingsUpdated();
    	}
    	if (mTxTail != dialog.getTxTail()) {
    		mTxTail = dialog.getTxTail();
    		mTncService.setSlotTime(mTxTail);
    		settingsUpdated();
    	}
    	if (mDuplex != dialog.getDuplex()) {
    		mDuplex = dialog.getDuplex();
    		mTncService.setDuplex(mDuplex);
    		settingsUpdated();
    	}
    }
    
	@Override
    public void onKissDialogResume(KissFragment dialog) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		KissFragment kissFragment = (KissFragment) fragmentManager.findFragmentByTag("KissFragment");
		if (kissFragment != mKissFragment) {
			mKissFragment = kissFragment;
		}
    }
    
    @Override
    public void onModemDialogUpdate(ModemFragment dialog) {
        if(D) Log.i(TAG, "onModemDialogPause()");
        if(D) Log.i(TAG, "DCD: " + dialog.getDcd());
        if(D) Log.i(TAG, "Has ConnTrack: " + dialog.hasConnTrack());
        if(D && dialog.hasConnTrack()) Log.i(TAG, "ConnTrack: " + dialog.getConnTrack());
        if(D) Log.i(TAG, "Verbose: " + dialog.getVerbose());
        
        if (mDcd != dialog.getDcd()) {
        	mDcd = dialog.getDcd();
        	mTncService.setDcd(mDcd);
    		settingsUpdated();
        }
        
        if (mHasConnTrack && (mConnTrack != dialog.getConnTrack())) {
        	mConnTrack = dialog.getConnTrack();
        	mTncService.setConnTrack(mConnTrack);
    		settingsUpdated();
        }
        
        if (mVerbose != dialog.getVerbose()) {
        	mVerbose = dialog.getVerbose();
        	mTncService.setVerbosity(mVerbose);
    		settingsUpdated();
        }
    }
    
	@Override
    public void onModemDialogResume(ModemFragment dialog) {
		FragmentManager fragmentManager = getSupportFragmentManager();
		ModemFragment modemFragment = (ModemFragment) fragmentManager.findFragmentByTag("ModemFragment");
		if (modemFragment != mModemFragment) {
			mModemFragment = modemFragment;
		}
    }
    
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        if(D) Log.d(TAG, "onActivityResult " + resultCode);
        switch (requestCode) {
        case REQUEST_CONNECT_DEVICE:
            // When DeviceListActivity returns with a device to connect
            if (resultCode == ActionBarActivity.RESULT_OK) {
                // Get the device MAC address
                String address = data.getExtras()
                                     .getString(DeviceListActivity.EXTRA_DEVICE_ADDRESS);
                // Get the BLuetoothDevice object
                BluetoothDevice device = mBluetoothAdapter.getRemoteDevice(address);
                // Attempt to connect to the device
                mTncService.connect(device);
            }
            break;
        case REQUEST_ENABLE_BT:
            // When the request to enable Bluetooth returns
            if (resultCode != ActionBarActivity.RESULT_OK) {
                // User did not enable Bluetooth or an error occured
                Log.d(TAG, "BT not enabled");
                Toast.makeText(this, R.string.bt_not_enabled_leaving, Toast.LENGTH_SHORT).show();
                finish();
            }
        }
    }

}
