package net.festinger.historia;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class HistoriaReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        if (HistoriaService.isServiceStarted == false)
        {
//        	Log.d("HistoriaonReceive", "onReceive");
        	Intent serviceIntent = new Intent(context, HistoriaService.class);
        	context.startService(serviceIntent);
        }
    }
}
