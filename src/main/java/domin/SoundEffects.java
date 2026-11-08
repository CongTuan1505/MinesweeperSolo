package domin;

import java.util.*;
import java.util.concurrent.*;
import java.util.function.Consumer;
import java.util.prefs.Preferences;
import javax.sound.sampled.*;

/** Short, locally synthesized effects. Audio work never blocks the Swing event thread. */
public final class SoundEffects implements AutoCloseable {
    public enum Cue { CLICK, OPEN, REGION, FLAG, UNFLAG, START, CHALLENGE, WIN, LOSE, DRAW, MINE_LOSS, WARNING }
    interface Backend { Clip open(byte[] pcm) throws Exception; }
    static final AudioFormat FORMAT = new AudioFormat(22050, 16, 1, true, false);
    private static final class Shared {
        static final SoundEffects INSTANCE = createDefault();
    }
    public static SoundEffects shared() { return Shared.INSTANCE; }
    private final Backend backend;
    private final BackgroundMusic music;
    private final Consumer<Boolean> save;
    private final ThreadPoolExecutor worker;
    private final Map<Cue, byte[]> cache = new EnumMap<>(Cue.class);
    private final Set<Clip> active = ConcurrentHashMap.newKeySet();
    private final Object gate = new Object();
    private volatile boolean enabled;
    private boolean closed;
    private long generation;
    private long lastTap;
    private long lastClick;

    SoundEffects(Backend backend, boolean enabled, Consumer<Boolean> save) {
        this.backend = backend; this.enabled = enabled; this.save = save;
        this.music = new BackgroundMusic(backend);
        worker = new ThreadPoolExecutor(1, 1, 0, TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(8), runnable -> {
                    Thread thread = new Thread(runnable, "minesweeper-audio");
                    thread.setDaemon(true); return thread;
                }, new ThreadPoolExecutor.DiscardPolicy());
    }

    private static SoundEffects createDefault() {
        Preferences prefs = null;
        boolean enabled = true;
        try {
            prefs = Preferences.userNodeForPackage(SoundEffects.class);
            enabled = prefs.getBoolean("soundEnabled", true);
        } catch (RuntimeException ignored) {}
        final Preferences settings = prefs;
        return new SoundEffects(pcm -> {
            Clip clip = AudioSystem.getClip();
            try { clip.open(FORMAT, pcm, 0, pcm.length); return clip; }
            catch (Exception e) { clip.close(); throw e; }
        }, enabled, value -> {
            if (settings != null) try { settings.putBoolean("soundEnabled", value); }
            catch (RuntimeException ignored) {}
        });
    }
    public boolean isEnabled() { return enabled; }
    public void startMusic() {
        synchronized (gate) { if (closed) return; music.start(enabled); }
    }

    public void setEnabled(boolean value) {
        synchronized (gate) { if (closed) return; enabled = value; }
        if (!value) stop();
        music.setEnabled(value);
        save.accept(value);
    }

    /** Result/start cues replace unfinished effects; ordinary taps are rate limited. */
    public void play(Cue cue) {
        if (cue == Cue.START || cue == Cue.WIN || cue == Cue.LOSE || cue == Cue.DRAW || cue == Cue.MINE_LOSS)
            stop();
        final long token;
        synchronized (gate) {
            if (!enabled || closed) return;
            long now = System.nanoTime();
            if (cue == Cue.CLICK) {
                if (now - lastClick < 45_000_000L) return;
                lastClick = now;
            }
            if ((cue == Cue.OPEN || cue == Cue.REGION || cue == Cue.FLAG || cue == Cue.UNFLAG)
                    && now - lastTap < 45_000_000L) return;
            if (cue == Cue.OPEN || cue == Cue.REGION || cue == Cue.FLAG || cue == Cue.UNFLAG) lastTap = now;
            token = generation;
        }
        worker.execute(() -> {
            Clip clip = null;
            try {
                synchronized (gate) {
                    if (!enabled || closed || token != generation || active.size() >= 4) return;
                }
                byte[] pcm = cache.computeIfAbsent(cue, SoundEffects::synthesize);
                clip = backend.open(pcm);
                final Clip playing = clip;
                clip.addLineListener(event -> {
                    if (event.getType() == LineEvent.Type.STOP) {
                        active.remove(playing);
                        playing.close();
                    }
                });
                synchronized (gate) {
                    if (!enabled || closed || token != generation) { clip.close(); return; }
                    active.add(clip);
                    clip.start();
                }
            } catch (Exception ignored) {
                // Missing mixer, disconnected output or unsupported audio must never interrupt play.
                if (clip != null) { active.remove(clip); clip.close(); }
            }
        });
    }

    public void stop() {
        List<Clip> clips;
        synchronized (gate) {
            generation++;
            worker.getQueue().clear();
            clips = new ArrayList<>(active);
            active.clear();
        }
        for (Clip clip : clips) {
            try { clip.stop(); } catch (RuntimeException ignored) {}
            try { clip.close(); } catch (RuntimeException ignored) {}
        }
    }

    @Override public void close() {
        synchronized (gate) { closed = true; }
        stop(); music.close(); worker.shutdownNow();
    }

    /** 16-bit PCM with smooth attack/release, restrained gain, and deterministic noise. */
    static byte[] synthesize(Cue cue) {
        double[] notes;
        double beat;
        switch (cue) {
            case CLICK: notes = new double[]{1050}; beat = .045; break;
            case OPEN: notes = new double[]{820}; beat = .065; break;
            case REGION: notes = new double[]{660, 990}; beat = .065; break;
            case FLAG: notes = new double[]{740, 1110}; beat = .055; break;
            case UNFLAG: notes = new double[]{560}; beat = .07; break;
            case START: notes = new double[]{523.25, 659.25, 783.99, 1046.5}; beat = .105; break;
            case CHALLENGE: notes = new double[]{880, 1174.66, 0, 880, 1174.66}; beat = .09; break;
            case WIN: notes = new double[]{523.25, 659.25, 783.99, 0, 1046.5, 1046.5}; beat = .115; break;
            case LOSE: notes = new double[]{392, 329.63, 261.63}; beat = .16; break;
            case DRAW: notes = new double[]{440, 523.25, 440}; beat = .14; break;
            case MINE_LOSS: notes = new double[]{-1, 0, 392, 329.63, 261.63}; beat = .14; break;
            default: notes = new double[]{880}; beat = .08;
        }
        int perNote = (int)(FORMAT.getSampleRate() * beat);
        byte[] pcm = new byte[perNote * notes.length * 2];
        Random noise = new Random(42);
        double filtered = 0;
        for (int n=0;n<notes.length;n++) {
            double frequency = notes[n];
            for (int i=0;i<perNote;i++) {
                double t = i / FORMAT.getSampleRate();
                double envelope = Math.min(1, t/.006) * Math.min(1, (beat-t)/.025) * Math.exp(-t*3);
                double sample = 0;
                if (frequency < 0) {
                    filtered = filtered*.65 + (noise.nextDouble()*2-1)*.35;
                    sample = (filtered*.75 + Math.sin(2*Math.PI*65*t)*.25)*Math.exp(-t*18);
                } else if (frequency > 0) {
                    sample = Math.sin(2*Math.PI*frequency*t)*.82 + Math.sin(4*Math.PI*frequency*t)*.18;
                }
                short value = (short)(sample * envelope * (cue == Cue.CLICK ? 0.12 : 0.22) * Short.MAX_VALUE);
                int offset = (n*perNote+i)*2;
                pcm[offset] = (byte)value; pcm[offset+1] = (byte)(value >> 8);
            }
        }
        return pcm;
    }
}
