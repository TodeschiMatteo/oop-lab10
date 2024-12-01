package it.unibo.mvc;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 */
public final class DrawNumberApp implements DrawNumberViewObserver {
    private static final String FILENAME = "config.yml";

    private final DrawNumber model;
    private final List<DrawNumberView> views;

    /**
     * @param views
     *            the views to attach
     */
    public DrawNumberApp(final DrawNumberView... views) {
        /*
         * Side-effect proof
         */
        this.views = Arrays.asList(Arrays.copyOf(views, views.length));
        for (final DrawNumberView view: views) {
            view.setObserver(this);
            view.start();
        }
        final Map<String, String> configMap = new HashMap<>();
        final Configuration.Builder confBuild = new Configuration.Builder();
        try (var info = new BufferedReader(new InputStreamReader(ClassLoader.getSystemResourceAsStream(FILENAME)))) {
            for (var line = info.readLine(); line != null; line = info.readLine()) {
                final String[] subLine = line.split(":");
                configMap.put(subLine[0].trim(), subLine[1].trim());
            }
            if (configMap.containsKey("maximum")) {
                confBuild.setMax(Integer.parseInt(configMap.get("maximum")));
            } else if (configMap.containsKey("minimum")) {
                confBuild.setMin(Integer.parseInt(configMap.get("minimum")));
            } else if (configMap.containsKey("attempts")) {
                confBuild.setAttempts(Integer.parseInt(configMap.get("attempts")));
            }
        } catch (IOException e) {
            showError(e.getMessage());
        }
        final Configuration configuration = confBuild.build();
        if (configuration.isConsistent()) {
            this.model = new DrawNumberImpl(configuration);
        } else {
            showError("Error in configuration");
            this.model = new DrawNumberImpl(new Configuration.Builder().build());
        }
    }

    private void showError(final String err) {
        for (final DrawNumberView view: views) {
            view.displayError(err);
        }
    }

    @Override
    public void newAttempt(final int n) {
        try {
            final DrawResult result = model.attempt(n);
            for (final DrawNumberView view: views) {
                view.result(result);
            }
        } catch (IllegalArgumentException e) {
            for (final DrawNumberView view: views) {
                view.numberIncorrect();
            }
        }
    }

    @Override
    public void resetGame() {
        this.model.reset();
    }

    @Override
    public void quit() {
        /*
         * A bit harsh. A good application should configure the graphics to exit by
         * natural termination when closing is hit. To do things more cleanly, attention
         * should be paid to alive threads, as the application would continue to persist
         * until the last thread terminates.
         */
        System.exit(0);
    }

    /**
     * @param args
     *            ignored
     * @throws FileNotFoundException 
     */
    public static void main(final String... args) throws FileNotFoundException {
        new DrawNumberApp(
            new DrawNumberViewImpl(),
            new DrawNumberViewImpl(),
            new PrintStreamView(System.out),
            new PrintStreamView("logger.txt"));
    }

}
