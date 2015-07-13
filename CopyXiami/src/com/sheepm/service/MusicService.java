package com.sheepm.service;

import java.util.List;

import com.sheepm.Utils.Constants;
import com.sheepm.bean.Mp3Info;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnCompletionListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.os.IBinder;
import android.util.Log;

public class MusicService extends Service implements OnPreparedListener,
		OnCompletionListener {

	private MediaPlayer player;
	private MyBroadcastReceiver receiver;
	private List<Mp3Info> mp3Infos;
	private int position;
	private boolean isFirst = true;
	private int current;

	@Override
	public void onCreate() {
		Log.i("music service", "oncreate");
		super.onCreate();
		regFilter();
	}

	/*
	 * 注册广播
	 */
	private void regFilter() {
		IntentFilter filter = new IntentFilter();
		filter.addAction(Constants.ACTION_LIST_SEARCH);
		filter.addAction(Constants.ACTION_PAUSE);
		filter.addAction(Constants.ACTION_PLAY);
		filter.addAction(Constants.ACTION_NEXT);
		filter.addAction(Constants.ACTION_PRV);
		receiver = new MyBroadcastReceiver();
		registerReceiver(receiver, filter); // 注册接收
	}

	@Override
	public IBinder onBind(Intent arg0) {
		return null;
	}

	@Override
	public void onDestroy() {
		Log.i("music service", "ondestroy");
		super.onDestroy();
		if (receiver!=null) {
			unregisterReceiver(receiver); // 服务终止时解绑
		}
		if (player != null) {
			player.release();
			player = null;
		}
	}

	// api2.0以后采用onStartCommand
	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		Log.i("music service", "onstartcommand");
		player = new MediaPlayer();
		mp3Infos = intent.getParcelableArrayListExtra("mp3Infos");
		return super.onStartCommand(intent, flags, startId);
	}

	/*
	 * 创建自定义的广播接收器
	 */
	public class MyBroadcastReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			if (intent.getAction().equals(Constants.ACTION_LIST_SEARCH)) {
				long id = intent.getLongExtra("id", 0);
				for (int i = 0; i < mp3Infos.size(); i++) {
					if (id == mp3Infos.get(i).getId()) {
						position = i;
						prepareMusic(position);
						isFirst = false;
						break;
					}
				}
			} else if (intent.getAction().equals(Constants.ACTION_PAUSE)) {
				if (player.isPlaying()) {
					pauseMusic();
				}
			} else if (intent.getAction().equals(Constants.ACTION_PLAY)) {
				if (!player.isPlaying()) {
					if (isFirst) {
						position = intent.getIntExtra("position", 0);
						prepareMusic(position);
						isFirst  =false;
					}else {
						player.seekTo(current);
						player.start();
					}
				}
			}else if (intent.getAction().equals(Constants.ACTION_NEXT)) {
				if (position < mp3Infos.size() -1 ) {
					prepareMusic(position+1);
					++position;
				}else {
					prepareMusic(0);
					position = 0;
				}
				
			}else if (intent.getAction().equals(Constants.ACTION_PRV)) {
				if (position == 0) {
					prepareMusic(mp3Infos.size()-1);
					position = mp3Infos.size()-1;
				}else {
					prepareMusic(position -1);
					--position;
				}
			}
		}

	}

	/*
	 * 准备播放音乐并添加播放与完结的事件监听
	 */
	private void prepareMusic(int position) {
		player.reset();
		player.setAudioStreamType(AudioManager.STREAM_MUSIC); // 设置播放类型
		String url = mp3Infos.get(position).getUrl();
		try {
			player.setDataSource(url);
		} catch (Exception e) {
			e.printStackTrace();
		}
		player.setOnPreparedListener(this);
		player.setOnCompletionListener(this);
		player.prepareAsync(); // prepare是阻塞的 prepareAsync是异步的
	}

	/**
	 * 音乐播放完成的回调函数
	 */
	@Override
	public void onCompletion(MediaPlayer arg0) {
		Log.i("music service", "oncompletion");
		Intent intent = new Intent();
		intent.setAction(Constants.ACTION_NEXT);
		sendBroadcast(intent);
		
	}

	/**
	 * 音乐准备完成的回调函数
	 */
	@Override
	public void onPrepared(MediaPlayer arg0) {
		Log.i("music service", "onprepare");
		player.start();
	}

	public void pauseMusic() {
		Log.i("pause", "pause");
		player.pause();
		current = player.getCurrentPosition();
	}
}
