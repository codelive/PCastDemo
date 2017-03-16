/*
 * Copyright (c) 2016. PhenixP2P Inc. All Rights Reserved.
 * Licensed under the Apache License, Version 2.0(the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.phenixp2p.demo;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.support.v7.app.AlertDialog;
import android.support.v7.widget.AppCompatButton;
import android.support.v7.widget.AppCompatSpinner;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.Toast;

import com.phenixp2p.demo.model.ServerLocation;
import com.phenixp2p.demo.model.ServerLocationFactory;
import com.phenixp2p.demo.ui.activities.MainActivity;
import com.phenixp2p.demo.ui.adapter.CustomListServersAdapter;
import com.phenixp2p.demo.utils.Utilities;

import java.util.List;

import static com.phenixp2p.demo.Constants.REQUEST_CODE_SECRET_URL;
import static com.phenixp2p.demo.utils.LogsUtil.currentDate;
import static com.phenixp2p.demo.utils.LogsUtil.generateLog;

public final class UriMenu {
  private int selection = 0;

  public void onActionSecretUrl(final Activity activity) {
    final PhenixApplication phenixApplication = ((PhenixApplication) activity.getApplication());

    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
    final LayoutInflater inflater = LayoutInflater.from(activity);
    final View customView = inflater.inflate(R.layout.popup_secret_url, new LinearLayout(activity), false);
    builder.setView(customView);
    final EditText editServerAddress = (EditText) customView.findViewById(R.id.editServerAddress);
    final EditText editPcastAddress = (EditText) customView.findViewById(R.id.editPcastAddress);
    final Button buttonEmailLog  = (Button) customView.findViewById(R.id.buttonEmailLog);
    editServerAddress.setSelection(editServerAddress.getText().length());

    this.createSpinner(phenixApplication, activity, customView, editPcastAddress, editServerAddress);
    this.createAlertDialog(phenixApplication, activity, builder, customView, editServerAddress, editPcastAddress, buttonEmailLog);
  }

  private void createSpinner(final PhenixApplication application,
                             final Activity activity,
                             final View customView,
                             final EditText editPcastAddress,
                             final EditText editServerAddress) {

    final ServerLocationFactory locationFactory = new ServerLocationFactory(activity);
    final List<ServerLocation> urlList = locationFactory.getLocations();
    final CustomListServersAdapter adapter = new CustomListServersAdapter(activity, urlList);
    final AppCompatSpinner spinner = (AppCompatSpinner) customView.findViewById(R.id.spinner);

    spinner.setAdapter(adapter);

    int position = application.getPositionUriMenu();
    String serverAddress = application.getServerAddress();
    String pcastAddress = application.getPcastAddress();
    if (position == 0) {
      if (serverAddress != null && !Utilities.areEqual(serverAddress, ServerAddress.PRODUCTION_ENDPOINT.getServerAddress())) {
        editPcastAddress.setText(pcastAddress);
        editServerAddress.setText(serverAddress);
      } else {
        spinner.setSelection(1);
      }
    } else {
      spinner.setSelection(position);
    }

    spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

      @Override
      public void onItemSelected(AdapterView<?> adapterView, View view, int i, long l) {
        selection = i;
        if (i > 0) {
          editServerAddress.setText(urlList.get(i).getServerAddress());
          editPcastAddress.setText(urlList.get(i).getPcastAddress());
        }
        editServerAddress.setSelection(editServerAddress.getText().length());
        adapter.notifyDataSetChanged();
      }

      @Override
      public void onNothingSelected(AdapterView<?> adapterView) {}
    });

    this.addTextChangedListen(urlList, spinner, editServerAddress, editPcastAddress);
  }

  private void createAlertDialog(final PhenixApplication application,
                                 final Activity activity,
                                 final AlertDialog.Builder builder,
                                 final View customView,
                                 final EditText editServerAddress,
                                 final EditText editPcastAddress,
                                 final Button buttonEmailLog) {

    AppCompatButton buttonSubmit = (AppCompatButton) customView.findViewById(R.id.buttonSubmit);
    AppCompatButton buttonCancel = (AppCompatButton) customView.findViewById(R.id.buttonCancel);

    final AlertDialog dialog = builder.create();
    builder.setCancelable(false);
    buttonSubmit.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        String serverAddress = editServerAddress.getText().toString();
        String pcastUrl = editPcastAddress.getText().toString();
        if (!serverAddress.equals("")) {
          ((MainActivity) activity).onRemove();
          activity.stopService(new Intent(activity, PhenixService.class));
          saveNewServerAddress(application, serverAddress, pcastUrl);
          activity.startActivityForResult(new Intent(activity, MainActivity.class), REQUEST_CODE_SECRET_URL);
          savePositionList(application, UriMenu.this.selection);
          dialog.dismiss();
          activity.finish();
        } else {
          editServerAddress.setFocusable(true);
          Toast.makeText(activity, R.string.input_address, Toast.LENGTH_SHORT).show();
        }
      }
    });

    buttonCancel.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View v) {
        dialog.dismiss();
      }
    });

    buttonEmailLog.setOnClickListener(new View.OnClickListener() {
      @Override
      public void onClick(View view) {
        String subject = "Log collected at " + currentDate();
        String bodyText = "Describe the issue you encountered";
        Uri path = Uri.fromFile(generateLog());
        if (path != null) {
          Intent mail = new Intent(Intent.ACTION_SENDTO);
          mail.setData(Uri.parse("mailto:"));
          mail.putExtra(Intent.EXTRA_EMAIL, new String[]{""});
          mail.putExtra(Intent.EXTRA_SUBJECT, subject);
          mail.putExtra(Intent.EXTRA_TEXT, bodyText);
          mail.putExtra(Intent.EXTRA_STREAM, path);
          activity.startActivity(Intent.createChooser(mail, "Send email..."));
        }
      }
    });
    dialog.setCancelable(false);
    dialog.show();
  }

  private void addTextChangedListen(final List<ServerLocation> urlList, final AppCompatSpinner spinner, EditText editServerAddress, EditText editPcastAddress) {
    editServerAddress.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        if (!Utilities.areEqual(urlList.get(UriMenu.this.selection).getServerAddress(), charSequence.toString())) {
          spinner.setSelection(0);
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {}
    });

    editPcastAddress.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence charSequence, int start, int count, int after) {}

      @Override
      public void onTextChanged(CharSequence charSequence, int start, int before, int count) {
        if (!Utilities.areEqual(urlList.get(UriMenu.this.selection).getPcastAddress(), charSequence.toString())) {
          spinner.setSelection(0);
        }
      }

      @Override
      public void afterTextChanged(Editable editable) {}
    });
  }

  private void saveNewServerAddress(PhenixApplication application, String serverAddress, String pcastUrl) {
    application.setServerAddress(serverAddress);
    application.setPcastAddress(pcastUrl);
  }

  private void savePositionList(PhenixApplication application, int position) {
    application.setPositionUriMenu(position);
  }
}
