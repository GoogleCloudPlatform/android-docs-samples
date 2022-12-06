package com.google.cloud.examples.speechtospeech.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.cloud.texttospeech.v1beta1.Voice;

import java.util.ArrayList;

public class VoiceAdapter extends ArrayAdapter<Voice> {

    private Context context;
    private int resource;
    private ArrayList<Voice> objects;

    public VoiceAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Voice> objects) {
        super(context, resource, objects);
        this.context = context;
        this.resource = resource;
        this.objects = objects;
    }

    @Override
    public View getDropDownView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        Holder holder;

        if (convertView == null) {
            holder = new Holder();
            convertView = LayoutInflater.from(context).inflate(resource, null);
            holder.text1 = convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.text1.setText(objects.get(position).getName() + " (" + objects.get(position).getSsmlGender().name() + ")");

        return convertView;
    }

    @NonNull
    @Override
    public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

        Holder holder;

        if (convertView == null) {
            holder = new Holder();
            convertView = LayoutInflater.from(context).inflate(resource, null);
            holder.text1 = convertView.findViewById(android.R.id.text1);
            convertView.setTag(holder);
        } else {
            holder = (Holder) convertView.getTag();
        }

        holder.text1.setText(objects.get(position).getName() + " (" + objects.get(position).getSsmlGender().name() + ")");

        return convertView;
    }

    class Holder {
        TextView text1;
    }
}
