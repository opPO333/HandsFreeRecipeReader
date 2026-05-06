package app.recipe.reader.voice;

import android.content.Context;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import java.util.Locale;

public class TTSManager implements TextToSpeech.OnInitListener {

    private TextToSpeech tts;
    private boolean isInitialized = false;
    private TTSCallback callback;

    public interface TTSCallback {
        void onTtsInitialized();
        void onTtsError(String error);
        void onSpeakingStart();
        void onSpeakingDone();
    }

    public TTSManager(Context context, TTSCallback callback) {
        this.callback = callback;
        this.tts = new TextToSpeech(context, this);
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            int result = tts.setLanguage(Locale.US);
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                if (callback != null) callback.onTtsError("TTS language not supported!");
            } else {
                isInitialized = true;
                tts.setOnUtteranceProgressListener(new UtteranceProgressListener() {
                    @Override
                    public void onStart(String utteranceId) {
                        if (callback != null) callback.onSpeakingStart();
                    }

                    @Override
                    public void onDone(String utteranceId) {
                        if (callback != null) callback.onSpeakingDone();
                    }

                    @Override
                    public void onError(String utteranceId) {
                        if (callback != null) callback.onSpeakingDone();
                    }
                });
                if (callback != null) callback.onTtsInitialized();
            }
        } else {
            if (callback != null) callback.onTtsError("TTS initialization failed!");
        }
    }

    public void speak(String text, String utteranceId) {
        if (!isInitialized || tts == null) return;
        Bundle params = new Bundle();
        params.putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f);
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, utteranceId);
    }

    public void shutdown() {
        if (tts != null) {
            tts.stop();
            tts.shutdown();
        }
    }
}
