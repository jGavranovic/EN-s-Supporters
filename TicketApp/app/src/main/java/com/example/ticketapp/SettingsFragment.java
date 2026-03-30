package com.example.ticketapp;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import androidx.fragment.app.Fragment;

import com.google.firebase.auth.FirebaseAuth;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        Button authButton = view.findViewById(R.id.authButton);
        UserSession userSession = UserSession.getInstance();
        
        // GUEST 
        if (userSession.isGuest()) {
            authButton.setText("Login");
            authButton.setOnClickListener(v -> {
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            });
        } else {
            // USER or ADMIN
            authButton.setText("Logout");
            authButton.setOnClickListener(v -> {
                FirebaseAuth.getInstance().signOut();
                userSession.logout();
                Intent intent = new Intent(getActivity(), LoginActivity.class);
                startActivity(intent);
                getActivity().finish();
            });
        }
        
        return view;
    }
}
