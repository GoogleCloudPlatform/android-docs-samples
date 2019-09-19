package com.google.cloud.examples.speechtospeech.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.cloud.translate.Language;

import java.util.ArrayList;

public class LanguageAdapter extends ArrayAdapter<Language> {

    private Context context;
    private int resource;
    private ArrayList<Language> objects;

    public LanguageAdapter(@NonNull Context context, int resource, @NonNull ArrayList<Language> objects) {
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

        holder.text1.setText(objects.get(position).getName() + " (" + objects.get(position).getCode() + ")");

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

        holder.text1.setText(objects.get(position).getName() + " (" + objects.get(position).getCode() + ")");

        return convertView;
    }

    class Holder {
        TextView text1;
    }
}
