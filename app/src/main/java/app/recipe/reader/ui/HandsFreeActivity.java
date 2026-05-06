package app.recipe.reader.ui;

import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import java.util.ArrayList;
import java.util.List;

import app.recipe.reader.R;
import app.recipe.reader.data.AppDatabase;
import app.recipe.reader.data.RecipeStep;
import app.recipe.reader.voice.TTSManager;
import app.recipe.reader.voice.VoiceAssistantManager;
import app.recipe.reader.voice.VoiceCommandListener;

public class HandsFreeActivity extends AppCompatActivity implements VoiceCommandListener, TTSManager.TTSCallback {

    private static final int PERMISSIONS_REQUEST_RECORD_AUDIO = 1;
    private static final String STATE_STEP_INDEX = "state_step_index";

    private AppDatabase db;
    private TTSManager ttsManager;
    private VoiceAssistantManager voiceAssistantManager;

    private TextView tvRecipeTitle;
    private TextView tvStepIndicator;
    private TextView tvStepDescription;
    private ImageButton btnPrev;
    private ImageButton btnNext;

    private List<RecipeStep> steps = new ArrayList<>();
    private int currentStepIndex = 0;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hands_free);

        if (savedInstanceState != null) {
            currentStepIndex = savedInstanceState.getInt(STATE_STEP_INDEX, 0);
        }

        db = AppDatabase.getInstance(this);

        tvRecipeTitle = findViewById(R.id.tv_recipe_title);
        tvStepIndicator = findViewById(R.id.tv_step_indicator);
        tvStepDescription = findViewById(R.id.tv_step_description);
        btnPrev = findViewById(R.id.btn_prev);
        btnNext = findViewById(R.id.btn_next);

        setupToolbar();
        setupButtons();

        String title = getIntent().getStringExtra(RecipeDetailActivity.EXTRA_RECIPE_TITLE);
        if (title != null) {
            tvRecipeTitle.setText(title);
        }

        ttsManager = new TTSManager(this, this);
        voiceAssistantManager = new VoiceAssistantManager(this, this);

        int permissionCheck = ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.RECORD_AUDIO}, PERMISSIONS_REQUEST_RECORD_AUDIO);
        } else {
            voiceAssistantManager.initModel();
        }

        loadSteps();
    }

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupButtons() {
        btnPrev.setOnClickListener(v -> onPreviousStep());
        btnNext.setOnClickListener(v -> onNextStep());
    }

    private void loadSteps() {
        int recipeId = getIntent().getIntExtra(RecipeDetailActivity.EXTRA_RECIPE_ID, -1);
        if (recipeId == -1) return;

        AppDatabase.databaseExecutor.execute(() -> {
            steps = db.recipeDao().getStepsForRecipe(recipeId);
            runOnUiThread(() -> {
                if (!steps.isEmpty()) {
                    if (currentStepIndex >= steps.size()) {
                        currentStepIndex = 0;
                    }
                    updateUI();
                    speakCurrentStep();
                } else {
                    tvStepDescription.setText("No steps found for this recipe!");
                }
            });
        });
    }

    private void updateUI() {
        if (steps.isEmpty()) return;

        RecipeStep currentStep = steps.get(currentStepIndex);
        tvStepIndicator.setText("Step " + (currentStepIndex + 1) + " of " + steps.size());
        tvStepDescription.setText(currentStep.getStepDescription());

        btnPrev.setEnabled(currentStepIndex > 0);
        btnNext.setEnabled(currentStepIndex < steps.size() - 1);
        
        btnPrev.setAlpha(currentStepIndex > 0 ? 1.0f : 0.3f);
        btnNext.setAlpha(currentStepIndex < steps.size() - 1 ? 1.0f : 0.3f);
    }

    private void speakCurrentStep() {
        if (!steps.isEmpty()) {
            String text = steps.get(currentStepIndex).getStepDescription();
            ttsManager.speak(text, "STEP_" + currentStepIndex);
        }
    }

    @Override
    public void onNextStep() {
        runOnUiThread(() -> {
            if (currentStepIndex < steps.size() - 1) {
                currentStepIndex++;
                updateUI();
                speakCurrentStep();
            }
        });
    }

    @Override
    public void onPreviousStep() {
        runOnUiThread(() -> {
            if (currentStepIndex > 0) {
                currentStepIndex--;
                updateUI();
                speakCurrentStep();
            }
        });
    }

    @Override
    public void onRepeatStep() {
        runOnUiThread(this::speakCurrentStep);
    }

    @Override
    public void onError(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_LONG).show());
    }

    @Override
    public void onTtsInitialized() {
        runOnUiThread(() -> {
            if (!steps.isEmpty()) {
                speakCurrentStep();
            }
        });
    }

    @Override
    public void onTtsError(String error) {
        runOnUiThread(() -> Toast.makeText(this, error, Toast.LENGTH_SHORT).show());
    }

    @Override
    public void onSpeakingStart() {
        if (voiceAssistantManager != null) {
            voiceAssistantManager.setSpeakingState(true);
        }
    }

    @Override
    public void onSpeakingDone() {
        if (voiceAssistantManager != null) {
            voiceAssistantManager.setSpeakingState(false);
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_RECORD_AUDIO) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                voiceAssistantManager.initModel();
            } else {
                Toast.makeText(this, "Microphone permission is required for voice commands!", Toast.LENGTH_LONG).show();
            }
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(STATE_STEP_INDEX, currentStepIndex);
    }

    @Override
    protected void onDestroy() {
        if (ttsManager != null) {
            ttsManager.shutdown();
        }
        if (voiceAssistantManager != null) {
            voiceAssistantManager.shutdown();
        }
        super.onDestroy();
    }
}
