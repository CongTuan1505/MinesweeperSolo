package domin;

import java.util.concurrent.*;
import javax.sound.sampled.*;

/** Original restrained electronic puzzle-game loop, played separately so game effects never restart the music. */
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

    /** Sixteen bars at 140 BPM: energetic arcade lead, octave bass and four-on-the-floor drums. */
    static byte[] synthesize() {
        final int rate = (int)SoundEffects.FORMAT.getSampleRate();
        final double beat = 60.0 / 140, bar = beat * 4;
        double[] mix = new double[(int)(rate * bar * 16)];
        int[][] chords = {{50,57,60,64},{46,53,57,60},{43,50,53,57},{45,52,57,62}};
        int[][] motifs = {
            {74,77,81,77,76,74,69,72},
            {70,74,77,74,72,69,65,69},
            {67,70,74,77,74,72,70,69},
            {69,73,76,81,79,76,73,71}
        };
        java.util.Random noise = new java.util.Random(140);
        for (int b=0;b<16;b++) {
            int section = b % 4;
            int[] chord = chords[section];
            double start = b*bar;
            for (int note : chord) pad(mix,rate,start,bar,note,.006);
            for (int step=0;step<8;step++) {
                double at = start + step*beat/2;
                int note = motifs[section][step];
                // The second half answers the motif without a tiring high-register loop.
                if (note > 0) synth(mix,rate,at,beat*.44,note + (b>=8 && step==6 ? 12 : 0),.038,false);
                synth(mix,rate,at,beat*.38,chord[0]-(step%2==0?12:0),.038,true);
                percussion(mix,rate,at,step%2==0 ? .006 : .009,2,noise);
                if (b%4==3 && step>=6)
                    percussion(mix,rate,at+beat/4,.006,2,noise);
            }
            for (int kick=0;kick<4;kick++)
                percussion(mix,rate,start+beat*kick,.052,0,noise);
            percussion(mix,rate,start+beat,.023,1,noise);
            percussion(mix,rate,start+beat*3,.023,1,noise);
            if (b%4==3) {
                percussion(mix,rate,start+beat*3.5,.015,1,noise);
                percussion(mix,rate,start+beat*3.75,.012,1,noise);
            }
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
            mix[(first+i)%mix.length]+=Math.sin(2*Math.PI*hz*t)*envelope*envelope*volume;
        }
    }
    private static void synth(double[] mix,int rate,double start,double duration,int midi,double volume,boolean bass) {
        double hz=frequency(midi);
        int first=(int)(start*rate),length=(int)(duration*rate);
        for(int i=0;i<length;i++) {
            double t=i/(double)rate,phase=2*Math.PI*hz*t;
            double envelope=Math.min(1,t/.008)*Math.exp(-t*(bass?5:7))*Math.min(1,(duration-t)/.035);
            // A few harmonics suggest classic game synths without harsh square-wave edges.
            double tone = bass ? Math.sin(phase)*.85+Math.sin(2*phase)*.15
                    : Math.sin(phase)*.64+Math.sin(2*phase)*.16+Math.sin(3*phase)*.14+Math.sin(5*phase)*.06;
            mix[(first+i)%mix.length]+=tone*envelope*volume;
        }
    }
    private static void percussion(double[] mix,int rate,double start,double volume,int kind,java.util.Random noise) {
        double duration=kind==0?.16:kind==1?.11:.045;
        int first=(int)(start*rate),length=(int)(duration*rate);
        double previous=0;
        for(int i=0;i<length;i++) {
            double t=i/(double)rate;
            double envelope=Math.min(1,t/.003)*Math.exp(-t*(kind==0?24:kind==1?35:75))
                    *Math.min(1,(duration-t)/.012);
            double tone;
            if (kind==0) {
                double phase=2*Math.PI*(45*t+65*(1-Math.exp(-35*t))/35);
                tone=Math.sin(phase);
            } else {
                double sample=noise.nextDouble()*2-1;
                tone=kind==2 ? (sample-previous)*.5 : sample*.7+Math.sin(2*Math.PI*180*t)*.3;
                previous=sample;
            }
            mix[(first+i)%mix.length]+=tone*envelope*volume;
        }
    }
}
