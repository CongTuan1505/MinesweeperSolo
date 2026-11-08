package domin;

import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Original quiet ambient loop, played separately so game effects never restart the music. */
final class BackgroundMusic implements AutoCloseable {
    private final SoundEffects.Backend backend;
    private final ExecutorService worker = Executors.newSingleThreadExecutor(r -> {
        Thread thread = new Thread(r, "minesweeper-music");
        thread.setDaemon(true); return thread;
    });
    private final Object gate = new Object();
    private boolean requested, enabled, closed;
    private long generation;
    private Clip playing;
    private byte[] pcm;

    BackgroundMusic(SoundEffects.Backend backend) { this.backend = backend; }

    void start(boolean enabled) {
        synchronized (gate) {
            if (closed || (requested && this.enabled == enabled)) return;
            requested = true;
        }
        setEnabled(enabled);
    }

    void setEnabled(boolean value) {
        final Clip previous;
        final long token;
        final boolean play;
        synchronized (gate) {
            if (closed) return;
            enabled = value;
            token = ++generation;
            previous = playing;
            playing = null;
            play = requested && enabled;
        }
        release(previous);
        if (!play) return;
        try { worker.execute(() -> open(token)); }
        catch (RejectedExecutionException ignored) {}
    }

    private void open(long token) {
        Clip clip = null;
        try {
            synchronized (gate) { if (closed || !enabled || generation != token) return; }
            if (pcm == null) pcm = synthesize();
            clip = backend.open(pcm);
            synchronized (gate) {
                if (closed || !enabled || generation != token) { release(clip); return; }
                playing = clip;
                clip.loop(Clip.LOOP_CONTINUOUSLY);
            }
        } catch (Exception ignored) {
            synchronized (gate) { if (playing == clip) playing = null; }
            release(clip);
        }
    }

    private static void release(Clip clip) {
        if (clip == null) return;
        try { clip.stop(); } catch (RuntimeException ignored) {}
        try { clip.close(); } catch (RuntimeException ignored) {}
    }

    @Override public void close() {
        Clip previous;
        synchronized (gate) {
            if (closed) return;
            closed = true; generation++; previous = playing; playing = null;
        }
        release(previous);
        worker.shutdownNow();
    }

    /** Eight bars at 96 BPM: soft chords, plucked arpeggios and a restrained bass line. */
    static byte[] synthesize() {
        final int rate = (int)SoundEffects.FORMAT.getSampleRate();
        final double beat = 60.0 / 96, bar = beat * 4;
        double[] mix = new double[(int)(rate * bar * 8)];
        int[][] chords = {{48,55,59,64},{45,52,55,60},{41,48,52,57},{43,50,55,59}};
        int[] pattern = {0,2,1,3,2,1,3,2};
        for (int b=0;b<8;b++) {
            int[] chord = chords[b%chords.length];
            double start = b*bar;
            for (int note : chord) pad(mix,rate,start,bar,note,.012);
            for (int step=0;step<8;step++)
                pluck(mix,rate,start+step*beat/2,1.0,chord[pattern[step]]+24,.035);
            pluck(mix,rate,start,beat*2,chord[0]-12,.035);
            pluck(mix,rate,start+beat*2,beat*2,chord[0]-12,.025);
        }
        byte[] data = new byte[mix.length*2];
        for (int i=0;i<mix.length;i++) {
            short sample = (short)(Math.max(-.18, Math.min(.18,mix[i]))*Short.MAX_VALUE);
            data[2*i]=(byte)sample; data[2*i+1]=(byte)(sample>>8);
        }
        return data;
    }
    private static double frequency(int midi) { return 440*Math.pow(2,(midi-69)/12.0); }
    private static void pad(double[] mix,int rate,double start,double duration,int midi,double volume) {
        double hz=frequency(midi);
        int first=(int)(start*rate),length=(int)(duration*rate);
        for(int i=0;i<length;i++) {
            double t=i/(double)rate;
            double envelope=Math.sin(Math.PI*t/duration);
            double tone=Math.sin(2*Math.PI*hz*t)*.8+Math.sin(2*Math.PI*hz*1.002*t)*.2;
            mix[(first+i)%mix.length]+=tone*envelope*envelope*volume;
        }
    }
    private static void pluck(double[] mix,int rate,double start,double duration,int midi,double volume) {
        double hz=frequency(midi);
        int first=(int)(start*rate),length=(int)(duration*rate);
        for(int i=0;i<length;i++) {
            double t=i/(double)rate;
            double envelope=Math.min(1,t/.012)*Math.exp(-t*5)*Math.min(1,(duration-t)/.04);
            double tone=Math.sin(2*Math.PI*hz*t)*.9+Math.sin(4*Math.PI*hz*t)*.1;
            // Wrap ringing notes across the end of the loop for a continuous transition.
            mix[(first+i)%mix.length]+=tone*envelope*volume;
        }
    }
}
