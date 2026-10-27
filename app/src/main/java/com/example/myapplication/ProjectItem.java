package com.example.myapplication;

import org.json.JSONException;
import org.json.JSONObject;

public class ProjectItem {

    private String id;
    private String category; // "clases", "trabajo", "personal", "eventos", "gym", "newproyect"
    private String title;
    private String topic;
    private String building;
    private String room;
    private String contact;
    private String days;
    private String time;
    private boolean reminderEnabled;
    private String priority;
    private int cardColor;
    private String description;
    private String focusArea;

    public ProjectItem(String id, String category, String title, String topic, String building,
                       String room, String contact, String days, String time,
                       boolean reminderEnabled, String priority, int cardColor, String description,
                       String focusArea) {
        this.id = id;
        this.category = category;
        this.title = title;
        this.topic = topic;
        this.building = building;
        this.room = room;
        this.contact = contact;
        this.days = days;
        this.time = time;
        this.reminderEnabled = reminderEnabled;
        this.priority = priority;
        this.cardColor = cardColor;
        this.description = description;
        this.focusArea = focusArea;
    }

    public ProjectItem(String id, String category, String title, String topic, String building,
                       String room, String contact, String days, String time,
                       boolean reminderEnabled, String priority, int cardColor, String description) {
        this(id, category, title, topic, building, room, contact, days, time,
             reminderEnabled, priority, cardColor, description, "🎯 Ubicación / Zona no registrada");
    }

    public JSONObject toJsonObject() {
        JSONObject json = new JSONObject();
        try {
            json.put("id", id);
            json.put("category", category);
            json.put("title", title);
            json.put("topic", topic);
            json.put("building", building);
            json.put("room", room);
            json.put("contact", contact);
            json.put("days", days);
            json.put("time", time);
            json.put("reminderEnabled", reminderEnabled);
            json.put("priority", priority);
            json.put("cardColor", cardColor);
            json.put("description", description);
            json.put("focusArea", focusArea);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

    public static ProjectItem fromJsonObject(JSONObject json) {
        try {
            return new ProjectItem(
                    json.optString("id", String.valueOf(System.currentTimeMillis())),
                    json.optString("category", "clases"),
                    json.optString("title", "Sin Título"),
                    json.optString("topic", "General"),
                    json.optString("building", "Edificio A"),
                    json.optString("room", "Aula 101"),
                    json.optString("contact", "Docente"),
                    json.optString("days", "Lunes a Viernes"),
                    json.optString("time", "08:00 AM - 10:00 AM"),
                    json.optBoolean("reminderEnabled", true),
                    json.optString("priority", "Media"),
                    json.optInt("cardColor", 0xFFD1E4F9),
                    json.optString("description", "Descripción de la actividad"),
                    json.optString("focusArea", "🎯 Ubicación / Zona no registrada")
            );
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    // Getters and Setters
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getCategory() { return category; }
    public void setCategory(String category) { this.category = category; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoom() { return room; }
    public void setRoom(String room) { this.room = room; }

    public String getContact() { return contact; }
    public void setContact(String contact) { this.contact = contact; }

    public String getDays() { return days; }
    public void setDays(String days) { this.days = days; }

    public String getTime() { return time; }
    public void setTime(String time) { this.time = time; }

    public boolean isReminderEnabled() { return reminderEnabled; }
    public void setReminderEnabled(boolean reminderEnabled) { this.reminderEnabled = reminderEnabled; }

    public String getPriority() { return priority; }
    public void setPriority(String priority) { this.priority = priority; }

    public int getCardColor() { return cardColor; }
    public void setCardColor(int cardColor) { this.cardColor = cardColor; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getFocusArea() { return focusArea != null ? focusArea : "🎯 Ubicación / Zona no registrada"; }
    public void setFocusArea(String focusArea) { this.focusArea = focusArea; }
}
