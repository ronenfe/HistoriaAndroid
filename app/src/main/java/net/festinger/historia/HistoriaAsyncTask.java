package net.festinger.historia;

import android.app.Activity;
import android.os.AsyncTask;
import android.os.SystemClock;
import android.view.View;

public  class HistoriaAsyncTask extends AsyncTask<Void, Void, Void> {


	Main main=null;
	CallListArrayAdapter adapter = null;
    HistoriaAsyncTask(Main main) {
    	this.main = main;
    }
    
    @Override
    protected Void doInBackground(Void... unused) {
		return(null);
    }
    @Override
    protected void onPreExecute()
    {
		main.listView.setVisibility(View.GONE);
	    main.findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
		main.getCSV();
		adapter = new CallListArrayAdapter(main,Main.arrayListCalls);
    }
    
    @Override
    protected void onPostExecute(Void unused) {
		main.listView.setAdapter(adapter);
	    main.findViewById(R.id.loadingPanel).setVisibility(View.GONE);
	    main.listView.setVisibility(View.VISIBLE);
    }

  }