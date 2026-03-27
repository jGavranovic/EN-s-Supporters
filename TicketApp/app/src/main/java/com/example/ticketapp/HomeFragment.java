package com.example.ticketapp;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;
import android.text.Editable;
import android.widget.TextView;
import android.widget.ArrayAdapter;
import android.text.TextWatcher;
import android.widget.AdapterView;
import android.app.DatePickerDialog;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;
import java.text.SimpleDateFormat;
import java.util.Locale;
import java.util.Calendar;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.example.ticketapp.adapters.EventAdapter;
import com.example.ticketapp.models.Event;

public class HomeFragment extends Fragment {

    private RecyclerView recyclerView;
    private EventAdapter adapter;
    private List<Event> eventList;
    private List<Event> filteredList;
    private FirebaseFirestore db;
    private TextView noEventsText;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);
        Spinner searchTypeSpinner = view.findViewById(R.id.searchTypeSpinner);
        EditText searchBar = view.findViewById(R.id.searchBar);
        Button searchButton = view.findViewById(R.id.searchButton);

        eventList = new ArrayList<>();
        filteredList = new ArrayList<>();
        adapter = new EventAdapter(filteredList);
        noEventsText = view.findViewById(R.id.noEventsText);
        noEventsText.setVisibility(View.GONE);

        recyclerView = view.findViewById(R.id.eventsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
    
        db = FirebaseFirestore.getInstance();
        loadEvents();
    
        ArrayAdapter<CharSequence> adapterSpinner = ArrayAdapter.createFromResource(
                getContext(),
                R.array.search_types,
                android.R.layout.simple_spinner_item
        );

        adapterSpinner.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        searchTypeSpinner.setAdapter(adapterSpinner);

        searchTypeSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                String selected = parent.getItemAtPosition(position).toString();

                searchBar.setText("");
        
                if (selected.equals("Date")) {
                    searchBar.setFocusable(false); 
                    searchBar.setOnClickListener(v -> showDatePicker(searchBar));
                } else {
                    searchBar.setFocusableInTouchMode(true);
                    searchBar.setOnClickListener(null);
                }
            }
        
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    
        searchButton.setOnClickListener(v -> {
            String query = searchBar.getText().toString().trim();
            Object selected = searchTypeSpinner.getSelectedItem();
            String type = (selected != null) ? selected.toString() : "All";
    
            if (query.isEmpty()) {
                filteredList.clear();
                filteredList.addAll(eventList);
                adapter.notifyDataSetChanged();
            } else {
                filterEvents(query, type);
            }
        });
    
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) { }
    
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim();
                if (query.isEmpty()) {
                    filteredList.clear();
                    filteredList.addAll(eventList);
                    adapter.notifyDataSetChanged();
                    noEventsText.setVisibility(View.GONE); 
                }
            }
            @Override
            public void afterTextChanged(Editable s) { }
        });
    
        return view;
    }
    
    private void showDatePicker(EditText searchBar) {
        final Calendar c = Calendar.getInstance();
        int year = c.get(Calendar.YEAR);
        int month = c.get(Calendar.MONTH);
        int day = c.get(Calendar.DAY_OF_MONTH);
    
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String monthString = String.format("%02d", selectedMonth + 1);
                    String dayString = String.format("%02d", selectedDay);
                    String formattedDate = selectedYear + "-" + monthString + "-" + dayString;
    
                    searchBar.setText(formattedDate);
    
                    String query = formattedDate;
                    String type = "Date";
                    filterEvents(query, type);
                },
                year, month, day
        );
        datePickerDialog.getDatePicker().setMinDate(System.currentTimeMillis() - 1000);
        datePickerDialog.show();
    }

    private void loadEvents() {
        db.collection("events")
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        eventList.clear();
                        long nowMillis = System.currentTimeMillis();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Event event = doc.toObject(Event.class);
                            if (event.getDate().toDate().getTime() >= nowMillis) {
                                eventList.add(event);
                            }
                        }
                        filteredList.clear();
                        filteredList.addAll(eventList);
                        adapter.notifyDataSetChanged();
                    } else {
                        noEventsText.setText("Error fetching events");
                        noEventsText.setVisibility(View.VISIBLE);
                    }
                });
    }

    private void filterEvents(String query, String type) {
        query = query.toLowerCase();
        filteredList.clear();
        long nowMillis = System.currentTimeMillis();

        for (Event event : eventList) {
            if (event.getDate().toDate().getTime() < nowMillis) continue;
            boolean match = false;
    
            if (type.equals("All")) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                String dateString = sdf.format(event.getDate().toDate());
                match = event.getTitle().toLowerCase().contains(query)
                        || event.getCategory().toLowerCase().contains(query)
                        || event.getVenue().toLowerCase().contains(query)
                        || event.getCity().toLowerCase().contains(query)
                        || dateString.toLowerCase().contains(query)
                        || String.valueOf(event.getPrice()).contains(query);
            } else {
                switch (type) {
                    case "Category":
                        match = event.getCategory().toLowerCase().contains(query);
                        break;
                    case "Date":
                        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault());
                        String dateString = sdf.format(event.getDate().toDate());
                        match = dateString.toLowerCase().contains(query);
                        break;
                    case "Location":
                        match = event.getVenue().toLowerCase().contains(query)
                                || event.getCity().toLowerCase().contains(query);
                        break;
                }
            }
            if (match) filteredList.add(event);
        }

        if (filteredList.isEmpty()) {
            noEventsText.setVisibility(View.VISIBLE); 
        } else {
            noEventsText.setVisibility(View.GONE);  
        }
        adapter.notifyDataSetChanged();
    }
}