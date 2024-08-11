package net.festinger.historia;

import net.festinger.historia.MyLocation.LocationResult;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import android.content.Context;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.SystemClock;
import android.provider.CallLog;
import android.telephony.gsm.GsmCellLocation;

public class CallStrings // class for each call strings
{
	private Location location = null;
	public String[] strings = new String[8];
	public final static int NUMBER = 0;
	public final static int NAME = 1;
	public final static int TIME = 2;
	public final static int DURATION = 3;
	public final static int TYPE = 4;
	public final static int LOCATION = 5;
	public final static int SN = 6;
	private Context context;
	protected String strloc = "";

	CallStrings(String number, String name, long time, String duration,
			int type, boolean isLocationValid, Context context) // constructor
																// to store data
																// in object
	{
		this.context = context;
		strings[NAME] = name == null ? "Unknown" : name
				.replaceAll("\"", "\"\"");
		strings[NUMBER] = (number == null || number.isEmpty()) ? "Unknown"
				: number;
		switch (type) // set type
		{
		case CallLog.Calls.MISSED_TYPE:
			strings[TYPE] = "Missed";
			break;
		case CallLog.Calls.INCOMING_TYPE:
			strings[TYPE] = "Incoming";
			break;
		default:
			strings[TYPE] = "Outgoing";
		}
		if (Main.dateFormat == 0 && Main.timeFormat == 0) {
			strings[TIME] = new SimpleDateFormat("M/d/yy, h:mm aa")
					.format(new Date(time)); // US date format
		} else if (Main.keys.getInt("DATEFORMAT", 0) == 1
				&& Main.keys.getInt("TIMEFORMAT", 0) == 1)
			strings[TIME] = new SimpleDateFormat("d/M/yy, H:mm")
					.format(new Date(time)); // European date format
		else if (Main.keys.getInt("DATEFORMAT", 0) == 0
				&& Main.keys.getInt("TIMEFORMAT", 0) == 1)
			strings[TIME] = new SimpleDateFormat("M/d/yy, H:mm")
					.format(new Date(time));
		else
			strings[TIME] = new SimpleDateFormat("d/M/yy, h:mm aa")
					.format(new Date(time));
		// ---- set duration, converting seconds to 0:00:00 format----
		int intDuration = Integer.parseInt(duration);
		int hours = intDuration / 3600;
		strings[DURATION] = hours == 0 ? "" : (Integer.toString(hours) + ":");
		intDuration %= 3600;
		int minutes = intDuration / 60;
		strings[DURATION] += (Integer.toString(minutes).length() == 2 ? Integer
				.toString(minutes) : ("0" + Integer.toString(minutes))) + ":";
		intDuration %= 60;
		strings[DURATION] += Integer.toString(intDuration).length() == 2 ? Integer
				.toString(intDuration) : ("0" + Integer.toString(intDuration));
		// ------------------------
		strings[SN] = Long.toString(time);
		if (isLocationValid == true) {
			MyLocation myLocation = new MyLocation();
			myLocation.getLocation(context, locationResult);
			long endTime = SystemClock.elapsedRealtime() + 60000;
			while (true) {
				try {
					Thread.sleep(1000);
				} catch (InterruptedException e) {
				}
				if (location != null
						|| endTime < SystemClock.elapsedRealtime()) {
					break;
				}
			}
			strings[LOCATION] = getLocationStr(location);
		} else
			strings[LOCATION] = "Not Available";

	};

	public LocationResult locationResult = new LocationResult() {

		@Override
		public void gotLocation(Location loc) {
			// TODO Auto-generated method stub
			location = loc;
		}
	};

	private String getLocationStr(Location location) {
		String locationStr = "Not Available";
		if (location != null) {
			Geocoder geoCoder = new Geocoder(context, Locale.getDefault());
			try {
				List<Address> addresses = geoCoder.getFromLocation(
						location.getLatitude(), location.getLongitude(), 1);

				if (addresses.size() > 0) {
					locationStr = "";
					for (int i = 0; i < addresses.get(0)
							.getMaxAddressLineIndex(); i++)

						locationStr += addresses.get(0).getAddressLine(i)
								+ "\n";
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		}
		return locationStr;
	}

	CallStrings() // default constructor
	{
	};

	CallStrings(String number, String name, String time, String duration,
			String type, String location, String sn) // constructor to store
														// data in object
	{
		strings[NUMBER] = number;
		strings[NAME] = name;
		strings[TIME] = time;
		strings[DURATION] = duration;
		strings[TYPE] = type;
		strings[LOCATION] = location;
		strings[SN] = sn;
	};

	public String toString() // used in writing to csv
	{
		if (Main.dateFormat == 0 && Main.timeFormat == 0) {
			strings[TIME] = new SimpleDateFormat("M/d/yy, h:mm aa").format(Long
					.parseLong(strings[SN])); // US date format
		} else if (Main.dateFormat == 1 && Main.timeFormat == 1)
			strings[TIME] = new SimpleDateFormat("d/M/yy, H:mm").format(Long
					.parseLong(strings[SN])); // European date format
		else if (Main.dateFormat == 0 && Main.timeFormat == 1)
			strings[TIME] = new SimpleDateFormat("M/d/yy, H:mm").format(Long
					.parseLong(strings[SN]));
		else
			strings[TIME] = new SimpleDateFormat("d/M/yy, h:mm aa").format(Long
					.parseLong(strings[SN]));
		return '"' + strings[NUMBER] + "\",\"" + strings[NAME] + "\",\""
				+ strings[TIME] + "\",\"" + strings[DURATION] + "\",\""
				+ strings[TYPE] + "\",\"" + "\",\"" + strings[LOCATION]
				+ "\",\"" + strings[SN] + '"' + '\r' + '\n';
	}
}