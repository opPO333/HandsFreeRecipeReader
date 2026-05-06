package app.recipe.reader.voice;

public interface VoiceCommandListener {
    void onNextStep();
    void onPreviousStep();
    void onRepeatStep();
    void onError(String message);
}
