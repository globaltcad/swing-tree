package examples.lists;

import org.slf4j.Logger;
import sprouts.From;
import sprouts.Var;
import sprouts.Vars;
import swingtree.UI;

import javax.swing.JPanel;
import java.io.File;

public class ListTestExample extends JPanel {
    Logger log = org.slf4j.LoggerFactory.getLogger(ListTestExample.class);

    public ListTestExample() {
        Var<File> selectedFile = Var.ofNull(File.class);
        Vars<File> files = Vars.of(
            new File("file1.txt"),
            new File("file2.txt"),
            new File("file3.txt")
        );
        Var<String> selectedString = Var.ofNull(String.class);
        Vars<String> strings = Vars.of(
            "string1",
            "string2",
            "string3"
        );

        selectedFile.onChange(From.ALL, it -> {
            log.info("Selected file: " + it);
        });

        UI.of(this).withLayout("fill, wrap 2", "12[grow]12[grow]12")
        .add(
            UI.panel("fill, wrap 1")
            .add(UI.label("Files:"))
            .add(
                UI.list(selectedFile, files)
            )
        )
        .add(
            UI.panel("fill, wrap 1")
            .add(UI.label("String:"))
            .add(
                UI.list(selectedString, strings)
            )
        )
        .add(
            UI.panel("fill, wrap 1")
            //.add(
            //    UI.label(selectedFile.viewAsString(file -> {
            //        if (file == null)
            //            return "No file selected!";
            //        return file.getName();
            //    }))
            //)
            .add(
                UI.button("Select file").onClick(f -> {
                    selectedFile.set(files.at(0).get());
                })
            )
            .add(
                UI.button("Add file").onClick(f -> {
                    files.add(new File("file" + files.size() + ".txt"));
                })
            )
            .add(
                UI.button("Remove file").onClick(f -> {
                    files.remove(selectedFile.get());
                })
            )
            .add(
                UI.button("Remove all").onClick(f -> {
                    files.clear();
                })
            )
            .add(
                UI.button("Add all (3)").onClick(f -> {
                    files.addAll(
                            new File("file" + files.size() + ".txt"),
                            new File("file" + (files.size() + 1) + ".txt"),
                            new File("file" + (files.size() + 2) + ".txt")
                        );
                })
            )
        )
        .add(
            UI.panel("fill, wrap 1")
            //.add(
            //    UI.label(selectedString)
            //)
            .add(
                UI.button("Select string").onClick(f -> {
                    selectedString.set(strings.at(0).get());
                })
            )
            .add(
                UI.button("Add string").onClick(f -> {
                    strings.add("string" + strings.size());
                })
            )
            .add(
                UI.button("Remove string").onClick(f -> {
                    strings.remove(selectedString.get());
                })
            )
            .add(
                UI.button("Remove all").onClick(f -> {
                    strings.clear();
                })
            )
            .add(
                UI.button("Add all (3)").onClick(f -> {
                    strings.addAll(
                            "string" +  strings.size(),
                            "string" + (strings.size() + 1),
                            "string" + (strings.size() + 2)
                        );
                })
            )
        );

    }

    public static void main(String[] args) {
        UI.show(f -> new ListTestExample());
    }
}
