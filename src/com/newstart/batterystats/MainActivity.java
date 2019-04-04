package com.newstart.batterystats;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FilenameFilter;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.SystemClock;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity implements View.OnClickListener {
	private static final String TAG = "BatteryStats";

	private static final String DATA_PATH = Environment.getExternalStorageDirectory().getAbsolutePath() + "/BatteryStats/";
	private StatData[] mData = new StatData[101], mOptData = new StatData[101];
	private boolean mStartedStat = false, mShowLines = false;
	private Button mBtnStartStop, mBtnStatType, mBtnExport, mBtnImport;
	private Chronometer mStartTime;
	private FrameLayout mStatLayout;
	private LinearLayout mTableLayout;
	private LinesView mLinesLayout;
	private TextView mTextNoData;
	private EditText mPowerOffVoltValue;
	private long mBeginTime = 0;
	private int mCurrentPrecent = -1, mCurrentVolt = 0;
	private ProgressDialog mProgressDialog;
	private String mLastSavedFilePath;
	
	private BroadcastReceiver mReceiver = new BroadcastReceiver() {

		@Override
		public void onReceive(Context context, Intent intent) {
			int level = intent.getIntExtra("level", 0);
			int scale = intent.getIntExtra("scale", 100);
			int voltage = intent.getIntExtra("voltage", 0);
			
			mCurrentPrecent = (level * 100)/ scale;
			mCurrentVolt = voltage;
			
			
			try {
				int poweroffVolt = Integer.parseInt(mPowerOffVoltValue.getText().toString());
				if(mCurrentVolt <= poweroffVolt) {
					mCurrentPrecent = 0;
				}
			} catch(NumberFormatException e) {
				e.printStackTrace();
			}
			
			if(mStartedStat) {
				if(mData[mCurrentPrecent].time == -1) {
					mData[mCurrentPrecent].precent = mCurrentPrecent;
					mData[mCurrentPrecent].volt = voltage;
					if(mBeginTime == 0) {
						mBeginTime = SystemClock.elapsedRealtime();
						mData[mCurrentPrecent].time = 0;
					} else {
						mData[mCurrentPrecent].time = (int) ((SystemClock.elapsedRealtime() - mBeginTime) / 1000);
					}
					mData[mCurrentPrecent].data.add(0, new StatDataSub(1, mData[mCurrentPrecent].volt, mData[mCurrentPrecent].time));
				} else {
					mData[mCurrentPrecent].data.add(new StatDataSub(0, voltage, (int) ((SystemClock.elapsedRealtime() - mBeginTime) / 1000)));
				}

				
				if(mCurrentPrecent == 1 || mCurrentPrecent == 0) {
					try {
						new File(mLastSavedFilePath).delete();
					} catch(Exception e) {
						e.printStackTrace();
					}
					
					exportData(true);
				}
			}
			
			Log.d(TAG, "level=" + level + ", scale=" + scale + ", voltage=" + voltage + ", mCurrentPrecent=" + mCurrentPrecent);
		}
		
	};
	
	private Handler mHandler = new Handler() {

		@Override
		public void handleMessage(Message msg) {
			int what = msg.what;
			
			switch(what) {
				case 1:{
					showProgressDialog();
					
					View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.table_button, null);
					Button btnNormal = (Button) view.findViewById(R.id.btn_table_normal);
					Button btnOpt = (Button) view.findViewById(R.id.btn_table_opt);
					
					btnNormal.setText("Normal");
					btnOpt.setText("Optimization");
					btnNormal.setOnClickListener(MainActivity.this);
					btnOpt.setOnClickListener(MainActivity.this);
					
					mTableLayout.addView(view, 0);
					
					sendEmptyMessage(2);
					break;
				}
				case 2:{
					View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.table_item, null);
					TextView percent = (TextView) view.findViewById(R.id.percent);
					TextView total = (TextView) view.findViewById(R.id.total_time);
					TextView indivi = (TextView) view.findViewById(R.id.indivi_time);
					TextView volt = (TextView) view.findViewById(R.id.volt);
					
					percent.setText("Precent");
					total.setText("Total Time");
					indivi.setText("Indivi Time");
					volt.setText("Volt");
					
					mTableLayout.addView(view, 1);
					
					sendMessage(Message.obtain(mHandler, 3, 100, 0));
					break;
				}
				case 3:{
					int i = msg.arg1;
					
					View view = LayoutInflater.from(getApplicationContext()).inflate(R.layout.table_item, null);
					TextView percent = (TextView) view.findViewById(R.id.percent);
					TextView total = (TextView) view.findViewById(R.id.total_time);
					TextView indivi = (TextView) view.findViewById(R.id.indivi_time);
					TextView volt = (TextView) view.findViewById(R.id.volt);
					
					if(mData[i].time != -1) {
						percent.setText(String.valueOf(mData[i].precent));
						if(mData[i].time == 0) {
							total.setText("0");
							indivi.setText("0");
						} else {
							total.setText(String.valueOf(mData[i].getTimeMinute()));
							if(i < 100) {
								indivi.setText(String.valueOf(mData[i].getTimeMinute() - mData[i + 1].getTimeMinute()));
							} else {
								indivi.setText(String.valueOf(mData[i].getTimeMinute()));
							}
						}
						volt.setText(String.valueOf(mData[i].volt));
					} else {
						percent.setText(String.valueOf(mData[i].precent));
						total.setText("N");
						indivi.setText("N");
						volt.setText("N");
					}

					mTableLayout.addView(view,102 - i);
					
					if(i == 0) {
						view.findViewById(R.id.bottomDiv).setVisibility(View.VISIBLE);
						dismissProgressDialog();
					} else {
						sendMessage(Message.obtain(mHandler, 3, i - 1, 0));
					}
					break;
				}
			}
		}
		
	};
	
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);
		
		mStartedStat = false;
		getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_PAN);
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
		WindowManager.LayoutParams lp = getWindow().getAttributes();
		lp.screenBrightness = 1.0f;
		getWindow().setAttributes(lp);
		
		TextView model = (TextView) findViewById(R.id.model_name);
		model.setText(android.os.Build.MODEL);
		
		TextView poweroff = (TextView) findViewById(R.id.poweroff_volt);
		poweroff.setText("Poweroff volt:");
		
		mBtnStartStop = (Button) findViewById(R.id.btn_startstop);
		mBtnStartStop.setText("Start");
		mBtnStartStop.setOnClickListener(this);
		
		mBtnStatType = (Button) findViewById(R.id.btn_stattype);
		mBtnStatType.setText("Lines");
		mBtnStatType.setOnClickListener(this);
		
		mBtnExport = (Button) findViewById(R.id.btn_export);
		mBtnExport.setText("Export");
		mBtnExport.setOnClickListener(this);
		
		mBtnImport = (Button) findViewById(R.id.btn_import);
		mBtnImport.setText("Import");
		mBtnImport.setOnClickListener(this);
		
		mStartTime = (Chronometer) findViewById(R.id.starttime);
		mStatLayout = (FrameLayout) findViewById(R.id.stat_layout);
		mTableLayout = (LinearLayout) findViewById(R.id.table_layout);
		mLinesLayout = (LinesView) findViewById(R.id.lines_layout);
		mTextNoData = (TextView) findViewById(R.id.nodata);
		mPowerOffVoltValue = (EditText) findViewById(R.id.poweroff_volt_value);
		
		mTextNoData.setText("No data");
		mPowerOffVoltValue.setText("3250");
		mStartTime.setTextColor(Color.parseColor("#FF0000"));
		mStartTime.setOnChronometerTickListener(new Chronometer.OnChronometerTickListener() {
			
			@Override
			public void onChronometerTick(Chronometer chronometer) {
				long time = SystemClock.elapsedRealtime() - chronometer.getBase();
                Date d = new Date(time);
                SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.US);
                sdf.setTimeZone(TimeZone.getTimeZone("UTC"));
                mStartTime.setText(sdf.format(d));
			}
		});
		
		IntentFilter intentFilter = new IntentFilter(Intent.ACTION_BATTERY_CHANGED);
		registerReceiver(mReceiver, intentFilter);
		
		initData();
	}
	
	

	@Override
	protected void onDestroy() {
		super.onDestroy();
		
		unregisterReceiver(mReceiver);
	}



	@Override
	public void onClick(View v) {
		int id = v.getId();
		
		switch(id) {
			case R.id.btn_startstop:
				if(mStartedStat) {
					mBtnStartStop.setText("Start");
					mStartedStat = false;
					mStartTime.setVisibility(View.INVISIBLE);
					mStatLayout.setVisibility(View.VISIBLE);
					mBtnStatType.setEnabled(true);
					mBtnExport.setEnabled(true);
					mBtnImport.setEnabled(true);
					
					mStartTime.stop();
					mBeginTime = 0;
					
					forceLoadViews();
				} else {
					mBtnStartStop.setText("Stop");
					mStartedStat = true;
					mStartTime.setVisibility(View.VISIBLE);
					mStatLayout.setVisibility(View.INVISIBLE);
					mTextNoData.setVisibility(View.GONE);
					mBtnStatType.setEnabled(false);
					mBtnExport.setEnabled(false);
					mBtnImport.setEnabled(false);
					
					initData();
					long beginTime = SystemClock.elapsedRealtime();
					mStartTime.setBase(beginTime);
					mStartTime.start();
					
					if(mCurrentPrecent != -1) {
						mBeginTime = beginTime;
						mData[mCurrentPrecent].precent = mCurrentPrecent;
						mData[mCurrentPrecent].volt = mCurrentVolt;
						mData[mCurrentPrecent].time = 0;
						mData[mCurrentPrecent].data.add(0, new StatDataSub(1, mData[mCurrentPrecent].volt, mData[mCurrentPrecent].time));
					}

				}
				break;
			case R.id.btn_stattype:
				if(mStartedStat) {
					Toast.makeText(getApplicationContext(), "Please stop first!", Toast.LENGTH_SHORT).show();
				} else {
					mStatLayout.setVisibility(View.VISIBLE);
					if(mShowLines) {
						mShowLines = false;
						mLinesLayout.setVisibility(View.VISIBLE);
						mTableLayout.setVisibility(View.INVISIBLE);
						mBtnStatType.setText("Table");
					} else {
						mShowLines = true;
						mLinesLayout.setVisibility(View.INVISIBLE);
						mTableLayout.setVisibility(View.VISIBLE);
						mBtnStatType.setText("Lines");
					}
					if(hasValidData()) {
						mTextNoData.setVisibility(View.GONE);
						if(mShowLines) {
							loadTableViews();
						} else {
							loadLinesViews();
						}
					} else {
						mTextNoData.setVisibility(View.VISIBLE);
					}

				}
				break;
			case R.id.btn_export:
				exportData(false);
				break;
			case R.id.btn_import:
				importData();
				break;
			case R.id.btn_table_normal:
				refreshTableView(false);
				break;
			case R.id.btn_table_opt:
				if(mData[100].time == -1 || mData[1].time == -1) {
					Toast.makeText(getApplicationContext(), "Please record full discharging data!", Toast.LENGTH_SHORT).show();
				} else {
					refreshTableView(true);
				}
				break;
		}
	}
	
	private void showProgressDialog() {
		if(mProgressDialog == null) {
			mProgressDialog = new ProgressDialog(this);
			mProgressDialog.setMessage("Loading");
			mProgressDialog.setCancelable(false);
		}
		mProgressDialog.show();
	}
	
	private void dismissProgressDialog() {
		if(mProgressDialog != null && mProgressDialog.isShowing()) {
			mProgressDialog.dismiss();
		}
	}
	
	private void initData() {
		for(int i  = 0; i < mData.length; i++) {
			if(mData[i] == null) {
				mData[i] = new StatData(i, 0, -1);
			}
			mData[i].precent = i;
			mData[i].volt = 0;
			mData[i].time = -1;
			mData[i].data.clear();
		}
	}
	
	private boolean hasValidData() {
		int count = 0;
		for(int i  = 0; i < mData.length; i++) {
			if(mData[i] != null && mData[i].time != -1) {
				count++;
				if(count >= 2) {
					return true;
				}
			}
		}
		return false;
	}
	
	private void forceLoadViews() {
		mLinesLayout.setHasDrawn(false);
		mTableLayout.removeAllViews();
		mBtnStatType.performClick();
	}
	
	private void loadLinesViews() {
		if(!mLinesLayout.hasDrawn()) {
			mLinesLayout.setData(mData);
			mLinesLayout.setHasDrawn(true);
		}
	}
	
	private void loadTableViews() {
		if(mTableLayout.getChildCount() == 0) {
			mHandler.sendEmptyMessage(1);
		}
	}
	
	private void refreshTableView(boolean isOpt) {
		if(mTableLayout.getChildCount() == 103) {
			for(int i= 0; i < mOptData.length; i++) {
				if(mOptData[i] == null) {
					mOptData[i] = new StatData(i, 0, -1);
				}
				mOptData[i].precent = i;
				mOptData[i].volt = 0;
				mOptData[i].time = -1;
			}
			
			TextView percent, total, indivi, volt;
			int index;
			for(int i = 2; i < mTableLayout.getChildCount(); i++) {
				View view = mTableLayout.getChildAt(i);
				percent = (TextView) view.findViewById(R.id.percent);
				total = (TextView) view.findViewById(R.id.total_time);
				indivi = (TextView) view.findViewById(R.id.indivi_time);
				volt = (TextView) view.findViewById(R.id.volt);
				
				if(isOpt) {
					int maxTime = 0;
					for(int j = 0; j < mData.length; j++) {
						for(StatDataSub sub : mData[j].data) {
							maxTime = sub.time > maxTime ? sub.time : maxTime;
						}
					}
					
					index = 102 - i;
					if(mData[index].time != -1) {
						percent.setText(String.valueOf(mData[index].precent));
						if(mData[index].time == 0) {
							total.setText("0");
							indivi.setText("0");
							volt.setText(String.valueOf(mData[index].volt));
							
							mOptData[index].precent = index;
							mOptData[index].volt = mData[index].volt;
							mOptData[index].time = 0;
						} else {
							int needTotoalTime = (100 - index) * maxTime / 100;
							StatDataSub sub = getBestData(needTotoalTime);
							
							mOptData[index].precent = index;
							if(sub != null) {
								mOptData[index].volt = sub.volt;
								mOptData[index].time = sub.time;
								
								total.setText(String.valueOf(mOptData[index].getTimeMinute()));
								if(index < 100) {
									indivi.setText(String.valueOf(mOptData[index].getTimeMinute() - mOptData[index + 1].getTimeMinute()));
								} else {
									indivi.setText(String.valueOf(mOptData[index].getTimeMinute()));
								}
								volt.setText(String.valueOf(mOptData[index].volt));
							} else {
								total.setText("N");
								indivi.setText("N");
								volt.setText("N");
							}
						}
					} else {
						percent.setText(String.valueOf(mData[index].precent));
						total.setText("N");
						indivi.setText("N");
						volt.setText("N");
					}
				} else {
					index = 102 - i;
					if(mData[index].time != -1) {
						percent.setText(String.valueOf(mData[index].precent));
						if(mData[index].time == 0) {
							total.setText("0");
							indivi.setText("0");
						} else {
							total.setText(String.valueOf(mData[index].getTimeMinute()));
							if(index < 100) {
								indivi.setText(String.valueOf(mData[index].getTimeMinute() - mData[index + 1].getTimeMinute()));
							} else {
								indivi.setText(String.valueOf(mData[index].getTimeMinute()));
							}
						}
						volt.setText(String.valueOf(mData[index].volt));
					} else {
						percent.setText(String.valueOf(mData[index].precent));
						total.setText("N");
						indivi.setText("N");
						volt.setText("N");
					}
				}
			}
		}
	}
	
	private void checkSaveDir() {
		File dir = new File(DATA_PATH);
		if(!dir.exists()) {
			dir.mkdir();
		}
	}
	
	private void exportData(boolean force) {
		if(mStartedStat && !force) {
			Toast.makeText(getApplicationContext(), "Please stop first!", Toast.LENGTH_SHORT).show();
		} else {
			if(hasValidData()) {
				checkSaveDir();
                Date d = new Date();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyyMMddHHmmssS", Locale.US);
				File file = new File(DATA_PATH + "BatteryStats_" + sdf.format(d) + ".txt");
				try {
					file.createNewFile();
					mLastSavedFilePath = file.getAbsolutePath();
					FileOutputStream fos = new FileOutputStream(file);
					for(int i = mData.length - 1; i >= 0; i--) {
						if(mData[i].data.size() > 0) {
							for(StatDataSub subData : mData[i].data) {
								String buff = subData.begin + "," + mData[i].precent + "," + subData.volt + "," +subData.time + "\n";
								fos.write(buff.getBytes());
							}
						} else {
							String buff = 0 + "," + mData[i].precent + "," + mData[i].volt + "," +mData[i].time + "\n";
							fos.write(buff.getBytes());
						}
					}
					fos.close();
					Toast.makeText(getApplicationContext(), "Save file to " + file.getAbsolutePath(), Toast.LENGTH_SHORT).show();
				} catch (IOException e) {
					e.printStackTrace();
				}
			} else {
				Toast.makeText(getApplicationContext(), "Need record more data!", Toast.LENGTH_SHORT).show();
			}
		}
	}
	
	
	private void importData() {
		if(mStartedStat) {
			Toast.makeText(getApplicationContext(), "Please stop first!", Toast.LENGTH_SHORT).show();
		} else {
			checkSaveDir();
			File dir = new File(DATA_PATH);
			File[] files = dir.listFiles(new FilenameFilter() {
				@Override
				public boolean accept(File dir, String name) {
					return name.startsWith("BatteryStats_");
				}
			});
			
			List<File> fileList = Arrays.asList(files);
			Collections.sort(fileList, new Comparator<File>() {
	            @Override
	            public int compare(File o1, File o2) {
	                if (o1.isDirectory() && o2.isFile())
	                    return -1;
	                if (o1.isFile() && o2.isDirectory())
	                    return 1;
	                return o1.getName().compareTo(o2.getName());
	            }
	        });
			
			if(files != null && files.length > 0) {
				File file = files[files.length - 1];
				try {
					initData();
					BufferedReader br = new BufferedReader(new FileReader(file));
					String data;
					int count = 0;
					int volt, time;
					while((data = br.readLine()) != null) {
						String[] lineData = data.split(",");
						if(lineData != null && lineData.length == 4) {
							int begin = Integer.parseInt(lineData[0]);
							int precent = Integer.parseInt(lineData[1]);
							if(precent > 100 || precent < 0 || (begin != 0 && begin != 1)) {
								br.close();
								throw new NumberFormatException();
							} else {
								mData[precent].precent = precent;
								volt = Integer.parseInt(lineData[2]);
								time = Integer.parseInt(lineData[3]);
								if(begin == 1 && mData[precent].time == -1) {
									mData[precent].volt = volt;
									mData[precent].time = time;
									mData[precent].data.add(0, new StatDataSub(1, volt, time));
									count++;
								}  else {
									mData[precent].data.add(new StatDataSub(0, volt, time));
								}
							}
						} else {
							initData();
							Toast.makeText(getApplicationContext(), "Wrong data!", Toast.LENGTH_SHORT).show();
							br.close();
							break;
						}
					}
					br.close();
					if(count >= 2) {
						forceLoadViews();
						Toast.makeText(getApplicationContext(), "Load data successfully!", Toast.LENGTH_SHORT).show();
					} else {
						initData();
						Toast.makeText(getApplicationContext(), "Miss some data!", Toast.LENGTH_SHORT).show();
					}
				} catch (FileNotFoundException e) {
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				} catch(NumberFormatException e) {
					initData();
					Toast.makeText(getApplicationContext(), "Wrong data!", Toast.LENGTH_SHORT).show();
					e.printStackTrace();
				}
			}
		}
	}
	
	private StatDataSub getBestData(int needTotoalTime) {
		for(int i = mData.length - 1; i >= 0; i--) {
			for(StatDataSub sub : mData[i].data) {
				if(sub.time >= needTotoalTime) {
					return sub;
				}
			}
		}
		return null;
	}
	
	public static class StatData {
		int precent, volt, time;
		ArrayList<StatDataSub> data;
		
		public StatData(int precent, int volt, int time) {
			this.precent = precent;
			this.volt = volt;
			this.time = time;
			data = new ArrayList<StatDataSub>();
		}
		
		public int getTimeMinute() {
			return Math.round(time / 60.f);
		}
	}
	
	public static class StatDataSub {
		int begin, volt, time;
		
		public StatDataSub(int begin, int volt, int time) {
			this.begin = begin;
			this.volt = volt;
			this.time = time;
		}
		
		public int getTimeMinute() {
			return Math.round(time / 60.f);
		}
	}
}
