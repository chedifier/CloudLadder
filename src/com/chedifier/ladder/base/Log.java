package com.chedifier.ladder.base;

import java.io.File;
import java.util.ArrayList;

import com.chedifier.ladder.base.JobScheduler.Job;
import com.chedifier.ladder.base.ObjectPool.IConstructor;
import com.chedifier.ladder.memory.ByteBufferPool;
import com.chedifier.ladder.socks5.Configuration;
import com.chedifier.ladder.socks5.SProxy;

public class Log {
	
	private static final int LOG_DEBUG = 1;
	private static final int LOG_INFO = 2;
	
	
	private static int sLogLevel = 0;
	private static String sLogDir;
	private static final String DEF_DIR = Configuration.DEFAULT_LOG_PATH;
	
	private static final int MAX_SIZE = 100;
	private static final int LOG_TIME_ZONE = 3600*1000;//seperate logs by time,put logs have same time zone together.
	private static ArrayList<String> sCache = new ArrayList<>(MAX_SIZE);
	private static volatile long sLastDumpTime = 0L;
	
	private static ObjectPool<LogDumper> sDumperPool = new ObjectPool<>(new IConstructor<LogDumper>() {

		@Override
		public LogDumper newInstance(Object... params) {
			return new LogDumper((String)params[0]);
		}

		@Override
		public void initialize(LogDumper e, Object... params) {
			e.cb = null;
		}
	}, 20);
	
	public static final void setLogDir(String dir) {
		sLogDir = dir;
	}
	
	public static final void setLogLevel(int level) {
		sLogLevel = level;
	}

	public static final void i(String tag,String content) {
		if(sLogLevel >= LOG_INFO) {
			System.out.println(DateUtils.getCurrentDate() + " : " + "I> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void d(String tag,String content) {
		if(sLogLevel >= LOG_DEBUG) {
			System.out.println(DateUtils.getCurrentDate() + " : " + "D> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
		}
	}
	
	public static final void e(String tag,String content) {
		String s = DateUtils.getCurrentDate() + " : " + "E> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
		System.err.println(s);
		addLog(s);
	}
	
	public static final void r(String tag,String content) {
		if(sLogLevel >= 0) {
			String s = DateUtils.getCurrentDate() + " : " + "R> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content;
			System.out.println(s);
			addLog(s);
		}
	}
	
	public static final void t(String tag,String content) {
		System.out.println(DateUtils.getCurrentDate() + " : " + "T> tid[" + Thread.currentThread().getId() + "] " + tag + " >> " + content);
	}
	
	private static final void addLog(String s) {
		synchronized (sCache) {
			sCache.add(s);
		}
		
		if(sCache.size() >= MAX_SIZE) {
			dumpLog2File();
		}
	}
	
	private static synchronized final String getLogFilePath() {
		
		long now = System.currentTimeMillis();
		if((now-sLastDumpTime) > LOG_TIME_ZONE) {
			sLastDumpTime = now;
		}
		long time = sLastDumpTime;
		
		
		String dir = DEF_DIR;
		if(!StringUtils.isEmpty(sLogDir)) {
			dir = sLogDir;
		}
		
		if(!sLogDir.endsWith(File.separator)) {
			dir += File.separator + SProxy.getBirthDay() + File.separator;
		}
		
		if(!initLogDir(dir)) {
			Log.e("Log", "init log path failed.");
			return null;
		}
		
		return dir + DateUtils.getDate(time) + ".txt";
	}
	
	private static final boolean initLogDir(String sDir) {
		File dir = new File(sDir);
		if(!dir.exists()) {
			if(!dir.mkdirs()) {
				return false;
			}
		}else if(!dir.isDirectory()) {
			return false;
		}
		
		return true;
	}
	
	public static final void dumpLog2File() {
		dumpLog2File(null);
	}
	
	public static final void dumpLog2File(final ICallback cb) {
		LogDumper dumper = sDumperPool.obtain("log-dumper");
		dumper.cb = cb;
		JobScheduler.schedule(dumper);
		
	}
	
	private static class LogDumper extends Job{
		public LogDumper(String tag) {
			super(tag);
		}
		
		ICallback cb;
		@Override
		public void run() {
			StringBuilder sb = null;
			synchronized (sCache) {
				if(!sCache.isEmpty()) {
					sb = new StringBuilder(1024);
					for(String s:sCache) {
						sb.append(s).append("\n\r");
					}
					
					sCache.clear();
				}
			}
			
			if(sb != null && sb.length() > 0) {
				String path = getLogFilePath();
				FileUtils.writeString2File(path, sb.toString(), true);
			}
			
			sDumperPool.recycle(this);
			
			if(cb != null) {
				cb.onDumpFinish();
			}
		}
		
	}
	
	public static final void dumpBeforeExit(ICallback callback) {
		final String TAG = "dumper";
		Log.r(TAG, "\n\n\n");
		Log.r(TAG, "----------------------run-info-begin-------------------");
		Log.r(TAG, "SProxy info: " + SProxy.dumpInfo());
		Log.r(TAG, ByteBufferPool.dumpInfo());
		Log.r(TAG, "----------------------run-info-end---------------------\n\n\n\n");
		
		Log.dumpLog2File(callback);
	}
	
	public interface ICallback{
		void onDumpFinish();
	}
	
}
