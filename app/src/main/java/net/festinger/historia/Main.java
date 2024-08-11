package net.festinger.historia;

import java.io.*;
import java.util.ArrayList;
import java.util.Calendar;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ListActivity;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.CallLog;
import android.util.Log;
import android.view.View;
import android.widget.ListView;

public class Main extends Activity// main screen of historia
{
	public ListView listView = null;
	public static final String PREFS_NAME = "PrefsFile";
	public static SharedPreferences keys; // saved settings keys
	public static long lastCall; // time of most recent call
	public static int numberOfCalls; // limit number of calls to
	public static int dateFormat; // date format
	public static int timeFormat; // time format
	public static boolean isListDirty = true; // time format
	public static boolean flagNewCall = false; // flag for new call happened
	public static ArrayList<CallStrings> arrayListCalls = new ArrayList<CallStrings>(); // stores
																						// all
																						// calls
	private ArrayList<CallStrings> arrayListFilteredByNumberCalls = new ArrayList<CallStrings>(); // stores
																									// calls
																									// for
																									// display
	private ArrayList<CallStrings> arrayListFilteredByTypeCalls = new ArrayList<CallStrings>(); // stores
																								// filtered
																								// by
																								// type
																								// calls
																								// for
																								// display
	public static long lastCallTime; // stores last call time
	private Settings settings; // create a settings window
	private CallDetails callDetails;
	// create a call details window
	private boolean filteredByTypeFlag = false; // flag raised if display is
												// filtered
	private boolean filteredByNumberFlag = false; // flag raised if display is
													// filtered by type
	private int selectedIndex = 0;
	private boolean isFirstTime = true;
	private static String targetFolder = Environment
			.getExternalStorageDirectory().getPath() + "/Historia/"; // where to
																		// save
																		// and
																		// get
																		// the
																		// csv
																		// file
	private static String historiaFileName = targetFolder + "Historia.txt"; // file
																			// name
																			// of
																			// calls

	/** Called when the activity is first created. */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		if (isFirstTime == true) {
			getSettings(getApplicationContext());
			setContentView(R.layout.listview);
			listView = (ListView) findViewById(R.id.list);
			listView.setVisibility(View.GONE);
		    findViewById(R.id.loadingPanel).setVisibility(View.VISIBLE);
			listView.setEmptyView(findViewById(R.id.emptyView));

			if (HistoriaService.isServiceStarted == false) {
				Intent intent = new Intent(this, HistoriaService.class);
				startService(intent);
			}
			isFirstTime = false;
		}
		// if (arrayListCalls.size() == 0)
		// getCSV(); // get calls from csv file
		// if (getCalls(getApplicationContext()) == true) // get new calls from
		// phone
		// saveCSV(); // store new calls in csv file
		// showCallList(); //display list on screen
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
	}

	@Override
	public void onBackPressed() {
		isListDirty = true;
		super.onBackPressed();
	}

	@Override
	public void onWindowFocusChanged(boolean hasFocus) {
		if (hasFocus == true && isListDirty == true) {
			isListDirty = false;
			showCallList();
		}
	}

	public void showCallList() // show call list on screen
	{
		HistoriaAsyncTask historiaAsyncTask = new HistoriaAsyncTask(this);
		historiaAsyncTask.execute();
	}

	public static CallStrings getCalls(Context context) // get calls from call
														// log
	// file
	{

		String[] strFields = { CallLog.Calls.NUMBER, CallLog.Calls.TYPE,
				CallLog.Calls.CACHED_NAME, CallLog.Calls.DATE,
				CallLog.Calls.DURATION, CallLog.Calls.TYPE, };
		String strOrder = android.provider.CallLog.Calls.DATE + " DESC";
		Cursor logs = context.getContentResolver().query(
				CallLog.Calls.CONTENT_URI, strFields, null, null, strOrder); // create
																				// a
																				// phone
																				// log
																				// to
																				// get
																				// calls
																				// from
																				// phone

		int numberIndex = logs.getColumnIndex(CallLog.Calls.NUMBER);
		int nameIndex = logs.getColumnIndex(CallLog.Calls.CACHED_NAME);
		int typeIndex = logs.getColumnIndex(CallLog.Calls.TYPE);
		int dateIndex = logs.getColumnIndex(CallLog.Calls.DATE);
		int durationIndex = logs.getColumnIndex(CallLog.Calls.DURATION);

		int limit = Main.numberOfCalls;
		CallStrings callStrings = null;
		if (logs.moveToNext() == true && lastCallTime < logs.getLong(dateIndex)) {
			callStrings = new CallStrings(logs.getString(numberIndex),
					logs.getString(nameIndex), logs.getLong(dateIndex),
					logs.getString(durationIndex), logs.getInt(typeIndex),
					true,context); // constructor to store data
							// in object; // create an
							// instance of class which
							// stores details of call
			arrayListCalls.add(0, callStrings); // insert new call
			lastCallTime = Long
					.parseLong(((CallStrings) arrayListCalls.get(0)).strings[CallStrings.SN]); // update
																								// last
																								// call
																								// time
			SharedPreferences.Editor editor = Main.keys.edit();
			editor.putLong("LASTCALL", lastCallTime);
			editor.commit();
			if (arrayListCalls.size() > limit) // delete calls from vector //
												// after limit
			{
				arrayListCalls.remove(limit);
			}
		}
		return callStrings;
	};

	public void getCSV() // get calls from csv file
	{
		boolean csvExists = true; // flag to indicate if csv was existing

		try {
			File file = new File(targetFolder);
			if (!file.exists() || file.isDirectory() == false) // if directory
																// does not
																// exists,
																// create it
			{
				file.mkdir();
			}
			file = new File(historiaFileName);
			if (!file.exists() || file.isFile() == false) // if csv file does
															// not exist ,
															// create a new one
			{
				FileOutputStream fo = new FileOutputStream(historiaFileName);
				OutputStreamWriter os = new OutputStreamWriter(fo);
				String line = "Number" + ',' + "Name" + ',' + "Time" + ','
						+ "Duration" + ',' + "Type" + ',' + "Error" + ','
						+ "Location" + ',' + "S/N" + '\r' + '\n';// header line
				os.write(line);
				os.close();
				fo.close();
				csvExists = false; // csv is new
			}

			csvToArrayList(); // get calls to vector
			if (Main.lastCall == 0) // if it's the first time
									// software is running
			{
				if (csvExists == true && arrayListCalls.isEmpty() == false) // if
																			// file
																			// csv
																			// was
																			// existing
																			// (from
																			// previous
																			// versions)
																			// and
																			// its's
																			// not
																			// empty
					lastCallTime = Long.parseLong(((CallStrings) arrayListCalls
							.get(0)).strings[CallStrings.SN]); // make the first
																// call in the
																// file the
																// lastcall so
																// you can
																// update the
																// new calls
																// from call log
				else
					// if csv wasn't existing or is empty
					lastCallTime = 0; // set lastcalltime to early date so file
										// will be updated with all calls from
										// call log
			} else // if it's not the first time the program is running
			{
				if (csvExists == true) // csv file was existing
					lastCallTime = Main.lastCall; // get it from file
				else
					lastCallTime = 0; // set lastcalltime to early date so file
										// will be updated with all calls from
										// call log
			}
		} catch (IOException e) // file exception
		{
			AlertDialog alertDialog = new AlertDialog.Builder(this)
					.setMessage(
							"There was an error getting data from Historia.txt (Do you have an sd card inserted?). you can try to launch the application again. if  it doesn't help, you can delete the file by tapping Yes. The old file will be saved as HistoriaBackup.txt for backup purpuses. If error persists please contact support or reinstall the software. Do you want to delete the file?")
					.setPositiveButton("Yes", dialogClickListener)
					.setNegativeButton("No", dialogClickListener).create();
			alertDialog.show();
		}
	}

	public void csvToArrayList() // get calls from file to vector
	{
		arrayListCalls.clear();
		int limit = Main.numberOfCalls; // limit of calls
		try {
			FileInputStream is = new FileInputStream(historiaFileName);
			DataInputStream in = new DataInputStream(is);
			InputStreamReader isr = new InputStreamReader(in);
			String line = null;
			BufferedReader br = new BufferedReader(isr);
			br.readLine(); // skip header line
			while ((line = br.readLine()) != null
					&& arrayListCalls.size() <= limit) {
				try
				{
				String[] values = new String[8]; // stores values
				values = split(line, "\",\"", this); // split by ,
				CallStrings callStrings = new CallStrings(
						values[CallStrings.NUMBER].substring(1),
						values[CallStrings.NAME], values[CallStrings.TIME],
						values[CallStrings.DURATION], values[CallStrings.TYPE],
						values[CallStrings.LOCATION],
						values[CallStrings.SN].substring(0,
								values[CallStrings.SN].length() - 1)); // stores
																		// in
																		// object
				arrayListCalls.add(callStrings); // add object to vector
				}
				catch (Exception e)
				{
					
				}
			}
			is.close();
		} catch (IOException e) {
			AlertDialog alertDialog = new AlertDialog.Builder(this).setMessage(
					"Error reading CSV File.").create();
			alertDialog.show();
		}
	}

	public static String[] split(String inString, String delimeter,
			Context context) // split strings
	{
		String[] retAr = new String[0]; // array of values
		try {
			ArrayList<String> arrayList = new ArrayList<String>();
			int indexA = 0;
			int indexB = inString.indexOf(delimeter);

			while (indexB != -1) {
				if (indexB > indexA)
					arrayList
							.add(new String(inString.substring(indexA, indexB)));
				indexA = indexB + delimeter.length();
				indexB = inString.indexOf(delimeter, indexA);
			}
			arrayList.add(new String(inString.substring(indexA,
					inString.length())));
			retAr = new String[arrayList.size()];
			for (int i = 0; i < arrayList.size(); i++) {
				retAr[i] = arrayList.get(i).toString();
			}
		} catch (Exception e) {
			AlertDialog alertDialog = new AlertDialog.Builder(context)
					.setMessage("Error proccesing CSV File.").create();
			alertDialog.show();
		}
		return retAr;
	}

	public static void saveCSV(Context context) // save to csv file
	{
		File fc;
		try {
			fc = new File(historiaFileName);
			fc.delete();
			FileOutputStream fo = new FileOutputStream(historiaFileName);
			OutputStreamWriter os = new OutputStreamWriter(fo);
			String line = ("Number" + ',' + "Name" + ',' + "Time" + ','
					+ "Duration" + ',' + "Type" + ',' + "Error" + ',' + "Location" + ',' + "S/N"
					+ '\r' + '\n');// header line
			BufferedWriter br = new BufferedWriter(os);
			br.write(line);

			for (int i = 0; i < arrayListCalls.size(); i++) {
				line = ((CallStrings) arrayListCalls.get(i)).toString(); // get
																			// string
																			// of
																			// data
																			// from
																			// vector
				br.write(line);
			}
			br.close();
			os.close();
		} catch (IOException e) {
		}
	}

	// private class ListCallBack implements ListFieldCallback // drawing call
	// list
	// {
	// public void drawListRow(ListField list, Graphics g, int index, int y, int
	// w) // draw row
	// {
	// String stringNumber;
	// String stringName;
	// String stringTime;
	// String stringDuration;
	// String stringType;
	// if (filteredByNumberFlag == true) // if display of calls filtered by type
	// {
	// stringNumber = ((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(index)).strings[CallStrings.NUMBER];
	// stringName = ((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(index)).strings[CallStrings.NAME];
	// stringTime = ((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(index)).strings[CallStrings.TIME];
	// stringDuration = ((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(index)).strings[CallStrings.DURATION];
	// stringType = ((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(index)).strings[CallStrings.TYPE];
	// }
	// else if (filteredByTypeFlag == true) // if display of calls filtered
	// {
	// stringNumber = ((CallStrings)
	// vectorFilteredByTypeCalls.elementAt(index)).strings[CallStrings.NUMBER];
	// stringName = ((CallStrings)
	// vectorFilteredByTypeCalls.elementAt(index)).strings[CallStrings.NAME];
	// stringTime = ((CallStrings)
	// vectorFilteredByTypeCalls.elementAt(index)).strings[CallStrings.TIME];
	// stringDuration = ((CallStrings)
	// vectorFilteredByTypeCalls.elementAt(index)).strings[CallStrings.DURATION];
	// stringType = ((CallStrings)
	// vectorFilteredByTypeCalls.elementAt(index)).strings[CallStrings.TYPE];
	// }
	// else // if display of calls not filtered
	// {
	// stringNumber = ((CallStrings)
	// vectorCalls.elementAt(index)).strings[CallStrings.NUMBER];
	// stringName = ((CallStrings)
	// vectorCalls.elementAt(index)).strings[CallStrings.NAME];
	// stringTime = ((CallStrings)
	// vectorCalls.elementAt(index)).strings[CallStrings.TIME];
	// stringDuration = ((CallStrings)
	// vectorCalls.elementAt(index)).strings[CallStrings.DURATION];
	// stringType = ((CallStrings)
	// vectorCalls.elementAt(index)).strings[CallStrings.TYPE];
	// }
	// stringTime = new StringBuffer(stringTime).insert(stringTime.indexOf(' '),
	// ",").toString(); // sets size to 16 chars
	// int pos = 10; // starting position of strings
	// int height = g.getFont().getHeight();
	// int width = Display.getWidth();
	// //---- add icon---------
	// if( stringType.compareTo("Incoming") == 0)
	// {
	// g.drawBitmap(pos, y + height*40/100, 30 , 30,incomingBitmap, 0, 0 );
	// }
	// else if (stringType.compareTo("Outgoing") == 0)
	// {
	// g.drawBitmap(pos, y+ height*40/100, 30,30,outgoingBitmap, 0, 0 );
	// }
	// else
	// g.drawBitmap(pos, y + height*40/100, 30,30,missedBitmap, 0, 0 );
	//
	// pos += 20 ;
	// g.drawText(stringNumber, pos , y + height*25/100, (DrawStyle.LEFT +
	// DrawStyle.ELLIPSIS + DrawStyle.TOP),(width - pos) *41/100 );
	// g.drawText(stringName, pos , y + height*150/100, (DrawStyle.LEFT +
	// DrawStyle.ELLIPSIS + DrawStyle.TOP),(width - pos) *41/100 );
	// g.drawText(stringTime, pos + (width - pos) *41/100 + 5 , y +
	// height*25/100, (DrawStyle.RIGHT + DrawStyle.ELLIPSIS + DrawStyle.TOP),
	// (width - pos) *59/100 - 10);
	// g.drawText(stringDuration, pos + (width - pos) *41/100 +5 , y +
	// height*150/100, (DrawStyle.RIGHT + DrawStyle.ELLIPSIS + DrawStyle.TOP),
	// (width - pos) *59/100 - 10);
	// g.drawLine(0 , y + height*275/100 - 1 ,Display.getWidth() , y +
	// height*275/100 - 1); // draw line under row
	//
	//
	// }
	// public Object get(ListField list, int index)
	// {
	// return vectorFilteredByTypeCalls.elementAt(index);
	// }
	// public int indexOfList(ListField list, String p, int s)
	// {
	// return vectorFilteredByTypeCalls.indexOf(p, s);
	// }
	// public int getPreferredWidth(ListField list)
	// {
	// return Display.getWidth();
	// }
	// }

	// ---making a menu
	// protected void makeMenu(Menu menu, int instance) // make application menu
	// {
	// super.makeMenu(menu, instance);
	// if (instance == Menu.INSTANCE_DEFAULT && filteredByNumberFlag == false)
	// // if not filtered by number
	// {
	// menu.add(menuIncoming);
	// menu.add(menuOutgoing);
	// menu.add(menuMissed);
	// menu.add(menuSummary);
	// if (callList.getSize() != 0) // if calls exist in list
	// menu.add(menuDeleteAll);
	// menu.add(menuSettings);
	// menu.add(menuAbout);
	// }
	// else if (instance == Menu.INSTANCE_DEFAULT && filteredByNumberFlag ==
	// true) // if filtered by number
	// {
	// menu.add(menuBack);
	// menu.add(menuSummary);
	// if (callList.getSize() != 0) // if calls exist in list
	// menu.add(menuDeleteAll);
	// menu.add(menuSettings);
	// menu.add(menuAbout);
	// }
	// else if (filteredByNumberFlag == false && callList.getSize() != 0) //
	// context menu if not filtered by number and list is not empty
	// {
	// selectedIndex = callList.getSelectedIndex();
	// menu.add(menuDetails);
	// menu.add(menuCall);
	// menu.add(menuSMS);
	// menu.add(menuContacts);
	// menu.add(menuFilter);
	// menu.add(menuCopy);
	// menu.add(menuDelete);
	// }
	// else if (filteredByNumberFlag == true && callList.getSize() != 0) //
	// context menu if filtered by number and list is not empty
	// {
	// selectedIndex = callList.getSelectedIndex();
	// menu.add(menuDetails);
	// menu.add(menuCall);
	// menu.add(menuSMS);
	// menu.add(menuContacts);
	// menu.add(menuCopy);
	// menu.add(menuDelete);
	// }
	//
	// }
	public void filterByType(String type) {
		// callList.setSize(0);
		filteredByTypeFlag = true;
		arrayListFilteredByTypeCalls.clear();
		// titleLabel.setText(" " + type);
		for (int i = 0; i < arrayListCalls.size(); i++) // get all Outgoing and
														// insert to list
		{
			if (((CallStrings) arrayListCalls.get(i)).strings[CallStrings.TYPE]
					.equals(type)) {
				arrayListFilteredByTypeCalls.add(arrayListCalls.get(i));
			}
		}

	}

	public void filterByNumber(String number) {
		filteredByNumberFlag = true;
		// callList.setSize(0);
		arrayListFilteredByNumberCalls.clear();
		for (int i = 0; i < arrayListCalls.size(); i++) // get all calls from
														// same number and
														// insert to list
		{
			if (((CallStrings) arrayListCalls.get(i)).strings[CallStrings.NUMBER]
					.equals(number)) {
				arrayListFilteredByNumberCalls.add(arrayListCalls.get(i));
			}
		}
		// titleLabel.setText(" " + number);
	}

	// ---filter by incoming
	// private MenuItem menuIncoming = new MenuItem(Characters.BALLOT_BOX +
	// " Incoming", 400000, 2000) // open settings window
	// {
	// public void run()
	// {
	// if (menuIncoming.toString().equals(Characters.BALLOT_BOX + " Incoming"))
	// {
	// menuIncoming.setText(Characters.BALLOT_BOX_WITH_CHECK + " Incoming");
	// menuOutgoing.setText(Characters.BALLOT_BOX + " Outgoing");
	// menuMissed.setText(Characters.BALLOT_BOX + " Missed");
	// filterByType("Incoming");
	// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// else
	// {
	// callList.setSize(0);
	// menuIncoming.setText(Characters.BALLOT_BOX + " Incoming");
	// filteredByTypeFlag = false;
	// for (int i=0; i < vectorCalls.size(); i++) // insert elements to the call
	// row
	// {
	// callList.insert(i);
	// }
	// titleLabel.setText(" Historia");
	// }
	//
	// }
	// };
	// //---filter by outgoing
	// private MenuItem menuOutgoing = new MenuItem(Characters.BALLOT_BOX +
	// " Outgoing", 400001, 2000) // open settings window
	// {
	// public void run()
	// {
	// if (menuOutgoing.toString().equals(Characters.BALLOT_BOX + " Outgoing"))
	// {
	// menuOutgoing.setText(Characters.BALLOT_BOX_WITH_CHECK + " Outgoing");
	// menuIncoming.setText(Characters.BALLOT_BOX + " Incoming");
	// menuMissed.setText(Characters.BALLOT_BOX + " Missed");
	// filterByType("Outgoing");
	// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// else
	// {
	// callList.setSize(0);
	// menuOutgoing.setText(Characters.BALLOT_BOX + " Outgoing");
	// filteredByTypeFlag = false;
	// for (int i=0; i < vectorCalls.size(); i++) // insert elements to the call
	// row
	// {
	// callList.insert(i);
	// }
	// titleLabel.setText(" Historia");
	// }
	// }
	// };
	// //---filter by missed
	// private MenuItem menuMissed = new MenuItem(Characters.BALLOT_BOX +
	// " Missed", 400002, 2000) // open settings window
	// {
	// public void run()
	// {
	// if (menuMissed.toString().equals(Characters.BALLOT_BOX + " Missed"))
	// {
	// menuMissed.setText(Characters.BALLOT_BOX_WITH_CHECK + " Missed");
	// menuIncoming.setText(Characters.BALLOT_BOX + " Incoming");
	// menuOutgoing.setText(Characters.BALLOT_BOX + " Outgoing");
	// filterByType("Missed");
	// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// else
	// {
	// callList.setSize(0);
	// menuMissed.setText(Characters.BALLOT_BOX + " Missed");
	// filteredByTypeFlag = false;
	// for (int i=0; i < vectorCalls.size(); i++) // insert elements to the call
	// row
	// {
	// callList.insert(i);
	// }
	// titleLabel.setText(" Historia");
	// }
	//
	// }
	// };
	// private MenuItem menuSettings = new MenuItem("Settings", 800000, 2000) //
	// open settings window
	// {
	// public void run()
	// {
	// settings = new Settings(Main.this, vectorCalls.size()); // create the
	// settings window
	// UiApplication.getUiApplication().pushScreen(settings);
	// }
	// };
	// private MenuItem menuAbout = new MenuItem("About", 900000, 2000) // open
	// settings window
	// {
	// public void run()
	// {
	// String owner;
	//
	// owner = Integer.toString(DeviceInfo.getDeviceId());
	// if (Historia.flagRegistered == false)
	// Dialog.inform("      Historia, Version 1.\n-----------------------------------\nUNREGISTERED\n"
	// +
	// "This Program is copyrighted \u00A92008.\nAll rights reserved to SBSH.\n");
	// else
	// Dialog.inform("      Historia, Version 1.\n-----------------------------------\nRegistered to: "
	// + owner + "\nKey: " + (String)Historia.keys.elementAt(Historia.KEYNUMBER)
	// +
	// "\n"This Program is copyrighted \u00A92008.\nAll rights reserved to SBSH.\n");
	// }
	// };
	// private MenuItem menuDetails = new MenuItem("Details", 39990, 2000) //
	// Show call details
	// {
	// public void run()
	// {
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false) // if
	// not filtered
	// callDetails = new
	// CallDetails((CallStrings)vectorCalls.elementAt(selectedIndex)); // create
	// the details window
	// else if (filteredByTypeFlag == true ) // if filtered by type
	// callDetails = new
	// CallDetails((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex));
	// // create the details window
	// else // if filtered by number
	// callDetails = new
	// CallDetails((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex));
	// // create the details window
	//
	// UiApplication.getUiApplication().pushScreen(callDetails);
	// }
	// };
	// private MenuItem menuBack = new MenuItem("Back", 400000, 2000) // back
	// from filtered number
	// {
	// public void run()
	// {
	// filteredByNumberFlag = false;
	// if (filteredByTypeFlag == true) // if display was filtered by type
	// {
	// callList.setSize(0);
	// vectorFilteredByTypeCalls.removeAllElements();
	// String type;
	//
	// if (menuMissed.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
	// " Missed"))
	// type = "Missed";
	// else if (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK
	// + " Outgoing"))
	// type = "Outgoing";
	// else
	// type = "Incoming";
	// titleLabel.setText(" " + type);
	// filterByType(type);
	// callList.setSize(0);
	// for (int i = 0; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// else
	// {
	// callList.setSize(0);
	// filteredByTypeFlag = false;
	// for (int i=0; i < vectorCalls.size(); i++) // insert elements to the call
	// row
	// {
	// callList.insert(i);
	// }
	// }
	// }
	// };
	//
	// private MenuItem menuCall = new MenuItem("Call", 400010, 2000) // call
	// number
	// {
	// public void run()
	// {
	// String number;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false) // if
	// not filtered
	// number =
	// ((CallStrings)vectorCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else if (filteredByTypeFlag == true ) // if filtered by type
	// number =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else // if filtered by number
	// number =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// PhoneArguments call = new PhoneArguments
	// (PhoneArguments.ARG_CALL,number);
	// Invoke.invokeApplication(Invoke.APP_TYPE_PHONE, call);
	//
	//
	// }
	// };
	// private MenuItem menuSMS = new MenuItem("SMS", 400020, 2000) // send sms
	// {
	// public void run()
	// {
	// try
	// {
	// String number;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false) // if
	// not filtered
	// number =
	// ((CallStrings)vectorCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else if (filteredByTypeFlag == true ) // if filtered by type
	// number =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else // if filtered by number
	// number =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	//
	// MessageConnection mc = (MessageConnection)Connector.open("sms://");
	// mc.close();
	// TextMessage textMessage =
	// (TextMessage)mc.newMessage(MessageConnection.TEXT_MESSAGE);
	// textMessage.setAddress("sms://" + number);
	// Invoke.invokeApplication(Invoke.APP_TYPE_MESSAGES, new
	// MessageArguments(textMessage));
	// }
	// catch (Throwable e)
	// {
	// Dialog.alert(e.getMessage());
	// }
	//
	//
	// }
	// };
	// private MenuItem menuContacts = new MenuItem("Save to Contacts", 400030,
	// 2000) // save to contacts
	// {
	// public void run()
	// {
	// try
	// {
	// String number;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false) // if
	// not filtered
	// number =
	// ((CallStrings)vectorCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else if (filteredByTypeFlag == true ) // if filtered by type
	// number =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else // if filtered by number
	// number =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	//
	// ContactList deviceAdrBook = null;
	// deviceAdrBook = (ContactList) PIM.getInstance().openPIMList(
	// PIM.CONTACT_LIST, PIM.READ_WRITE);
	// Contact newBBContact = deviceAdrBook.createContact();
	//
	//
	// newBBContact.addString(Contact.TEL, Contact.ATTR_MOBILE, number);
	// newBBContact.commit();
	//
	// Invoke.invokeApplication(Invoke.APP_TYPE_ADDRESSBOOK,new
	// AddressBookArguments(AddressBookArguments.ARG_NEW, newBBContact));
	// }
	// catch(Exception e)
	// {
	// Dialog.alert(e.getMessage());
	// }
	//
	// }
	// };
	// private MenuItem menuCopy = new MenuItem("Copy", 400040, 2000) // Copy
	// number to clipboard
	// {
	// public void run()
	// {
	// String number;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false) // if
	// not filtered
	// number =
	// ((CallStrings)vectorCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else if (filteredByTypeFlag == true ) // if filtered by type
	// number =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else // if filtered by number
	// number =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// Clipboard.getClipboard().put(number);
	// }
	// };
	// private MenuItem menuFilter = new MenuItem("Filter", 400050, 2000) //
	// filter by number
	// {
	// public void run()
	// {
	// String number;
	// if (filteredByTypeFlag == true) // if filtered by type
	// number =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// else // if not filtered
	// number =
	// ((CallStrings)vectorCalls.elementAt(selectedIndex)).strings[CallStrings.NUMBER];
	// filterByNumber(number);
	// for (int i = 0 ; i < vectorFilteredByNumberCalls.size(); i++)
	// callList.insert(i);
	// }
	// };
	// private MenuItem menuDelete = new MenuItem("Delete", 400060, 2000) //
	// delete a number
	// {
	// public void run()
	// {
	// int index;
	// String sn;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false ) // if
	// in non filtered display
	// {
	// callList.setSize(0);
	// vectorCalls.removeElementAt(selectedIndex);
	// saveCSV();
	// for (int i =0 ; i < vectorCalls.size(); i++)
	// callList.insert(i);
	// }
	// else if (filteredByNumberFlag == true ) // if in filtered by number
	// {
	// sn =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(selectedIndex)).strings[CallStrings.SN];
	// callList.setSize(0);
	// vectorFilteredByNumberCalls.removeElementAt(selectedIndex);
	// index = binarySearch(vectorCalls,sn); // remove from vectorCalls
	// if (index!= -1)
	// {
	// vectorCalls.removeElementAt(index);
	// saveCSV();
	// }
	// if (filteredByTypeFlag == true)
	// {
	// index = binarySearch(vectorFilteredByTypeCalls,sn); // remove from
	// filtered by type vector
	// if (index!= -1)
	// vectorFilteredByTypeCalls.removeElementAt(index);
	// }
	// }
	// else if (filteredByNumberFlag == false && filteredByTypeFlag == true) //
	// if in filtered by type display
	// {
	// sn =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(selectedIndex)).strings[CallStrings.SN];
	// callList.setSize(0);
	// vectorFilteredByTypeCalls.removeElementAt(selectedIndex);
	//
	// index = binarySearch(vectorCalls,sn); // remove from vectorCalls
	// if (index!= -1)
	// {
	// vectorCalls.removeElementAt(index);
	// saveCSV();
	// }
	// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// }
	// };
	// private MenuItem menuDeleteAll = new MenuItem("Delete All", 700000, 2000)
	// // delete all numbers
	// {
	// public void run()
	// {
	// int index;
	// String sn;
	// if
	// (Dialog.ask(Dialog.D_YES_NO,"All calls shown in current display will be deleted, are you sure?",Dialog.NO)
	// == Dialog.YES) // warn the user
	// {
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false ) // if
	// in non filtered display
	// {
	// callList.setSize(0);
	// vectorCalls.removeAllElements();
	// saveCSV();
	// }
	// else if (filteredByNumberFlag == true ) // if in filtered by number
	// display
	// {
	// for (int i =0 ; i < vectorFilteredByNumberCalls.size(); i++)
	// {
	// sn =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(i)).strings[CallStrings.SN];
	// index = binarySearch(vectorCalls,sn); // remove from vectorCalls
	// if (index != -1)
	// {
	// vectorCalls.removeElementAt(index);
	// }
	// if (filteredByTypeFlag == true) // if display was also difiltered by type
	// {
	// index = binarySearch(vectorFilteredByTypeCalls,sn);
	// if (index != -1)
	// {
	// vectorFilteredByTypeCalls.removeElementAt(index);
	// }
	// }
	// }
	// saveCSV();
	// callList.setSize(0);
	// vectorFilteredByNumberCalls.removeAllElements();
	// }
	// else if (filteredByNumberFlag == false && filteredByTypeFlag == true) //
	// if in filtered by type display
	// {
	// for (int i =0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// {
	// sn =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(i)).strings[CallStrings.SN];
	// index = binarySearch(vectorCalls,sn); // remove from vectorCalls
	// if (index != -1)
	// {
	// vectorCalls.removeElementAt(index);
	// }
	//
	// }
	// callList.setSize(0);
	// saveCSV();
	// vectorFilteredByTypeCalls.removeAllElements();
	// }
	// }
	// }
	// };
	// private MenuItem menuSummary = new MenuItem("Summary", 600000, 2000) //
	// delete all numbers
	// {
	// public void run()
	// {
	// String duration; // current row duration
	// String[] durationSeparated; // total duration separated strings
	// int total = 0; // total duration
	// int i = 0; //counter
	// int hours;
	// int minutes;
	// if (filteredByNumberFlag == false && filteredByTypeFlag == false ) // if
	// in non filtered display
	// {
	// for (; i < vectorCalls.size(); i++)
	// {
	// duration =
	// ((CallStrings)vectorCalls.elementAt(i)).strings[CallStrings.DURATION];
	// durationSeparated = split(duration,":");
	// if ( durationSeparated.length == 3)
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 3600 +
	// Integer.parseInt(durationSeparated[1]) * 60 +
	// Integer.parseInt(durationSeparated[2]);
	// }
	// else
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 60 +
	// Integer.parseInt(durationSeparated[1]);
	// }
	//
	// }
	//
	// }
	// else if (filteredByNumberFlag == true ) // if in filtered by number
	// display
	// {
	// for (; i < vectorFilteredByNumberCalls.size(); i++)
	// {
	// duration =
	// ((CallStrings)vectorFilteredByNumberCalls.elementAt(i)).strings[CallStrings.DURATION];
	// durationSeparated = split(duration,":");
	// if ( durationSeparated.length == 3)
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 3600 +
	// Integer.parseInt(durationSeparated[1]) * 60 +
	// Integer.parseInt(durationSeparated[2]);
	// }
	// else
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 60 +
	// Integer.parseInt(durationSeparated[1]);
	// }
	//
	// }
	// }
	// else if (filteredByNumberFlag == false && filteredByTypeFlag == true) //
	// if in filtered by type display
	// {
	// for (; i < vectorFilteredByTypeCalls.size(); i++)
	// {
	// duration =
	// ((CallStrings)vectorFilteredByTypeCalls.elementAt(i)).strings[CallStrings.DURATION];
	// durationSeparated = split(duration,":");
	// if ( durationSeparated.length == 3)
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 3600 +
	// Integer.parseInt(durationSeparated[1]) * 60 +
	// Integer.parseInt(durationSeparated[2]);
	// }
	// else
	// {
	// total+= Integer.parseInt(durationSeparated[0]) * 60 +
	// Integer.parseInt(durationSeparated[1]);
	// }
	//
	// }
	// }
	// //---- set duration, converting seconds to 0:00:00 format----
	// duration = "";
	// hours = total/3600;
	// total %= 3600;
	// duration += hours == 0 ? "" : (Integer.toString(hours) +":");
	// minutes = total/60;
	// duration += (Integer.toString(minutes).length() == 2 ?
	// Integer.toString(minutes) : ("0" + Integer.toString(minutes))) + ":";
	// total %= 60;
	// duration += Integer.toString(total).length() == 2 ?
	// Integer.toString(total) : ("0" + Integer.toString(total));
	// //------------------------
	// Dialog.inform("Number of Calls: " + i + "\n" +"Total Duration: " +
	// duration);
	// }
	// };
	//
	// public void notifyScreenClosed(MainScreen screen) // when settings is
	// closed invalidate display
	// {
	// // check the event source object
	// if (screen.equals(settings))
	// {
	// int callsLimit =
	// Integer.parseInt((String)Historia.keys.elementAt(Historia.NUMBEROFCALLS));
	// // get call limit from file
	// if (callsLimit < vectorCalls.size()) // if call limit is less then
	// current number of calls
	// {
	// callList.setSize(0);
	// while (callsLimit < vectorCalls.size()) // loop and delete all lines
	// after the call limit location
	// vectorCalls.removeElementAt(callsLimit);
	// saveCSV(); // store new calls in csv file; // update the csv file
	//
	// if (filteredByTypeFlag == false && filteredByNumberFlag == false) // if
	// display is not filtered
	// {
	// for (int i = 0 ; i < vectorCalls.size(); i++)
	// callList.insert(i);
	// }
	// else if (filteredByTypeFlag == true && filteredByNumberFlag == false ) //
	// if table is filtered by type
	// {
	// if (menuMissed.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
	// " Missed")) // if filtered to missed
	// {
	// filterByType("Missed"); //filter again
	// }
	// else if (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK
	// + " Outgoing")) // if filtered to Outgoing
	// {
	// filterByType("Outgoing"); //filter again
	// }
	// else // if filtered to incoming
	// {
	// filterByType("Incoming"); //filter again
	// }
	// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
	// callList.insert(i);
	// }
	// else if (filteredByNumberFlag == true && filteredByTypeFlag == false) //
	// if filtered by number
	// {
	// filterByNumber(((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(0)).strings[CallStrings.NUMBER]);
	// // filter by number again
	// for (int i = 0 ; i < vectorFilteredByNumberCalls.size(); i++)
	// callList.insert(i);
	//
	// }
	// else if (filteredByNumberFlag == true && filteredByTypeFlag == true) //
	// if filtered by number after filtered by type
	// {
	// if (menuMissed.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
	// " Missed")) // if filtered to missed
	// filterByType("Missed"); //filter again
	// else if (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK
	// + " Outgoing")) // if filtered to Outgoing
	// filterByType("Outgoing"); //filter again
	// else // if filtered to incoming
	// filterByType("Incoming"); //filter again
	//
	// filterByNumber(((CallStrings)
	// vectorFilteredByNumberCalls.elementAt(0)).strings[CallStrings.NUMBER]);
	// // filter by number again
	// for (int i = 0 ; i < vectorFilteredByNumberCalls.size(); i++)
	// callList.insert(i);
	// }
	// }
	// }
	// };
	public int binarySearch(ArrayList<CallStrings> calls, String sn) {
		int min = 0;
		int max = calls.size() - 1;
		int mid;

		while (min <= max) {
			mid = (min + max) / 2;
			if ((((CallStrings) calls.get(mid)).strings[CallStrings.SN])
					.compareTo(sn) > 0)
				min = mid + 1;
			else if ((((CallStrings) calls.get(mid)).strings[CallStrings.SN])
					.compareTo(sn) < 0)
				max = mid - 1;
			else
				return mid;
		}
		return -1;
	};

	protected void onExposed() // listen to closed event
	{

		// PhoneLogs logs = PhoneLogs.getInstance(); // create a phone log to
		// get calls from phone
		// int indexMissed = logs.numberOfCalls(PhoneLogs.FOLDER_MISSED_CALLS) -
		// 1; // store current index of missed call
		// int indexNormal = logs.numberOfCalls(PhoneLogs.FOLDER_NORMAL_CALLS) -
		// 1; // stores current index of normal call
		// PhoneCallLog normalCall = (PhoneCallLog) (indexNormal >= 0 ?
		// logs.callAt(indexNormal, PhoneLogs.FOLDER_NORMAL_CALLS) : null) ;
		// PhoneCallLog missedCall = (PhoneCallLog) (indexMissed >= 0 ?
		// logs.callAt(indexMissed, PhoneLogs.FOLDER_MISSED_CALLS) : null) ;
		//
		// if (flagNewCall == true || (normalCall != null && lastCallTime <
		// normalCall.getDate().getTime() )||(missedCall != null && lastCallTime
		// < missedCall.getDate().getTime())) // if new calls exist
		// {
		// callList.setSize(0);
		// if (flagNewCall == false)
		// {
		// getCalls(); // get new calls from phone
		// saveCSV(); // store new calls in csv file
		// }
		// else
		// {
		// getCSV();
		// }
		// if (filteredByTypeFlag == false && filteredByNumberFlag == false) //
		// if display is not filtered
		// for (int i = 0 ; i < vectorCalls.size(); i++)
		// callList.insert(i);
		// else if (filteredByTypeFlag == true && filteredByNumberFlag == false
		// ) // if table is filtered by type
		// {
		// if (menuMissed.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Missed")) // if filtered to missed
		// filterByType("Missed"); //filter again
		// else if
		// (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Outgoing")) // if filtered to Outgoing
		// filterByType("Outgoing"); //filter again
		// else // if filtered to incoming
		// filterByType("Incoming"); //filter again
		// for (int i = 0 ; i < vectorFilteredByTypeCalls.size(); i++)
		// callList.insert(i);
		// }
		// else if (filteredByNumberFlag == true && filteredByTypeFlag == false
		// && vectorFilteredByNumberCalls.size() != 0) // if filtered by number
		// {
		// filterByNumber(((CallStrings)
		// vectorFilteredByNumberCalls.elementAt(0)).strings[CallStrings.NUMBER]);
		// // filter by number again
		// for (int i = 0 ; i < vectorFilteredByNumberCalls.size(); i++)
		// callList.insert(i);
		// }
		// else if (filteredByNumberFlag == true && filteredByTypeFlag == true)
		// // if filtered by number after filtered by type
		// {
		// if(menuMissed.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Missed")) // if filtered to missed
		// filterByType("Missed"); //filter again
		// else if
		// (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Outgoing")) // if filtered to Outgoing
		// filterByType("Outgoing"); //filter again
		// else // if filtered to incoming
		// filterByType("Incoming"); //filter again
		// if (vectorFilteredByNumberCalls.size() != 0 )
		// {
		// filterByNumber(((CallStrings)
		// vectorFilteredByNumberCalls.elementAt(0)).strings[CallStrings.NUMBER]);
		// // filter by number again
		// for (int i = 0 ; i < vectorFilteredByNumberCalls.size(); i++)
		// callList.insert(i);
		// }
		// }
		// flagNewCall = false;
		// }
	}

	public boolean onClose() // restore previous values
	{
		// if (filteredByNumberFlag == true) // if filtered by number , go to
		// previous screen
		// {
		// menuBack.run();
		// }
		// else if (filteredByTypeFlag == true ) // if filtered by type go to
		// main screen
		// {
		// callList.setSize(0);
		// if (menuIncoming.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Incoming"))
		// {
		// menuIncoming.setText(Characters.BALLOT_BOX + " Incoming");
		// }
		// else if
		// (menuOutgoing.toString().equals(Characters.BALLOT_BOX_WITH_CHECK +
		// " Outgoing"))
		// {
		// menuOutgoing.setText(Characters.BALLOT_BOX + " Outgoing");
		// }
		// else
		// {
		// menuMissed.setText(Characters.BALLOT_BOX + " Missed");
		// }
		// filteredByTypeFlag = false;
		// for (int i=0; i < vectorCalls.size(); i++) // insert elements to the
		// call row
		// {
		// callList.insert(i);
		// }
		// titleLabel.setText(" Historia");
		// }
		// else
		// super.onClose();
		return true;
	}

	public static boolean copyFile(File source, File dest) {
		BufferedInputStream bis = null;
		BufferedOutputStream bos = null;

		try {
			bis = new BufferedInputStream(new FileInputStream(source));
			bos = new BufferedOutputStream(new FileOutputStream(dest, false));

			byte[] buf = new byte[1024];
			bis.read(buf);

			do {
				bos.write(buf);
			} while (bis.read(buf) != -1);
		} catch (IOException e) {
			return false;
		} finally {
			try {
				if (bis != null)
					bis.close();
				if (bos != null)
					bos.close();
			} catch (IOException e) {
				return false;
			}
		}

		return true;
	}

	DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
			switch (which) {
			case DialogInterface.BUTTON_POSITIVE:
				// Yes button clicked
				String historiaFileNameBackup = targetFolder
						+ "HistoriaBackup.txt"; // file name of backup
				File fcException;
				try // create backup
				{
					fcException = new File(historiaFileName);
					if (fcException.exists() == true) {
						copyFile(fcException, new File(historiaFileNameBackup));
						fcException.delete(); // delete historia.txt
					}
				} catch (Exception e1) {
					AlertDialog alertDialog = new AlertDialog.Builder(Main.this)
							.setMessage("Error creating backup file.").create();
					alertDialog.show();
				}
				break;

			case DialogInterface.BUTTON_NEGATIVE:
				// No button clicked
				break;
			}
		}
	};

	public static void addCalendarEvent(CallStrings callStrings, Context context) {
		try {
//			Log.d("calendar", "calendar");
			String addString;
			// check if call should be added:
			// if
			// (((String)Historia.keys.elementAt(Historia.CALLTYPETOLOG)).compareTo("Custom")
			// == 0 )
			// {
			// if(((String)Historia.keys.elementAt(Historia.LOGOUTGOING)).compareTo("0")
			// == 0 &&
			// callStrings.strings[CallStrings.TYPE].compareTo("Outgoing") == 0
			// && callStrings.strings[CallStrings.DURATION].compareTo("00:00")
			// != 0)
			// return;
			// if(((String)Historia.keys.elementAt(Historia.LOGINCOMING)).compareTo("0")
			// == 0 &&
			// callStrings.strings[CallStrings.TYPE].compareTo("Incoming") == 0)
			// return;
			// if(((String)Historia.keys.elementAt(Historia.LOGMISSED)).compareTo("0")
			// == 0 &&
			// callStrings.strings[CallStrings.TYPE].compareTo("Missed") == 0)
			// return;
			// if(((String)Historia.keys.elementAt(Historia.LOGOUTGOING)).compareTo("0")
			// == 0 &&
			// callStrings.strings[CallStrings.TYPE].compareTo("Outgoing") == 0
			// && callStrings.strings[CallStrings.DURATION].compareTo("00:00")
			// == 0)
			// return;
			//
			// }
			// if
			// (((String)Historia.keys.elementAt(Historia.NUMBERSTOLOG)).compareTo("Custom")
			// == 0 )
			// {
			// numbersToVector();
			// int index =
			// NumbersListScreen.binarySearchNumber(Historia.vectorNumbers,
			// callStrings.strings[CallStrings.NUMBER]);
			// if (Historia.vectorNumbers.size() <= index ||
			// ((String)(Historia.vectorNumbers.elementAt(index))).compareTo(
			// callStrings.strings[CallStrings.NUMBER]) != 0)
			// {
			// return;
			// }
			//
			// }
			String _id = "-1";
			ContentResolver contentResolver = context.getContentResolver();
			final Cursor cursor = contentResolver.query(
					Uri.parse("content://com.android.calendar/calendars"), (new String[] {
							"_id", "displayName", "selected" }), null, null,
					null);

			while (cursor.moveToNext()) {

				_id = cursor.getString(0);
				final String displayName = cursor.getString(1);
				final Boolean selected = !cursor.getString(2).equals("0");
				if (selected)
					break;
			}
			if (_id.compareTo("-1") == 0)
				return;
			String eventUriString = "content://com.android.calendar/events";
			ContentValues eventValues = new ContentValues();
			eventValues.put("calendar_id", Integer.parseInt(_id)); // id, We need to choose from
			// our mobile for primary
			// its 1

			addString = callStrings.strings[CallStrings.TYPE] + " call ";

			if (callStrings.strings[CallStrings.TYPE].compareTo("Outgoing") == 0
					&& callStrings.strings[CallStrings.DURATION]
							.compareTo("00:00") == 0)
				addString = "Unanswered" + " call ";
			addString += callStrings.strings[CallStrings.TYPE]
					.compareTo("Outgoing") == 0 ? " to: " : " from: ";
			if (callStrings.strings[CallStrings.NAME].compareTo("Unknown") != 0)
				addString += callStrings.strings[CallStrings.NAME] + ", ";
			addString += callStrings.strings[CallStrings.NUMBER];

			eventValues.put("title", addString);

			addString = "Duration: " + callStrings.strings[CallStrings.NUMBER];
			if (callStrings.strings[CallStrings.LOCATION]
					.compareTo("Not Available") != 0)
				eventValues.put("eventLocation",
						callStrings.strings[CallStrings.LOCATION]);
			eventValues.put("description", "Duration: "
					+ callStrings.strings[CallStrings.DURATION]);
			eventValues.put("dtstart",
					Long.parseLong(callStrings.strings[CallStrings.SN]));
			eventValues.put("dtend",
					Long.parseLong(callStrings.strings[CallStrings.SN]));
			context.getContentResolver().insert(Uri.parse(eventUriString),
					eventValues);

		} catch (Exception e) {
			// Handle Exception
		}
	}

	public static void getSettings(Context context) {
		if (keys == null) {
			keys = context.getSharedPreferences(PREFS_NAME, 0); // get settings
			if (keys.contains("LAST_CALL")) // if settings exists
			{
				lastCall = keys.getLong("LAST_CALL", 0);
				numberOfCalls = keys.getInt("NUMBER_OF_CALLS", 0);
				dateFormat = keys.getInt("DATE_FORMAT", 0);
				dateFormat = keys.getInt("Time_FORMAT", 0);
			} else {
				SharedPreferences.Editor editor = keys.edit();
				editor.putLong("LAST_CALL", 0);
				editor.putInt("NUMBER_OF_CALLS", 1000);
				editor.putInt("DATE_FORMAT", 0);
				editor.putInt("TIME_FORMAT", 0);
				editor.commit();

			}
		}
	}
}
