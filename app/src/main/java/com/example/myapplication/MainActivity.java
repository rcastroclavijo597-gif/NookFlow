package com.example.myapplication;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setLoginLayout();
    }

    private void setLoginLayout() {
        setContentView(R.layout.iniciodesesion);
        applyWindowInsets(findViewById(R.id.main));

        Button btnLogin = findViewById(R.id.btnLogin);
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> setLobbyLayout());
        }
    }

    private void setLobbyLayout() {
        setContentView(R.layout.loby);
        applyWindowInsets(findViewById(R.id.main_loby));

        View btnFloatingAdd = findViewById(R.id.btnFloatingAdd);
        if (btnFloatingAdd != null) {
            btnFloatingAdd.setOnClickListener(v -> setInicioLayout());
        }

        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> setNotiLayout("loby"));
        }
    }

    private void setInicioLayout() {
        setContentView(R.layout.inicio);
        applyWindowInsets(findViewById(R.id.main_inicio));

        setupSidebarNavigation();

        // Navigation cards
        View cardClases = findViewById(R.id.cardClases);
        if (cardClases != null) cardClases.setOnClickListener(v -> setClasesLayout());

        View cardTrabajo = findViewById(R.id.cardTrabajo);
        if (cardTrabajo != null) cardTrabajo.setOnClickListener(v -> setTrabajosLayout());

        View cardPersonal = findViewById(R.id.cardPersonal);
        if (cardPersonal != null) cardPersonal.setOnClickListener(v -> setPersonalLayout());

        View cardEventos = findViewById(R.id.cardEventos);
        if (cardEventos != null) cardEventos.setOnClickListener(v -> setEventosLayout());

        View cardCrear = findViewById(R.id.cardCrear);
        if (cardCrear != null) cardCrear.setOnClickListener(v -> setNewProyectLayout());

        View cardGym = findViewById(R.id.cardGym);
        if (cardGym != null) cardGym.setOnClickListener(v -> setGymLayout());

        ImageView btnNotifications = findViewById(R.id.btnNotifications);
        if (btnNotifications != null) {
            btnNotifications.setOnClickListener(v -> setNotiLayout("inicio"));
        }

        ImageView btnNavHome = findViewById(R.id.btnNavHome);
        if (btnNavHome != null) {
            btnNavHome.setOnClickListener(v -> setLobbyLayout());
        }
    }

    private void setupSidebarNavigation() {
        View navSchool = findViewById(R.id.navIconSchool);
        if (navSchool != null) navSchool.setOnClickListener(v -> setClasesLayout());

        View navWork = findViewById(R.id.navIconWork);
        if (navWork != null) navWork.setOnClickListener(v -> setTrabajosLayout());

        View navPerson = findViewById(R.id.navIconPerson);
        if (navPerson != null) navPerson.setOnClickListener(v -> setPersonalLayout());

        View navGym = findViewById(R.id.navIconGym);
        if (navGym != null) navGym.setOnClickListener(v -> setGymLayout());

        View navEvent = findViewById(R.id.navIconEvent);
        if (navEvent != null) navEvent.setOnClickListener(v -> setEventosLayout());

        View sidebarAdd = findViewById(R.id.btnFloatingAddInicio);
        if (sidebarAdd != null) sidebarAdd.setOnClickListener(v -> setNewProyectLayout());
    }

    // Detail Views
    private void setClasesLayout() {
        setContentView(R.layout.clases);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setTrabajosLayout() {
        setContentView(R.layout.trabajos);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setPersonalLayout() {
        setContentView(R.layout.personal);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setEventosLayout() {
        setContentView(R.layout.eventos);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setNewProyectLayout() {
        setContentView(R.layout.newproyect);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setGymLayout() {
        setContentView(R.layout.gym);
        setupSidebarNavigation();
        View btnClose = findViewById(R.id.btnClose);
        if (btnClose != null) btnClose.setOnClickListener(v -> setInicioLayout());
    }

    private void setNotiLayout(String from) {
        setContentView(R.layout.noti);
        View btnBack = findViewById(R.id.btnBack);
        if (btnBack != null) {
            btnBack.setOnClickListener(v -> {
                if ("loby".equals(from)) setLobbyLayout();
                else setInicioLayout();
            });
        }
    }

    private void applyWindowInsets(View view) {
        if (view == null) return;
        ViewCompat.setOnApplyWindowInsetsListener(view, (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });
    }
}
