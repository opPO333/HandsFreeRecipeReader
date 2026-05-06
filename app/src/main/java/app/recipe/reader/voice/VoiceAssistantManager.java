package app.recipe.reader.voice;

import android.content.Context;

import org.json.JSONException;
import org.json.JSONObject;
import org.vosk.Model;
import org.vosk.Recognizer;
import org.vosk.android.RecognitionListener;
import org.vosk.android.SpeechService;
import org.vosk.android.StorageService;

public class VoiceAssistantManager {

    private Model model;
    private SpeechService speechService;
    private VoiceCommandListener commandListener;
    private Context context;

    private boolean isSpeaking = false;
    private long lastCommandTime = 0;

    public VoiceAssistantManager(Context context, VoiceCommandListener listener) {
        this.context = context;
        this.commandListener = listener;
    }

    public void initModel() {
        StorageService.unpack(context, "model", "model",
                (model) -> {
                    this.model = model;
                    startRecognition();
                },
                (exception) -> {
                    if (commandListener != null) {
                        commandListener.onError("Failed to unpack model: " + exception.getMessage());
                    }
                });
    }

    private void startRecognition() {
        if (speechService != null) {
            speechService.stop();
        }
        try {
            Recognizer rec = new Recognizer(model, 16000.0f);
            speechService = new SpeechService(rec, 16000.0f);
            speechService.startListening(new RecognitionListener() {
                @Override
                public void onPartialResult(String hypothesis) {
                    handleVoiceCommand(hypothesis);
                }

                @Override
                public void onResult(String hypothesis) {
                    handleVoiceCommand(hypothesis);
                }

                @Override
                public void onFinalResult(String hypothesis) {}

                @Override
                public void onError(Exception e) {}

                @Override
                public void onTimeout() {}
            });
        } catch (Exception e) {
            if (commandListener != null) {
                commandListener.onError("SpeechService error: " + e.getMessage());
            }
        }
    }

    private void handleVoiceCommand(String jsonStr) {
        try {
            JSONObject obj = new JSONObject(jsonStr);
            String text = "";
            if (obj.has("text")) {
                text = obj.getString("text");
            } else if (obj.has("partial")) {
                text = obj.getString("partial");
            }

            if (isSpeaking || text.isEmpty()) return;

            text = text.toLowerCase();

            long now = System.currentTimeMillis();
            if (now - lastCommandTime < 1500) return;

            boolean commandFound = false;
            if (text.contains("next") || text.contains("forward")) {
                if (commandListener != null) commandListener.onNextStep();
                commandFound = true;
            } else if (text.contains("previous") || text.contains("back")) {
                if (commandListener != null) commandListener.onPreviousStep();
                commandFound = true;
            } else if (text.contains("repeat") || text.contains("again")) {
                if (commandListener != null) commandListener.onRepeatStep();
                commandFound = true;
            }

            if (commandFound) {
                lastCommandTime = now;
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    public void setSpeakingState(boolean speaking) {
        this.isSpeaking = speaking;
        if (!speaking) {
            this.lastCommandTime = System.currentTimeMillis();
        }
        if (speechService != null) {
            speechService.setPause(speaking);
        }
    }

    public void shutdown() {
        if (speechService != null) {
            speechService.stop();
            speechService.shutdown();
        }
    }
}
