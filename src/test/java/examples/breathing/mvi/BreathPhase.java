package examples.breathing.mvi;

import swingtree.animation.Progress;

/**
 *  The four stages of a single breathing cycle. <br>
 *  Each phase knows three things that the animation needs:
 *  <ul>
 *      <li>a short {@link #instruction()} shown to the user,</li>
 *      <li>how the orb should be {@link #scaleAt(Progress) scaled} while the
 *          phase progresses (an eased {@code 0..1} value),</li>
 *      <li>and the orb scale it should rest on once the phase {@link #endScale() ends}.</li>
 *  </ul>
 *  This enum carries no Swing or styling code — it is a pure, value-based
 *  part of the {@link BreathingViewModel}.
 */
public enum BreathPhase {
    INHALE  ("Breathe in"),
    HOLD_IN ("Hold it"),
    EXHALE  ("Breathe out"),
    HOLD_OUT("Rest");

    private final String instruction;

    BreathPhase( String instruction ) { this.instruction = instruction; }

    public String instruction() { return instruction; }

    /** The phase that naturally follows this one, wrapping around to {@link #INHALE}. */
    public BreathPhase next() {
        BreathPhase[] all = values();
        return all[ (ordinal() + 1) % all.length ];
    }

    public boolean isHold() { return this == HOLD_IN || this == HOLD_OUT; }

    /**
     *  The eased orb scale ({@code 0 == fully contracted}, {@code 1 == fully expanded})
     *  at the supplied {@link Progress} within this phase. <br>
     *  Note how the {@link AnimationStatus} handed to the animation already
     *  implements {@link Progress}, so the sinusoidal {@link Progress#fadeIn(double, double)}
     *  easing makes the orb swell and shrink in a calm, organic way — no
     *  easing math anywhere in the view.
     */
    public double scaleAt( Progress p ) {
        switch ( this ) {
            case INHALE:   return p.fadeIn(0.0, 1.0); // gently swell
            case HOLD_IN:  return 1.0;                // stay full
            case EXHALE:   return p.fadeIn(1.0, 0.0); // gently shrink
            case HOLD_OUT:
            default:       return 0.0;                // stay empty
        }
    }

    /** The orb scale at the very end of this phase, which is also the start of the next one. */
    public double endScale() {
        switch ( this ) {
            case INHALE:
            case HOLD_IN: return 1.0;
            default:      return 0.0;
        }
    }
}