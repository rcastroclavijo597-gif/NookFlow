package com.example.myapplication;

import android.content.Context;
import android.content.SharedPreferences;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class DataManager {

    private static final String PREF_NAME = "nookflow_data";
    private static final String KEY_PROJECTS = "projects_list";
    private static final String KEY_USER_NAME = "user_name";
    private static final String KEY_USER_EMAIL = "user_email";
    private static final String KEY_USER_PASSWORD = "user_password";

    private final SharedPreferences prefs;

    public DataManager(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        initDefaultDataIfEmpty();
    }

    private void initDefaultDataIfEmpty() {
        if (!prefs.contains(KEY_PROJECTS)) {
            List<ProjectItem> defaults = new ArrayList<>();
            defaults.add(new ProjectItem(
                    "1", "clases", "Programación Móvil", "Android Studio",
                    "Edificio B", "Aula 204", "Prof. García",
                    "Lunes y Miércoles", "08:00 AM - 10:00 AM",
                    true, "Alta", 0xFFD1E4F9, "Desarrollo de Apps en Java"
            ));
            defaults.add(new ProjectItem(
                    "2", "trabajo", "Proyecto NookFlow", "Entregables",
                    "Torre Norte", "Sala 302", "Lic. Martínez",
                    "Martes y Jueves", "10:00 AM - 12:00 PM",
                    true, "Alta", 0xFFE1D1F9, "Reunión de avance y sprints"
            ));
            defaults.add(new ProjectItem(
                    "3", "personal", "Planificación Semanal", "Finanzas",
                    "Casa", "Estudio", "Personal",
                    "Viernes", "04:00 PM - 05:00 PM",
                    false, "Media", 0xFFE1F9E8, "Revisión de presupuesto"
            ));
            defaults.add(new ProjectItem(
                    "4", "eventos", "Conferencia Tech 2024", "Networking",
                    "Auditorio Central", "Escenario A", "Org. Tech",
                    "20 de Mayo", "09:00 AM - 02:00 PM",
                    true, "Alta", 0xFF84F9A1, "Presentación de proyectos AI"
            ));
            defaults.add(new ProjectItem(
                    "5", "gym", "Rutina de Torso & Cardio", "Bienestar",
                    "Gimnasio Central", "Área de Pesas", "Entrenador Luis",
                    "Lunes a Viernes", "06:00 PM - 07:30 PM",
                    true, "Media", 0xFFFFF4D1, "Foco: Pecho y Tríceps"
            ));

            saveProjects(defaults);
        }
    }

    public List<ProjectItem> getAllProjects() {
        List<ProjectItem> list = new ArrayList<>();
        String jsonString = prefs.getString(KEY_PROJECTS, "[]");
        try {
            JSONArray array = new JSONArray(jsonString);
            for (int i = 0; i < array.length(); i++) {
                JSONObject obj = array.getJSONObject(i);
                ProjectItem item = ProjectItem.fromJsonObject(obj);
                if (item != null) {
                    list.add(item);
                }
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return list;
    }

    public ProjectItem getProjectByCategory(String category) {
        List<ProjectItem> all = getAllProjects();
        for (ProjectItem item : all) {
            if (item.getCategory().equalsIgnoreCase(category)) {
                return item;
            }
        }
        return null;
    }

    public void saveProject(ProjectItem updatedItem) {
        List<ProjectItem> list = getAllProjects();
        boolean found = false;
        for (int i = 0; i < list.size(); i++) {
            if (list.get(i).getId().equals(updatedItem.getId()) ||
                list.get(i).getCategory().equalsIgnoreCase(updatedItem.getCategory())) {
                list.set(i, updatedItem);
                found = true;
                break;
            }
        }
        if (!found) {
            list.add(updatedItem);
        }
        saveProjects(list);
    }

    public void deleteProject(String id) {
        List<ProjectItem> list = getAllProjects();
        List<ProjectItem> updated = new ArrayList<>();
        for (ProjectItem item : list) {
            if (!item.getId().equals(id)) {
                updated.add(item);
            }
        }
        saveProjects(updated);
    }

    private void saveProjects(List<ProjectItem> list) {
        JSONArray array = new JSONArray();
        for (ProjectItem item : list) {
            array.put(item.toJsonObject());
        }
        prefs.edit().putString(KEY_PROJECTS, array.toString()).apply();
    }

    public String getUserName() {
        return prefs.getString(KEY_USER_NAME, "Usuario");
    }

    public void setUserName(String name) {
        prefs.edit().putString(KEY_USER_NAME, name).apply();
    }

    public void registerUser(String name, String email, String password) {
        prefs.edit()
                .putString(KEY_USER_NAME, name)
                .putString(KEY_USER_EMAIL, email.trim().toLowerCase())
                .putString(KEY_USER_PASSWORD, password)
                .apply();
    }

    public boolean hasRegisteredAccount() {
        return prefs.contains(KEY_USER_EMAIL) && prefs.contains(KEY_USER_PASSWORD);
    }

    public boolean validateCredentials(String email, String password) {
        if (!hasRegisteredAccount()) return false;
        String savedEmail = prefs.getString(KEY_USER_EMAIL, "");
        String savedPassword = prefs.getString(KEY_USER_PASSWORD, "");
        return savedEmail.equalsIgnoreCase(email.trim()) && savedPassword.equals(password);
    }

    public String getRegisteredEmail() {
        return prefs.getString(KEY_USER_EMAIL, "");
    }
}
