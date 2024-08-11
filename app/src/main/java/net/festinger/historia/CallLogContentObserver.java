package net.festinger.historia;

import android.content.Context;
import android.database.ContentObserver;
import android.location.Location;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Handler;
import android.util.Log;

public class CallLogContentObserver extends ContentObserver {
	Context context;

	public CallLogContentObserver(Handler h, Context context) {
		super(h);
		this.context = context;
//		Log.d("CallLogContentObserver", "CallLogContentObserver");
	}

	@Override
	public boolean deliverSelfNotifications() {
		return true;
	}


	@Override
	public void onChange(boolean selfChange) {
//		Log.d("call ended", "call ended");
		CallStrings callStrings = Main.getCalls(context);
		if (callStrings !=  null)
		{
			Main.isListDirty = true;
			
			Main.saveCSV(context); // store new calls in csv file
			Main.addCalendarEvent(callStrings, context);
		}
	}
}
