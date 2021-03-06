package com.lewish.asciiflow.client.tools;

import javax.inject.Inject;

import com.google.gwt.resources.client.ImageResource;
import com.lewish.asciiflow.client.AsciiKeyCodes;
import com.lewish.asciiflow.client.Canvas;
import com.lewish.asciiflow.client.HistoryManager;
import com.lewish.asciiflow.client.Tool;
import com.lewish.asciiflow.client.common.Box;
import com.lewish.asciiflow.client.resources.AsciiflowClientBundle;
import com.lewish.asciiflow.shared.CellState;
import com.lewish.asciiflow.shared.CellStateMap;

public class SelectTool extends Tool {

	public static enum SelectState {
		Dragging, Nothing, Selected, Moving;
	}

	private SelectState state = SelectState.Nothing;
	private CellStateMap clipboard;
	private Box currentBox;

	private int moveX = 0;
	private int moveY = 0;

	@Inject
	public SelectTool(Canvas canvas, HistoryManager historyManager,
			AsciiflowClientBundle clientBundle) {
		super(canvas, historyManager, clientBundle);
	}

	@Override
	public void mouseOver(int x, int y) {
		if (state == SelectState.Dragging) {
			currentBox.setFinish(x, y);
			draw();
		}
		if(state == SelectState.Moving) {
			currentBox.setStart(x, y);
			currentBox.setFinish(x, y);
			paste(moveX, moveY);
			refreshDraw();
		}
	}

	@Override
	public void mouseDown(int x, int y) {
		if (state == SelectState.Nothing) {
			state = SelectState.Dragging;
			currentBox = new Box(x, y);
			draw();
		}
		if (state == SelectState.Selected) {
			
			if(isInside(x, y)) {
				state = SelectState.Moving;
				moveX = x - currentBox.topLeftX();
				moveY = y - currentBox.topLeftY();
				copy(true);
				currentBox = new Box(x, y);
				refreshDraw();
				commitDraw();
				paste(moveX, moveY);
				refreshDraw();
			} else {
				state = SelectState.Dragging;
				currentBox = new Box(x, y);
				draw();
			}
		}
	}

	@Override
	public void mouseUp(int x, int y) {
		if (state == SelectState.Dragging) {
			state = SelectState.Selected;
			currentBox.setFinish(x, y);
		}
		if (state == SelectState.Moving) {
			state = SelectState.Selected;
			commitDraw();
		}
	}

	@Override
	public void cleanup() {
		state = SelectState.Nothing;
		if (currentBox != null) {
			canvas.clearDraw();
			currentBox = null;
		}
	}

	private void copy(boolean cut) {
		if (currentBox == null)
			return;
		clipboard = new CellStateMap();
		for (int x = currentBox.topLeftX(); x <= currentBox.bottomRightX(); x++) {
			for (int y = currentBox.topLeftY(); y <= currentBox.bottomRightY(); y++) {
				int dx = x - currentBox.topLeftX();
				int dy = y - currentBox.topLeftY();
				String val = canvas.getValue(x, y);
				// Move to origin
				if (val != null) {
					clipboard.add(new CellState(dx, dy, val));
					if (cut) {
						canvas.draw(x, y, " ");
					}
				}
			}
		}
	}

	private void paste(int dx, int dy) {
		CellStateMap pasteState = new CellStateMap();
		if (currentBox != null && clipboard != null) {
			for (CellState cs : clipboard.getCellStates()) {
				// Move to select position
				pasteState.add(new CellState(cs.x + currentBox.topLeftX() - dx, cs.y
						+ currentBox.topLeftY() - dy, cs.value));
			}
			canvas.drawCellStates(pasteState);
		}
	}

	@Override
	public String getLabel() {
		return "+box";
	}

	public void draw() {
		for (int x = currentBox.topLeftX(); x <= currentBox.bottomRightX(); x++) {
			for (int y = currentBox.topLeftY(); y <= currentBox.bottomRightY(); y++) {
				canvas.highlight(x, y, true);
			}
		}
		refreshDraw();
	}

	@Override
	public String getDescription() {
		return "Select, move, copy (ctrl+c), cut (ctrl+x) and paste (ctrl+v). This is only internal to the app!";
	}

	@Override
	public ImageResource getImageResource() {
		return clientBundle.selectToolImage();
	}

	@Override
	public void specialKeyPress(int keyCode) {
		switch (keyCode) {
		case AsciiKeyCodes.COPY:
			copy(false);
			break;
		case AsciiKeyCodes.PASTE:
			paste(0,0);
			refreshDraw();
			commitDraw();
			break;
		case AsciiKeyCodes.CUT:
			copy(true);
			refreshDraw();
			commitDraw();
			break;
		}
	}

	public boolean isInside(int x, int y) {
		if(currentBox == null) return false;
		return x <= currentBox.bottomRightX() && x >= currentBox.topLeftX()
		&& y <= currentBox.bottomRightY() && y >= currentBox.topLeftY();
	}
}
