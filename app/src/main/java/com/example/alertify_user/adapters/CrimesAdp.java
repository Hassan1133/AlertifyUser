package com.example.alertify_user.adapters;

import static com.example.alertify_user.constants.Constants.ALERTIFY_CRIMES_REF;

import android.content.Context;
import android.content.DialogInterface;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.alertify_user.R;
import com.example.alertify_user.models.CrimesModel;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.Task;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.util.List;

public class CrimesAdp extends RecyclerView.Adapter<CrimesAdp.Holder> {

    private final Context context;

    private final List<CrimesModel> crimesList;

    private final DatabaseReference crimesRef;

    public CrimesAdp(Context context, List<CrimesModel> crimes) {
        this.context = context;
        crimesList = crimes;
        crimesRef = FirebaseDatabase.getInstance().getReference(ALERTIFY_CRIMES_REF);

    }

    @NonNull
    @Override
    public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.crimes_recycler_design, parent, false);

        return new Holder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull Holder holder, int position) {

        CrimesModel crimesModel = crimesList.get(position);


        holder.crime.setText(crimesModel.getCrimeType());

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