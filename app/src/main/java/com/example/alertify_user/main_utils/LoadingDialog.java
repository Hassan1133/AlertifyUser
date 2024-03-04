package com.example.alertify_user.main_utils;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;

import com.example.alertify_user.R;

public class LoadingDialog {

    private static Dialog dialog;

    public static Dialog showLoadingDialog(Context context) {
        dialog = new Dialog(context);
        dialog.setContentView(R.layout.loading_dialog);
        dialog.setCancelable(false); // To prevent the dialog from being dismissed by touching outside

        // Make the dialog background transparent
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        dialog.show();

        return dialog;
    }

    public static void hideLoadingDialog(Dialog dialog) {
        if (dialog != null && dialog.isShowing()) {
            dialog.dismiss();
        }
    }
}
