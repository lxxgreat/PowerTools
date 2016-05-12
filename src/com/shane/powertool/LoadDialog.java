
package com.shane.powertool;

import android.app.Dialog;
import android.content.Context;
import android.view.View;
import android.widget.TextView;

public class LoadDialog {
    public static Dialog getSimpleDialog(final Context context, String title, String content,
            boolean canBack, final DialogCallback callback) {
        final Dialog dialog = new Dialog(context, R.style.dialog);
        dialog.setContentView(R.layout.common_simple_dialog);
        dialog.setCancelable(canBack);
        TextView titleTv = (TextView) dialog.findViewById(R.id.simple_dialog_title);
        TextView contentTv = (TextView) dialog.findViewById(R.id.simple_dialog_content);
        titleTv.setText(title);
        contentTv.setText(content);

        dialog.findViewById(R.id.simple_dialog_ok).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (callback != null) {
                            callback.ok(dialog);
                        }
                    }
                });
        dialog.findViewById(R.id.simple_dialog_cancel).setOnClickListener(
                new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (callback != null) {
                            callback.cancel(dialog);
                        }
                    }
                });

        return dialog;
    }

    public interface DialogCallback {
        public void ok(Dialog dialog);

        public void cancel(Dialog dialog);
    }
}
