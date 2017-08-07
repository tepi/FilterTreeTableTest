package com.vaadin.ftttest;

import javax.servlet.annotation.WebServlet;

import org.tepi.filtertable.FilterTreeTable;

import com.vaadin.annotations.Theme;
import com.vaadin.annotations.VaadinServletConfiguration;
import com.vaadin.data.util.HierarchicalContainer;
import com.vaadin.event.Action;
import com.vaadin.event.Action.Handler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinServlet;
import com.vaadin.ui.Component;
import com.vaadin.ui.HorizontalSplitPanel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;

@Theme("valo")
public class FttTestUI extends UI {

	static final Action COPY = new Action("Copy");
	static final Action PASTE = new Action("Paste");
	static final Action[] ACTIONS = new Action[] { COPY, PASTE };

	private int nextItemId = 0;

	private FilterTreeTable copySource;
	private Object itemIdToCopy;
	private String nameToCopy;
	private FilterTreeTable leftTable;
	private FilterTreeTable rightTable;

	@Override
	protected void init(VaadinRequest vaadinRequest) {
		final VerticalLayout layout = new VerticalLayout();
		layout.setSizeFull();
		layout.setMargin(true);
		layout.setSpacing(true);
		setContent(layout);

		layout.addComponent(buildContent());
	}

	private Component buildContent() {
		leftTable = buildTable(true);
		rightTable = buildTable(false);

		leftTable.addActionHandler(new CopyPasteHandler());
		rightTable.addActionHandler(new CopyPasteHandler());

		HorizontalSplitPanel vsp = new HorizontalSplitPanel(leftTable, rightTable);
		vsp.setSizeFull();
		return vsp;
	}

	private FilterTreeTable buildTable(boolean left) {
		FilterTreeTable ftt = new FilterTreeTable();
		ftt.setFilterBarVisible(true);
		ftt.setSizeFull();
		ftt.setSelectable(true);

		HierarchicalContainer cont = new HierarchicalContainer();
		cont.addContainerProperty("Name", String.class, null);
		for (int i = 0; i < 100; i++) {
			cont.addItem(nextItemId);
			cont.getContainerProperty(nextItemId, "Name").setValue((left ? "Left" : "Right") + " Table Item #" + i);
			nextItemId++;
		}
		ftt.setContainerDataSource(cont);

		return ftt;
	}

	private final class CopyPasteHandler implements Handler {

		@Override
		public void handleAction(Action action, Object sender, Object target) {
			FilterTreeTable targetTable = (FilterTreeTable) sender;
			targetTable.setValue(target);
			if (COPY.equals(action)) {
				copySource = (FilterTreeTable) sender;
				itemIdToCopy = target;
				nameToCopy = (String) copySource.getContainerProperty(itemIdToCopy, "Name").getValue();
			} else if (PASTE.equals(action)) {
				HierarchicalContainer cont = (HierarchicalContainer) targetTable.getContainerDataSource();

				int indexToPasteTo = cont.indexOfId(targetTable.getValue());

				// We need to create a new container due to https://github.com/vaadin/framework/issues/3335
				HierarchicalContainer newContainer = new HierarchicalContainer();
				newContainer.addContainerProperty("Name", String.class, null);
				for (int i = 0; i < cont.size(); i++) {
					if (i == indexToPasteTo) {
						newContainer.addItem(++nextItemId);
						newContainer.getContainerProperty(nextItemId, "Name").setValue(nameToCopy + " (copy)");
					}
					Object idByIndex = cont.getIdByIndex(i);
					newContainer.addItem(idByIndex);
					newContainer.getContainerProperty(idByIndex, "Name")
							.setValue(cont.getContainerProperty(idByIndex, "Name").getValue());
				}
				targetTable.setContainerDataSource(newContainer);
				targetTable.setValue(nextItemId);
			}
		}

		@Override
		public Action[] getActions(Object target, Object sender) {
			return ACTIONS;
		}
	}

	@WebServlet(urlPatterns = "/*", name = "FttTestUIServlet", asyncSupported = true)
	@VaadinServletConfiguration(ui = FttTestUI.class, productionMode = false)
	public static class FttTestUIServlet extends VaadinServlet {
	}
}
