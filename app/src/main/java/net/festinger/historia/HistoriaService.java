package net.festinger.historia;

import android.app.IntentService;
import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.util.Log;

public class HistoriaService extends Service {

	public static boolean isServiceStarted = false;
	private static boolean isContentObserverRegistered = false;
	
	 @Override
	    public IBinder onBind(Intent intent) {
	        return null;
	    }

	  /**
	   * The IntentService calls this method from the default worker thread with
	   * the intent that started the service. When this method returns, IntentService
	   * stops the service, as appropriate.
	   */
	    @Override
	    public void onCreate() {
	        super.onCreate();
	        Main.getSettings(getApplicationContext());
//	        Log.d("HistoriaonCreate", "onCreate");
	        isServiceStarted = true;
	        if (isContentObserverRegistered == false)
	 	   	{
	 	    CallLogContentObserver callLogContentObserver = new CallLogContentObserver(new Handler(), getApplicationContext());
	        this.getApplicationContext()
	 	    .getContentResolver()
	 	    .registerContentObserver(
	 	            android.provider.CallLog.Calls.CONTENT_URI, true,callLogContentObserver); 
	        isContentObserverRegistered = true;
	 	   	}
//	        Log.d("onCreate","onCreate");
	    }
	    @Override
	    public int onStartCommand(Intent intent, int flags, int startId) {
//	        Log.d("HistoriaonStartCommand","onStartCommand");
	        super.onStartCommand(intent, flags, startId); // If this is not written then onHandleIntent is not called.
	        return START_STICKY;
	    }
	}