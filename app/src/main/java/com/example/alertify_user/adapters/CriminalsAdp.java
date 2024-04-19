package com.example.alertify_user.adapters;

import static com.example.alertify_user.constants.Constants.ALERTIFY_CRIMINALS_REF;

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
import com.example.alertify_user.activities.CriminalDetailActivity;
import com.example.alertify_user.models.CriminalsModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CriminalsAdp extends RecyclerView.Adapter<CriminalsAdp.Holder> {

    private Context context;

    private List<CriminalsModel> criminalsList;


    public CriminalsAdp(Context context, List<CriminalsModel> criminalsList) {
        this.context = context;
        this.criminalsList = criminalsList;
    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.criminals_recycler_design, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        CriminalsModel criminalModel = criminalsList.get(position);

        holder.criminalName.setText(criminalModel.getCriminalName());
        holder.criminalCnic.setText(criminalModel.getCriminalCnic());

        holder.itemView.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(context, CriminalDetailActivity.class);
                intent.putExtra("criminalModel", criminalModel);
                context.startActivity(intent);
            }
        });
    }

    @Override
    public int getItemCount() {
        return criminalsList.size();
    }

    class Holder extends RecyclerView.ViewHolder {
        private TextView criminalName, criminalCnic;
        public Holder(@NonNull View itemView) {
            super(itemView);
            criminalName = itemView.findViewById(R.id.criminalName);
            criminalCnic = itemView.findViewById(R.id.criminalCnic);
        }
    }
}
