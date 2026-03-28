package com.example.ticketapp.models;
import com.google.firebase.Timestamp;

public class Event {
    private String id; 
    private String title;
    private Timestamp date;
    private String venue;
    private String category;
    private String city;
    private double price;

    public Event() {}

    public Event(String title, Timestamp date, String venue, String city, String category, double price) {
        this.title = title;
        this.date = date;
        this.venue = venue;
        this.city = city;
        this.category = category;
        this.price = price;
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public Timestamp getDate() { return date; }
    public void setDate(Timestamp date) { this.date = date; }
    
    public String getVenue() { return venue; }
    public void setVenue(String venue) { this.venue = venue; }
    
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    
    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }
    
    public double getPrice() { return price; }
    public void setPrice(double price) { this.price = price; }
}