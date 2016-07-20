package hu.bme.mit.r5cop_wp24.voiceagent;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.base.Preconditions;

import java.util.ArrayList;
import java.util.List;

public class ShoppingList {

    public static int SHOPPING_LIST_QR_SCAN_NEW_ID = 236549782;
    public static int SHOPPING_LIST_QR_SCAN_EXISTING_ID = 236549783;


    public static void notifyDataSetChanged(Dialog d) {
        ((ShoppingList.ShoppingListAdapter)((ListView)d.findViewById(R.id.listView)).getAdapter()).notifyDataSetChanged();
    }

    public static Dialog createDialog(final Activity context, List<ShoppingList.ShoppingListItem>  list) {
        final Dialog dialog = new Dialog(context);
        dialog.setContentView(R.layout.activity_shopping_list);
        dialog.setTitle("Shopping List");

        ListView listView = (ListView)dialog.findViewById(R.id.listView);

        listView.setAdapter(new ShoppingListAdapter(context, R.layout.shopping_list_row, list));


        Button newButton = (Button) dialog.findViewById(R.id.buttonNewItem);
        newButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                Intent intent = new Intent(BAR_CODE_SCANNER_PACKAGE_NAME);
                intent.putExtra("SCAN_MODE", "QR_CODE_MODE");
                // Check if the Barcode Scanner is installed.
                if (!isQRCodeReaderInstalled(context, intent)) {
                    // Open the Market and take them to the page from which they can download the Barcode Scanner
                    // app.
                    dialog.dismiss();
                    context.startActivity(new Intent(Intent.ACTION_VIEW,
                            Uri.parse("market://details?id=com.google.zxing.client.android")));
                } else {
                    // Call the Barcode Scanner to let the user scan a QR code.
                    dialog.dismiss();
                    context.startActivityForResult(intent, SHOPPING_LIST_QR_SCAN_NEW_ID);
                }

            }
        });

        return dialog;

    }


    private static final String BAR_CODE_SCANNER_PACKAGE_NAME =
            "com.google.zxing.client.android.SCAN";

    public static class ShoppingListItem {

        String id;
        public ShoppingListItem(String id) {
            this.id = id;
        }
    }


    public static class ShoppingListAdapter extends ArrayAdapter<ShoppingListItem> {

        public ShoppingListAdapter(Context context, int textViewResourceId) {
            super(context, textViewResourceId);
        }

        public ShoppingListAdapter(Context context, int resource, List<ShoppingListItem> items) {
            super(context, resource, items);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            View v = convertView;

            if (v == null) {
                LayoutInflater vi;
                vi = LayoutInflater.from(getContext());
                v = vi.inflate(R.layout.shopping_list_row, null);
            }

            ShoppingListItem p = getItem(position);

            if (p != null) {
                TextView tt1 = (TextView) v.findViewById(R.id.textViewID);
                ImageButton tt2 = (ImageButton) v.findViewById(R.id.imageButton);


                if (tt1 != null) {
                    tt1.setText(p.id);
                }

                if (tt2 != null) {
                    tt2.setImageResource(R.drawable.ic_mic_off_black_48dp);
                }
            }

            return v;
        }

    }

    private static boolean isQRCodeReaderInstalled(Context context, Intent intent) {
        List<ResolveInfo> list =
                context.getPackageManager().queryIntentActivities(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return (list.size() > 0);
    }

}
