package example;

import com.formdev.flatlaf.intellijthemes.FlatMaterialDesignDarkIJTheme;
import sprouts.Var;
import sprouts.Vars;
import swingtree.UI;

import javax.swing.*;
import java.awt.*;

import static swingtree.UI.Panel;
import static swingtree.UI.*;

public class SomeSpinnersView extends Panel
{
	public SomeSpinnersView(SomeSpinnersViewModel vm) {
		try { UIManager.setLookAndFeel(new FlatMaterialDesignDarkIJTheme()); }
		catch (Exception e) {e.printStackTrace();}
		UIManager.put("ComboBox.selectionBackground", Color.BLUE);
		UIManager.put("List.selectionBackground", Color.GREEN);
		of(this).withLayout(FILL.and(INS(12)))
		.add(GROW,
			panel(FILL.and(INS(0)).and(WRAP(1)))
			.add(GROW, label("Base Size:"))
			.add(GROW,
				comboBox(vm.getBaseSize())
				.withRenderer(
					renderComboItem(SomeSpinnersViewModel.BaseSize.class)
					.asText( cell -> cell.value().map(SomeSpinnersViewModel.BaseSize::title).orElse("") )
				)
			)
		)
		.add(GROW.and(PUSH_Y).and(WRAP),
			panel(FILL.and(INS(0)))
			.add(GROW.and(WRAP),
				panel(FILL.and(INS(0)))
				.add(WIDTH(30, 60, 75).and(GROW).and(PUSH_Y),
					spinner(vm.getX())
				)
				.add(WIDTH(10, 12, 15), label("x"))
				.add(WIDTH(30, 52, 75).and(GROW).and(PUSH_Y),
					spinner(vm.getPercent()).withStepSize(0.1)
				)
				.add(WIDTH(10, 60, 15), label("%"))
			)
			.add(GROW,
				panel(FILL.and(INS((3))))
				.add(PUSH_Y, panel())
				.add(GAP_LEFT_PUSH,
					button("-")
				)
				.add(ALIGN_CENTER,
					button("o")
				)
				.add(GAP_RIGHT_PUSH,
					button("+")
				)
				.add(PUSH_Y, panel())
			)
		)
		.add(SPAN.and(GROW),
			panel(FILL.and(WRAP(2)))
			.add(GROW,
				of(new JComboBox<>(new String[]{"1", "2", "3"}))
			)
			.add(GROW,
				comboBox(Var.of("A"),"A", "B", "C")
			)
			.add(GROW,
				comboBox("A", "B", "C").isEditableIf(true)
			)
			.add(GROW,
				UI.comboBoxWithUnmodifiable("X", "Y", "Z").isEditableIf(true)
			)
			.add(GROW,
				comboBoxWithUnmodifiable(1, 2, 4, 6, 8, 12, 16)
				.isEditableIf(true)
				.withSelectedItem(Var.of(4))
			)
			.add(GROW,
				comboBox(1, 2, 4, 6, 8, 12, 16)
				.onOpen( it -> System.out.println("open") )
				.onClose( it -> System.out.println("close") )
				.onCancel( it -> System.out.println("cancel") )
				.isEditableIf(true)
				.withPrefWidth(50)
				.withSelectedItem(Var.of(4))
			)
			.add(GROW,
				comboBox("U", "H", "K")
				.isEditableIf(true)
				.withSelectedItem(Var.of("H"))
			)
			.add(GROW,
				comboBox(Vars.of(1, 2, 4, 6, 8, 12, 16))
				.withSelectedItem(Var.of(4))
			)
			.add(GROW.and(SPAN),
				splitButton("Hi")
				.onSplitClick( it -> System.out.println("split") )
				.onOpen( it -> System.out.println("open") )
				.onClose( it -> System.out.println("close") )
				.onCancel( it -> System.out.println("cancel") )
				.add(
					splitItem("A")
				)
				.add(
					splitItem("B")
				)
			)
		);
	}

	public static void main(String... args) {
		UI.show(new SomeSpinnersView(new SomeSpinnersViewModel()));
		UI.joinDecoupledEventProcessor();
	}

}

