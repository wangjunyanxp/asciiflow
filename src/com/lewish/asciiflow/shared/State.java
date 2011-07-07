//Copyright Lewis Hemens 2011
package com.lewish.asciiflow.shared;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

import javax.jdo.annotations.IdGeneratorStrategy;
import javax.jdo.annotations.PersistenceCapable;
import javax.jdo.annotations.Persistent;
import javax.jdo.annotations.PrimaryKey;

import org.dellroad.lzma.client.LZMAByteArrayCompressor;
import org.dellroad.lzma.client.LZMAByteArrayDecompressor;

import com.google.appengine.api.datastore.Blob;
import com.google.gwt.core.client.Scheduler;
import com.google.gwt.user.client.rpc.AsyncCallback;

@PersistenceCapable
public class State implements Serializable {

	private static final long serialVersionUID = 8847057226414076746L;

	private transient final List<CellState> states = new ArrayList<CellState>();

	@PrimaryKey
	@Persistent(valueStrategy = IdGeneratorStrategy.IDENTITY)
	private Long id;

	@Persistent
	private transient Blob compressedBlob;

	private byte[] compressedState;

	public void add (CellState cellState) {
		states.add(cellState);
	}

	public List<CellState> getStates() {
		return states;
	}

	public static class CellState {
		public int x;
		public int y;
		public String value;
	
		public CellState(int x, int y, String value) {
			this.x = x;
			this.y = y;
			this.value = value;
		}

		public String toString() {
			return x + ":" + y + ":" + value;
		}

		public static CellState fromString(String string) {
			String[] split = string.split(":",3);
			return new CellState(
					Integer.parseInt(split[0]),
					Integer.parseInt(split[1]),
					split[2]);
		}
	}

	public void compress(final AsyncCallback<Boolean> callback) {
		String s = "";
		for(CellState cellstate : states) {
			if(!s.equals("")) {
				s += "\n";
			}
			s += cellstate.toString();
		}
		final String toCompress = s;
		Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
			LZMAByteArrayCompressor c = new LZMAByteArrayCompressor(toCompress.getBytes());
			@Override
			public boolean execute() {
				if (!c.execute()) {
					compressedState = c.getCompressedData();
					callback.onSuccess(true);
					return false;
				}
				return true;
			}
		});
	}

	public void uncompress(final AsyncCallback<Boolean> callback) {
		Scheduler.get().scheduleIncremental(new Scheduler.RepeatingCommand() {
			LZMAByteArrayDecompressor c = new LZMAByteArrayDecompressor(compressedState);
			@Override
			public boolean execute() {
				if (!c.execute()) {
					String s = new String(c.getUncompressedData());
					String[] split = s.split("\n");
					states.clear();
					for(String line : split) {
						states.add(CellState.fromString(line));
					}
					callback.onSuccess(true);
					return false;
				}
				return true;
			}
		});
	}

	public Long getId() {
		return id;
	}

	public boolean isCompressed() {
		return compressedState != null;
	}

	public void setId(Long id) {
		this.id = id;
	}

	public boolean hasId() {
		return id != null && id != 0l;
	}

	/**
	 * Do not call clientside!
	 * Blob not supported client side but required in the datastore.
	 */
	public void blobify() {
		compressedBlob = new Blob(compressedState);
		compressedState = null;
	}

	/**
	 * Do not call clientside!
	 * Blob not supported client side but required in the datastore.
	 */
	public void unblobify() {
		compressedState = compressedBlob.getBytes();
		compressedBlob = null;
	}
}