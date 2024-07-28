package examples.games.notepicker.mvvm;

public final class Note
{
    private final String name;
    private final int octave;
    private final int index;
    private final boolean isBlack;


    public Note( String name, int octave, int index, boolean isBlack ) {
        this.name = name;
        this.octave = octave;
        this.index = index;
        this.isBlack = isBlack;
    }

    public String name() { return name; }

    public int octave() { return octave; }

    public int index() { return index; }

    public int midiIndex() {
        int baseOctave = 3;
        int octave = baseOctave + this.octave;
        int durNote = index % 7;
        int note = 0;
        switch ( durNote ) {
            case 0: note = 0;break;
            case 1: note = 2;break;
            case 2: note = 4;break;
            case 3: note = 5;break;
            case 4: note = 7;break;
            case 5: note = 9;break;
            case 6: note = 11;break;
        }
        return octave * 12 + note;
    }

    public boolean isBlack() { return isBlack; }

    @Override public String toString() {
        return "Note[name=" + name + ", octave=" + octave + ", index=" + index + "]";
    }
}
