package com.example.ticketapp;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.fragment.app.Fragment;

import com.example.ticketapp.auth.AuthService;

public class SettingsFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);
        
        Button authButton = view.findViewById(R.id.authButton);
        UserSession userSession = UserSession.getInstance();
        
        // GUEST 
        if (userSession.isGuest()) {
            authButton.setText(R.string.login);
            authButton.setOnClickListener(v -> openLoginAndFinishHost());
        } else {
            // USER or ADMIN
            authButton.setText(R.string.logout);
            authButton.setOnClickListener(v -> {
                AuthService.getAuthApi().logout();
                userSession.logout();
                openLoginAndFinishHost();
            });
        }
        
        return view;
    }

    private void openLoginAndFinishHost() {
        Activity activity = getActivity();
        if (activity == null) {
            return;
        }

        Intent intent = new Intent(activity, LoginActivity.class);
        startActivity(intent);
        activity.finish();
    }
}
