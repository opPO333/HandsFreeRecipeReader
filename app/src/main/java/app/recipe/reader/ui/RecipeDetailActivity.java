package app.recipe.reader.ui;

import app.recipe.reader.R;
import app.recipe.reader.adapters.StepAdapter;

import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.List;

import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.RecipeDao;
import app.recipe.reader.data.RecipeStep;

public class RecipeDetailActivity extends AppCompatActivity {

    public static final String EXTRA_RECIPE_ID = "extra_recipe_id";
    public static final String EXTRA_RECIPE_TITLE = "extra_recipe_title";
    public static final String EXTRA_RECIPE_INFO = "extra_recipe_info";

    private StepAdapter adapter;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_recipe_detail);

        db = AppDatabase.getInstance(this);

        setupToolbar();
        setupRecyclerView();
        setupButtons();
        loadRecipeDetails();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> getOnBackPressedDispatcher().onBackPressed());
        
        String title = getIntent().getStringExtra(EXTRA_RECIPE_TITLE);
        TextView titleLarge = findViewById(R.id.recipe_title_large);
        titleLarge.setText(title);

        String info = getIntent().getStringExtra(EXTRA_RECIPE_INFO);
        TextView infoView = findViewById(R.id.recipe_info);
        infoView.setText(info);
    }

    private void setupRecyclerView() {
        RecyclerView recyclerView = findViewById(R.id.steps_recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new StepAdapter();
        recyclerView.setAdapter(adapter);
    }

    private void setupButtons() {
        FloatingActionButton fab = findViewById(R.id.fab_hands_free);
        fab.setOnClickListener(v -> {
            Toast.makeText(this, "Hands-free mode coming soon!", Toast.LENGTH_SHORT).show();
        });

        ImageButton btnBluetooth = findViewById(R.id.btn_bluetooth);
        btnBluetooth.setOnClickListener(v -> {
            Toast.makeText(this, "Bluetooth setup coming soon!", Toast.LENGTH_SHORT).show();
        });

        ImageButton btnDelete = findViewById(R.id.btn_delete);
        btnDelete.setOnClickListener(v -> {
            deleteRecipe();
        });
    }

    private void loadRecipeDetails() {
        int recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);
        if (recipeId == -1) return;

        RecipeDao dao = db.recipeDao();
        AppDatabase.databaseExecutor.execute(() -> {
            List<RecipeStep> steps = dao.getStepsForRecipe(recipeId);
            runOnUiThread(() -> {
                adapter.setSteps(steps);
            });
        });
    }

    private void deleteRecipe() {
        int recipeId = getIntent().getIntExtra(EXTRA_RECIPE_ID, -1);
        if (recipeId == -1) return;

        AppDatabase.databaseExecutor.execute(() -> {
            db.recipeDao().deleteRecipeById(recipeId);
            runOnUiThread(() -> {
                Toast.makeText(RecipeDetailActivity.this, "Recipe deleted!", Toast.LENGTH_SHORT).show();
                finish();
            });
        });
    }
}
