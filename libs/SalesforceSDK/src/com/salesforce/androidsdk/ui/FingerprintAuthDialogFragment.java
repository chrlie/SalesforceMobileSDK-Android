/*
 * Copyright (c) 2016-present, salesforce.com, inc.
 * All rights reserved.
 * Redistribution and use of this software in source and binary forms, with or
 * without modification, are permitted provided that the following conditions
 * are met:
 * - Redistributions of source code must retain the above copyright notice, this
 * list of conditions and the following disclaimer.
 * - Redistributions in binary form must reproduce the above copyright notice,
 * this list of conditions and the following disclaimer in the documentation
 * and/or other materials provided with the distribution.
 * - Neither the name of salesforce.com, inc. nor the names of its contributors
 * may be used to endorse or promote products derived from this software without
 * specific prior written permission of salesforce.com, inc.
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE
 * LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR
 * CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF
 * SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS
 * INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN
 * CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE)
 * ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE
 * POSSIBILITY OF SUCH DAMAGE.
 */
package com.salesforce.androidsdk.ui;

import android.Manifest.permission;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.hardware.fingerprint.FingerprintManager;
import android.hardware.fingerprint.FingerprintManager.AuthenticationCallback;
import android.hardware.fingerprint.FingerprintManager.AuthenticationResult;
import android.hardware.fingerprint.FingerprintManager.CryptoObject;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.widget.Button;
import android.widget.TextView;

import com.salesforce.androidsdk.R;
import com.salesforce.androidsdk.app.SalesforceSDKManager;

import javax.crypto.Cipher;

/**
 * A dialog which uses Fingerprint APIs to authenticate the user, and falls back to password
 * authentication if fingerprint is not available.
 */
public class FingerprintAuthDialogFragment extends DialogFragment {

    private TextView mStatusText;
    private PasscodeActivity mContext;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        boolean isDarkTheme = SalesforceSDKManager.getInstance().isDarkTheme();

        // Do not create a new Fragment when the Activity is re-created such as orientation changes.
        setRetainInstance(true);
        setStyle(DialogFragment.STYLE_NORMAL, (isDarkTheme ? R.style.SalesforceSDK_Dialog_Dark : R.style.SalesforceSDK_Dialog));
    }

    @Override
    public void onResume() {
        super.onResume();
        FingerprintManager fingerprintManager = (FingerprintManager) mContext.getSystemService(Context.FINGERPRINT_SERVICE);
        if (mContext.checkSelfPermission(permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {

            // If we got so far, we already got the permission in the PasscodeActivity. This is an OS mandated check.
            return;
        }
        fingerprintManager.authenticate(new CryptoObject((Cipher) null), null, 0, new AuthenticationCallback() {

            @Override
            public void onAuthenticationError(int errorCode, CharSequence errString) {
                super.onAuthenticationError(errorCode, errString);
            }

            @Override
            public void onAuthenticationSucceeded(AuthenticationResult result) {
                super.onAuthenticationSucceeded(result);
                if (mStatusText != null) {
                    mStatusText.setText(R.string.sf__biometric_success);
                    mStatusText.setTextColor(Color.GREEN);
                }
                if (FingerprintAuthDialogFragment.this.getFragmentManager() != null) {
                    FingerprintAuthDialogFragment.this.dismiss();
                }
                mContext.unlockViaFingerprintScan();
            }

            @Override
            public void onAuthenticationFailed() {
                super.onAuthenticationFailed();
                if (mStatusText != null) {
                    mStatusText.setText(R.string.sf__fingerprint_failed);
                    mStatusText.setTextColor(Color.RED);
                }
            }

            @Override
            public void onAuthenticationHelp(int helpCode, CharSequence helpString) {
                super.onAuthenticationHelp(helpCode, helpString);
                if (mStatusText != null) {
                    mStatusText.setText(helpString.toString());
                    mStatusText.setTextColor(Color.RED);
                }
            }
        }, null);
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        final Dialog dialog =  super.onCreateDialog(savedInstanceState);
        dialog.getWindow().requestFeature(Window.FEATURE_NO_TITLE);
        return dialog;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
        final View v = inflater.inflate(R.layout.sf__fingerprint_dialog, container, false);
        final Button cancelButton = v.findViewById(R.id.sf__use_password_button);
        cancelButton.setOnClickListener(new View.OnClickListener() {

            @Override
            public void onClick(View view) {
                dismiss();
                mContext.biometricDeclined();
            }
        });
        mStatusText = v.findViewById(R.id.sf__fingerprint_status);

        TextView textView = v.findViewById(R.id.sf__fingerprint_description);
        textView.setText(getString(R.string.sf__fingerprint_description, SalesforceSDKManager.getInstance().provideAppName()));

        getDialog().setCanceledOnTouchOutside(false);
        return v;
    }

    public void setContext(PasscodeActivity ctx) {
        mContext = ctx;
    }
}
