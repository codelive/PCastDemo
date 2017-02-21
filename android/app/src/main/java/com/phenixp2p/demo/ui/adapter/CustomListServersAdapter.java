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

package com.phenixp2p.demo.ui.adapter;

import android.annotation.SuppressLint;
import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.TextView;

import com.phenixp2p.demo.PhenixApplication;
import com.phenixp2p.demo.R;
import com.phenixp2p.demo.model.ServerLocation;

import java.util.List;

public final class CustomListServersAdapter extends BaseAdapter {
  private Context context;
  private LayoutInflater inflter;
  private List<ServerLocation> secretUrlList;

  public CustomListServersAdapter(Context applicationContext, List<ServerLocation> secretUrlList) {
    this.context = applicationContext;
    this.inflter = (LayoutInflater.from(applicationContext));
    this.secretUrlList = secretUrlList;
  }

  @Override
  public int getCount() {
    return this.secretUrlList.size();
  }

  @Override
  public Object getItem(int position) {
    return this.secretUrlList.get(position);
  }

  @Override
  public long getItemId(int position) {
    return position;
  }

  @SuppressLint("ViewHolder")
  @Override
  public View getView(int i, View view, ViewGroup viewGroup) {
    view = this.inflter.inflate(R.layout.spinner_item, viewGroup, false);
    TextView names = (TextView) view.findViewById(R.id.textViewServer);
    names.setText(this.secretUrlList.get(i).getTitle());
    return view;
  }

  @Override
  public View getDropDownView(int position, View convertView, ViewGroup parent) {
    convertView = this.inflter.inflate(R.layout.spinner_item, parent, false);
    TextView names = (TextView) convertView.findViewById(R.id.textViewServer);
    names.setText(this.secretUrlList.get(position).getTitle());
    int positionSave = ((PhenixApplication) this.context.getApplicationContext()).getPositionUriMenu();
    if (positionSave == position) {
      names.setBackgroundColor(ContextCompat.getColor(this.context, R.color.blue));
    }
    return convertView;
  }
}
