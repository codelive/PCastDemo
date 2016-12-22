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

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.phenixp2p.demo.R;
import com.phenixp2p.demo.model.StreamList;

import java.util.List;

public class StreamIdAdapter extends RecyclerView.Adapter<StreamIdAdapter.StreamViewHolder> {
  private OnItemClickListener listener;

  public StreamIdAdapter(OnItemClickListener clickListener) {
    this.listener = clickListener;
  }

  @Override
  public StreamViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
    View itemView = LayoutInflater.from(parent.getContext())
    .inflate(R.layout.item_stream_id, parent, false);
    return new StreamViewHolder(itemView);
  }

  @Override
  public void onBindViewHolder(final StreamViewHolder holder, final int position) {
    if (listener.getListStreams().size() >= 1) {
      StreamList.Stream streamID = listener.getListStreams().get(position);
      String streamId = streamID.getStreamId();
      String newStreamId = streamId.substring(0, streamId.indexOf("#") + 1).concat("...").concat(streamId.substring(streamId.length() - 4, streamId.length()));
      if (position == 0 && listener.isThisPhone()) {
        holder.title.setText(holder.title.getContext().getResources().getString(R.string.this_phone, newStreamId));
      } else {
        holder.title.setText(newStreamId);
      }
    }
  }

  @Override
  public int getItemCount() {
    return listener.getListStreams().size();
  }

  public interface OnItemClickListener {
    void onItemClick(View itemView, int position);

    List<StreamList.Stream> getListStreams();

    boolean isThisPhone();
  }

  public class StreamViewHolder extends RecyclerView.ViewHolder {
    public TextView title;

    public StreamViewHolder(View view) {
      super(view);
      title = (TextView) view.findViewById(R.id.tvId);
      view.setOnClickListener(new View.OnClickListener() {
        @Override
        public void onClick(View view) {
          listener.onItemClick(itemView, getLayoutPosition());
        }
      });
    }
  }
}
