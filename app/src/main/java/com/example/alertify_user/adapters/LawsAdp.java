package com.example.alertify_user.adapters;


import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertify_user.R;
import com.example.alertify_user.activities.LawsDetailsActivity;
import com.example.alertify_user.models.LawsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class LawsAdp extends RecyclerView.Adapter<LawsAdp.Holder> {

    private final Context context;

    private final List<LawsModel> crimesList;


    public LawsAdp(Context context, List<LawsModel> crimes) {
        this.context = context;
        crimesList = crimes;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.laws_recyler_design, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        LawsModel lawsModel = crimesList.get(position);

        holder.crime.setText(lawsModel.getCrimeType());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, LawsDetailsActivity.class);
                intent.putExtra("lawsModel", lawsModel);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return crimesList.size();
    }

    class Holder extends RecyclerView.ViewHolder {

        private TextView crime;

        public Holder(@NonNull View itemView) {
            super(itemView);
            crime = itemView.findViewById(R.id.crime);
        }
    }
}
