package examples.mixer;

/**
 *  Mix snapshots the engineer can recall from the Session menu.
 *  <p>
 *  A recalled snapshot is the application setting values, so it is where the difference between
 *  what the user may pick and what the application may set shows. Many numbers below sit between
 *  two tick marks of a snapping slider on purpose: a fader at -7.3 dB on a scale that snaps to
 *  whole decibels, a pan at 0.35 on a scale that snaps to tenths, a send at 127 beyond the last
 *  tick mark at 120. The knobs must sit exactly there, clicking a knob without moving it must
 *  keep the number, and an arrow key must never move a knob against its direction.
 */
public enum SessionPreset
{
    PODCAST("Podcast, two voices") {
        @Override MixerViewModel applyTo( MixerViewModel m ) {
            return m
                .withStripAt(0, m.channel(0).withFaderDb(-7.3).withTrimDb(3).withPan(-0.35f).withSend((byte) 12)
                                              .withLowGainDb(-4.5).withMidFrequencyHz(2350).withMidGainDb(2.5).withHighGainDb(1.5)
                                              .withMuted(false).withSoloed(false))
                .withStripAt(1, m.channel(1).withFaderDb(Channel.SILENCE_DB).withMuted(true).withSoloed(false))
                .withStripAt(2, m.channel(2).withFaderDb(-7.3).withTrimDb(3).withPan(0.35f).withSend((byte) 12)
                                              .withLowGainDb(-4.5).withMidFrequencyHz(2350).withMidGainDb(2.5).withHighGainDb(1.5)
                                              .withMuted(false).withSoloed(false))
                .withStripAt(3, m.channel(3).withFaderDb(Channel.SILENCE_DB).withMuted(true).withSoloed(false))
                .withMasterDb(-2.0).withLimiterCeilingDb(-1.25).withStereoWidth(0.6f)
                .withMonitorLimitAt(80).withMonitorVolume(55);
        }
    },
    BAND_REHEARSAL("Band rehearsal, everything loud") {
        @Override MixerViewModel applyTo( MixerViewModel m ) {
            return m
                .withStripAt(0, m.channel(0).withFaderDb(-3.5).withTrimDb(-4).withPan(0.0f).withSend((byte) 127).withMuted(false).withSoloed(false))
                .withStripAt(1, m.channel(1).withFaderDb(-8.25).withTrimDb(2).withPan(-0.55f).withSend((byte) 127).withMuted(false).withSoloed(false))
                .withStripAt(2, m.channel(2).withFaderDb(-11.6).withTrimDb(0).withPan(0.45f).withSend((byte) 98).withMuted(false).withSoloed(false))
                .withStripAt(3, m.channel(3).withFaderDb(-1.8).withTrimDb(5).withPan(0.05f).withSend((byte) 127).withMuted(false).withSoloed(false))
                .withMasterDb(0.0).withLimiterCeilingDb(-0.3).withStereoWidth(1.35f)
                .withMonitorLimitAt(100).withMonitorVolume(93);
        }
    },
    LATE_NIGHT("Late-night mix, gentle on the ears") {
        @Override MixerViewModel applyTo( MixerViewModel m ) {
            return m
                .withStripAt(0, m.channel(0).withFaderDb(-14.5).withPan(-0.12f).withSend((byte) 51).withHighGainDb(-3.5).withMuted(false))
                .withStripAt(1, m.channel(1).withFaderDb(-19.4).withPan(-0.72f).withSend((byte) 33).withMuted(false))
                .withStripAt(2, m.channel(2).withFaderDb(-17.0).withPan(0.72f).withSend((byte) 77).withMuted(false))
                .withStripAt(3, m.channel(3).withFaderDb(-24.7).withPan(0.0f).withSend((byte) 9).withLowGainDb(-6.5).withMuted(false))
                .withMasterDb(-9.5).withLimiterCeilingDb(-2.35).withStereoWidth(0.85f)
                .withMonitorLimitAt(65).withMonitorVolume(38);
        }
    },
    FACTORY("Factory defaults") {
        @Override MixerViewModel applyTo( MixerViewModel m ) {
            MixerViewModel fresh = MixerViewModel.initial();
            return fresh
                .withTransport(m.transport())
                .withLanguage(m.language())
                .withScalesVisible(m.scalesVisible())
                .withFaderSnap(m.faderSnap())
                .withWheelStyle(m.wheelStyle())
                .withEngineUnderLoad(m.engineUnderLoad())
                .withMidiLog(m.midiLog());
        }
    };

    private final String title;

    SessionPreset( String title ) { this.title = title; }

    abstract MixerViewModel applyTo( MixerViewModel m );

    @Override
    public String toString() { return title; }
}
