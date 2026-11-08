package domin;

import java.lang.reflect.*;
import java.util.*;
import java.util.concurrent.*;
import javax.sound.sampled.*;

public class TestBackgroundMusic {
    static int checks;
    static void check(boolean ok,String message) { if(!ok)throw new AssertionError(message);checks++; }
    static Object field(Object o,String key)throws Exception {
        Field f=o.getClass().getDeclaredField(key);f.setAccessible(true);return f.get(o);
    }
    static void idle(BackgroundMusic music)throws Exception {
        ((ExecutorService)field(music,"worker")).submit(() -> {}).get(5,TimeUnit.SECONDS);
    }
    public static void main(String[] args)throws Exception {
        byte[] pcm=BackgroundMusic.synthesize();
        check(pcm.length==(int)SoundEffects.FORMAT.getSampleRate()*20*2,"Twenty-second PCM music loop");
        int peak=0;double square=0;
        for(int i=0;i<pcm.length;i+=2) {
            int sample=(short)((pcm[i]&255)|(pcm[i+1]<<8));
            peak=Math.max(peak,Math.abs(sample));square+=(double)sample*sample;
        }
        double rms=Math.sqrt(square/(pcm.length/2));
        check(peak>500 && peak<6000,"Quiet music with headroom");
        check(rms>50 && rms<1500,"Background gain remains restrained");
        int first=(short)((pcm[0]&255)|(pcm[1]<<8));
        int last=(short)((pcm[pcm.length-2]&255)|(pcm[pcm.length-1]<<8));
        check(Math.abs(last-first)<500,"Loop boundary has no abrupt amplitude jump");

        List<TestSoundEffects.FakeClip> clips=Collections.synchronizedList(new ArrayList<>());
        try(SoundEffects sounds=new SoundEffects(data -> {
            TestSoundEffects.FakeClip c=new TestSoundEffects.FakeClip();clips.add(c);return c.clip;
        },true,value -> {})) {
            BackgroundMusic music=(BackgroundMusic)field(sounds,"music");
            sounds.startMusic();idle(music);
            check(clips.size()==1 && clips.get(0).loops==1 &&
                    clips.get(0).loopCount==Clip.LOOP_CONTINUOUSLY,"Background loops continuously");
            sounds.startMusic();idle(music);
            check(clips.size()==1,"Repeated startup never duplicates music");
            sounds.play(SoundEffects.Cue.WIN);TestSoundEffects.idle(sounds);
            check(clips.get(0).closes==0 && clips.get(0).loops==1,"Result effects do not interrupt or restart music");
            sounds.setEnabled(false);idle(music);
            check(clips.get(0).closes==1 && clips.get(1).closes==1,"Mute stops music and effects");
            sounds.startMusic();idle(music);
            check(clips.size()==2,"Music stays silent when disabled");
            sounds.setEnabled(true);idle(music);
            check(clips.size()==3 && clips.get(2).loops==1,"Unmute starts one music instance");
        }
        check(clips.get(2).closes==1,"Shutdown releases music output");

        TestSoundEffects.FakeClip pending=new TestSoundEffects.FakeClip();
        CountDownLatch opening=new CountDownLatch(1),release=new CountDownLatch(1);
        try(BackgroundMusic music=new BackgroundMusic(data -> {
            opening.countDown();release.await(5,TimeUnit.SECONDS);return pending.clip;
        })) {
            music.start(true);
            check(opening.await(5,TimeUnit.SECONDS),"Music opening occurs off-thread");
            music.setEnabled(false);release.countDown();idle(music);
            check(pending.loops==0 && pending.closes==1,"Muting during mixer open cancels stale music");
        }
        java.util.concurrent.atomic.AtomicInteger attempts=new java.util.concurrent.atomic.AtomicInteger();
        TestSoundEffects.FakeClip recovered=new TestSoundEffects.FakeClip();
        try(BackgroundMusic music=new BackgroundMusic(data -> {
            if(attempts.incrementAndGet()==1)throw new LineUnavailableException("No audio output");
            return recovered.clip;
        })) {
            music.start(true);idle(music);
            music.setEnabled(false);music.setEnabled(true);idle(music);
            check(recovered.loops==1,"Unavailable mixer fails safely and can recover");
        }
        System.out.println("PASS: "+checks+" background music checks.");
    }
}
