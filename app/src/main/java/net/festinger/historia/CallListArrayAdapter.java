package net.festinger.historia;

import java.util.ArrayList;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

public class CallListArrayAdapter extends ArrayAdapter<CallStrings> {
	  private final Context context;
	  private final ArrayList<CallStrings> values;

	  public CallListArrayAdapter(Context context, ArrayList<CallStrings> values) {
	    super(context, R.layout.rowlayout, values);
	    this.context = context;
	    this.values = values;
	  }

	  @Override
	  public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) context
	        .getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View rowView = inflater.inflate(R.layout.rowlayout, parent, false);
	    TextView textView = (TextView) rowView.findViewById(R.id.number);
	    textView.setText(values.get(position).strings[CallStrings.NUMBER]);
	    textView = (TextView) rowView.findViewById(R.id.date);
	    textView.setText(values.get(position).strings[CallStrings.TIME]);
	    textView = (TextView) rowView.findViewById(R.id.name);
	    textView.setText(values.get(position).strings[CallStrings.NAME]);
	    textView = (TextView) rowView.findViewById(R.id.duration);
	    textView.setText(values.get(position).strings[CallStrings.DURATION]);
	    ImageView imageView = (ImageView) rowView.findViewById(R.id.icon);

	    // Change the icon for Windows and iPhone
	    String s = values.get(position).strings[CallStrings.TYPE];
	    if (s.compareTo("Incoming") == 0)
	    {
	      imageView.setImageResource(R.drawable.call_type_incoming);
	    }
	    else if (s.compareTo("Missed") == 0)
	    {
	      imageView.setImageResource(R.drawable.call_type_error);
	    }
	    else
	    {
	      imageView.setImageResource(R.drawable.call_type_outgoing);
	    }

	    return rowView;
	  }
	} 