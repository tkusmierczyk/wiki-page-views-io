package no.ntnu.idi.wikiviews.storage;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import no.ntnu.idi.wikiviews.aux.CodeProfiler;
import no.ntnu.idi.wikiviews.base.*;

public class SingleUnwrappedFileStorageWriter {

	final private static Logger LOGGER = Logger.getLogger(CacheStorage.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}

	final BufferedWriter out;

	public SingleUnwrappedFileStorageWriter(String path) throws IOException {
		out = new BufferedWriter(new FileWriter(path));
	}

	public SingleUnwrappedFileStorageWriter(FileWriter f) {
		out = new BufferedWriter(f);
	}

	public SingleUnwrappedFileStorageWriter(BufferedWriter f) {
		out = f;
	}
	
	public void store(PageDisplaysHistory history) throws IOException {
		out.write(history.toUnwrappedString().replace("\n", ""));
		out.write("\n");
		CodeProfiler.getInstance().register("SingleFileWrite");
	}

	public void store(PageMetadata meta, List<PageDisplays> history) throws IOException {
		PageDisplaysHistory p = new PageDisplaysHistory(meta.getId(), meta.getStartDate(), meta.getStartTime(), 0,
				history);
		store(p);
	}
	
	public void close() throws IOException {
		out.close();
	}
}
