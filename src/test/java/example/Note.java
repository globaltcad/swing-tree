package example;

public class Note
{
    private final String name;
    private final int octave;
    private final int index;


    public Note(String name, int octave, int index) {
        this.name = name;
        this.octave = octave;
        this.index = index;
    }

    public String name() { return name; }

    public int octave() { return octave; }

    public int index() { return index; }

    @Override public String toString() {
        return "Note[name=" + name + ", octave=" + octave + ", index=" + index + "]";
    }
}
