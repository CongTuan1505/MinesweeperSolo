package domin;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.*;
import javax.sound.sampled.*;

/** Exercises audio lifecycle with a fake mixer, without changing user settings or playing sound. */
public class TestSoundEffects {
    static int checks;
    static void check(boolean value, String message) {
        if (!value) throw new AssertionError(message);
        checks++;
    }
    static class FakeClip implements InvocationHandler {
        int starts, stops, closes, loops, loopCount;
        final List<LineListener> listeners = new ArrayList<>();
        final Clip clip = (Clip) Proxy.newProxyInstance(Clip.class.getClassLoader(),
                new Class<?>[]{Clip.class}, this);
        public Object invoke(Object proxy, Method method, Object[] args) {
            switch (method.getName()) {
                case "start": starts++; return null;
                case "loop": loops++; loopCount=(Integer)args[0]; return null;
                case "stop": stops++; return null;
                case "close": closes++; return null;
                case "addLineListener": listeners.add((LineListener)args[0]); return null;
                case "hashCode": return System.identityHashCode(proxy);
                case "equals": return proxy == args[0];
                case "toString": return "FakeClip";
                default:
                    if (method.getReturnType() == boolean.class) return false;
                    if (method.getReturnType() == int.class) return 0;
                    if (method.getReturnType() == long.class) return 0L;
                    if (method.getReturnType() == float.class) return 0f;
                    return null;
            }
        }
        void finish() {
            for (LineListener listener : listeners) listener.update(new LineEvent(clip, LineEvent.Type.STOP, 0));
        }
    }
    static void idle(SoundEffects sounds) throws Exception {
        Field field = SoundEffects.class.getDeclaredField("worker");
        field.setAccessible(true);
        ((ThreadPoolExecutor)field.get(sounds)).submit(() -> {}).get(3, TimeUnit.SECONDS);
    }
    public static void main(String[] args) throws Exception {
        for (SoundEffects.Cue cue : SoundEffects.Cue.values()) {
            byte[] pcm = SoundEffects.synthesize(cue);
            check(pcm.length > 1000 && pcm.length < 88200 && pcm.length % 2 == 0, "Short valid PCM: " + cue);
            int peak = 0;
            for (int i=0;i<pcm.length;i+=2) {
                int sample = (short)((pcm[i]&255) | (pcm[i+1]<<8));
                peak = Math.max(peak, Math.abs(sample));
            }
            check(peak > 500 && peak < 8000, "Audible restrained gain without clipping: " + cue);
            check(pcm[0] == 0 && pcm[1] == 0, "Smooth attack: " + cue);
        }
        List<FakeClip> clips = new ArrayList<>();
        AtomicReference<Boolean> saved = new AtomicReference<>();
        try (SoundEffects sounds = new SoundEffects(pcm -> {
            FakeClip clip = new FakeClip(); clips.add(clip); return clip.clip;
        }, true, saved::set)) {
            sounds.play(SoundEffects.Cue.START); idle(sounds);
            check(clips.size()==1 && clips.get(0).starts==1, "Start plays off-thread");
            clips.get(0).finish();
            check(clips.get(0).closes==1, "Finished clip releases mixer");
            sounds.play(SoundEffects.Cue.CHALLENGE); idle(sounds);
            sounds.setEnabled(false); idle(sounds);
            check(!sounds.isEnabled() && Boolean.FALSE.equals(saved.get()), "Mute preference saved");
            check(clips.get(1).stops==1 && clips.get(1).closes==1, "Mute stops active audio immediately");
            sounds.play(SoundEffects.Cue.WIN); idle(sounds);
            check(clips.size()==2, "Muted cues never reach mixer");
            sounds.setEnabled(true); sounds.play(SoundEffects.Cue.WIN); idle(sounds);
            check(clips.size()==3 && Boolean.TRUE.equals(saved.get()), "Unmute resumes playback");
        }
        check(clips.get(2).closes==1, "Shutdown closes active audio");

        CountDownLatch opening = new CountDownLatch(1), release = new CountDownLatch(1);
        FakeClip pending = new FakeClip();
        try (SoundEffects sounds = new SoundEffects(pcm -> {
            opening.countDown(); release.await(3, TimeUnit.SECONDS); return pending.clip;
        }, true, value -> {})) {
            sounds.play(SoundEffects.Cue.CHALLENGE);
            check(opening.await(3, TimeUnit.SECONDS), "Mixer open started");
            sounds.play(SoundEffects.Cue.FLAG); // queued while mixer is busy
            sounds.setEnabled(false);
            sounds.setEnabled(true);
            release.countDown(); idle(sounds);
            check(pending.starts==0 && pending.closes==1, "Mute invalidates in-flight and queued sounds across unmute");
        }
        AtomicInteger attempts = new AtomicInteger();
        FakeClip recovered = new FakeClip();
        try (SoundEffects sounds = new SoundEffects(pcm -> {
            if (attempts.incrementAndGet()==1) throw new LineUnavailableException("No output device");
            return recovered.clip;
        }, true, value -> {})) {
            sounds.play(SoundEffects.Cue.CHALLENGE); idle(sounds);
            sounds.play(SoundEffects.Cue.CHALLENGE); idle(sounds);
            check(recovered.starts==1, "Unavailable device does not kill audio worker");
        }
        clips.clear();
        try (SoundEffects sounds = new SoundEffects(pcm -> {
            FakeClip clip = new FakeClip(); clips.add(clip); return clip.clip;
        }, true, value -> {})) {
            for(int i=0;i<6;i++) { sounds.play(SoundEffects.Cue.CHALLENGE); idle(sounds); }
            check(clips.size()==4, "Polyphony is bounded");
            sounds.play(SoundEffects.Cue.LOSE); idle(sounds);
            check(clips.size()==5 && clips.get(4).starts==1, "Result replaces old cues");
            for(int i=0;i<4;i++) check(clips.get(i).closes==1, "Result closes old clip");
        }
        System.out.println("PASS: " + checks + " audio generation and lifecycle checks.");
    }
}
