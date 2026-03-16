package com.example.ticketapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast; 
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
       // Inflate the XML layout
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        
        // Get UI elements
        EditText searchBar = view.findViewById(R.id.searchBar);
        Button searchButton = view.findViewById(R.id.searchButton);
        
        // Button click
        searchButton.setOnClickListener(v -> {
            String searchQuery = searchBar.getText().toString().trim();
            if (searchQuery.isEmpty()) {
                Toast.makeText(getContext(), "Please enter a search query", Toast.LENGTH_SHORT).show();
                return;
            }
            Toast.makeText(getContext(), "Searching for: " + searchQuery, Toast.LENGTH_SHORT).show();
        });
        
        return view;
    }
}
