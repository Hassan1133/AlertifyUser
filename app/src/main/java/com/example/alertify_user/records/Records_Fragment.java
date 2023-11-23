package com.example.alertify_user.records;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.Nullable;
import androidx.cardview.widget.CardView;
import androidx.fragment.app.Fragment;

import com.example.alertify_user.R;
import com.example.alertify_user.records.crimes.CrimesActivity;
import com.example.alertify_user.records.criminals.CriminalsActivity;
import com.example.alertify_user.records.laws.LawsActivity;

public class Records_Fragment extends Fragment implements View.OnClickListener {

    private CardView crimes, laws, criminals;

    private Intent intent;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.records_fragment, container, false);
        init(view);
        return view;
    }

    private void init(View view) {
        crimes = view.findViewById(R.id.crimes);
        crimes.setOnClickListener(this);

        laws = view.findViewById(R.id.laws);
        laws.setOnClickListener(this);

        criminals = view.findViewById(R.id.criminals);
        criminals.setOnClickListener(this);
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.crimes:
                intent = new Intent(getActivity(), CrimesActivity.class);
                startActivity(intent);
                break;
            case R.id.laws:
                intent = new Intent(getActivity(), LawsActivity.class);
                startActivity(intent);
                break;
            case R.id.criminals:
                intent = new Intent(getActivity(), CriminalsActivity.class);
                startActivity(intent);
                break;
        }
    }
}
