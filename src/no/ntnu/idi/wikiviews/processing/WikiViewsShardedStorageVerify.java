package no.ntnu.idi.wikiviews.processing;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import no.ntnu.idi.wikiviews.aux.CodeProfiler;
import no.ntnu.idi.wikiviews.aux.GlobalTime;
import no.ntnu.idi.wikiviews.aux.IntervalDate;
import no.ntnu.idi.wikiviews.aux.Sharding;
import no.ntnu.idi.wikiviews.base.PageDisplays;
import no.ntnu.idi.wikiviews.base.PageDisplaysHistory;
import no.ntnu.idi.wikiviews.base.PageId;
import no.ntnu.idi.wikiviews.base.PageMetadata;
import no.ntnu.idi.wikiviews.exceptions.BadDateTimeFormat;
import no.ntnu.idi.wikiviews.storage.ShardedStorageReader;

public class WikiViewsShardedStorageVerify {

	protected static final Logger LOGGER = Logger.getLogger(PageDisplaysHistory.class.getName());
	static {
		LOGGER.setLevel(Level.ALL);
	}

	public static void main(String args[]) throws BadDateTimeFormat, ParseException, IOException {
		LOGGER.info("The program validates consistency and prints statistcs of sharded storage.");
		final String baseDir;
		final int numShards;
		try {
			baseDir = args[0];
			numShards = Sharding.DEFAULT_NUM_SHARDS;
		} catch (Exception e) {
			LOGGER.severe("Failed parsing arguments! Argument expected: sharded storage directory.");
			System.exit(-1);
			return;
		}
		LOGGER.info("baseDir = " + baseDir + " numShards = " + numShards);

		ShardedStorageReader reader = new ShardedStorageReader(baseDir, numShards);
		Set<PageId> pages = reader.getKeys();
		LOGGER.info(pages.size() + " keys read");

		long timelineEndingMsec = extractTimelineEndMsec(reader, pages.iterator().next());
		int counter = 0, validCounter = 0, failuresCounter = 0;
		for (PageId page : pages) {
			if (reader.contains(page)) {
				try {

					List<PageDisplays> history = reader.read(page);
					PageMetadata meta = reader.getPageMetadata(page);
					long startTime = GlobalTime.parseDate(meta.getStartDate(), meta.getStartTime()).getTime();
					long thisEndingMsec = startTime + PageDisplaysHistory.unwrappedLength(history) * 3600000L;

					if (thisEndingMsec != timelineEndingMsec) {
						List<PageDisplays> unwrapHistory = PageDisplaysHistory.unwrap(history);
						LOGGER.warning("Wrong timeline for: page = " + meta + " history.size() = " + history.size()
								+ " history => " + PageDisplaysHistory.toString(history) + " UnwrappedLength = "
								+ PageDisplaysHistory.unwrappedLength(history) + " unwrapHistory.size() = "
								+ unwrapHistory.size() + " equals = "
								+ PageDisplaysHistory.equals(history, unwrapHistory) + " startTime =" + startTime
								+ " (" + (new IntervalDate(startTime)) + ") thisEndingMsec = " + thisEndingMsec + " ("
								+ (new IntervalDate(thisEndingMsec)) + ") timelineEndingMsec = " + timelineEndingMsec
								+ " (" + (new IntervalDate(timelineEndingMsec)) + ")");
						failuresCounter++;
					} else {
						validCounter++;
					}
				} catch (IOException e) {
					LOGGER.warning("Failed reading page = " + page + ". Excpetion:" + e);
					failuresCounter++;
				}
			} else {
				LOGGER.warning(page.toString() + " is not contained in the storage!");
				failuresCounter++;
			}

			counter++;
			if (counter % 100000 == 0) {
				CodeProfiler.getInstance().set("ValidEntries", validCounter);
				CodeProfiler.getInstance().set("Failures", failuresCounter);
				CodeProfiler.getInstance().printStats("StorageValidation");
			}
		}
		
		CodeProfiler.getInstance().set("ValidEntries", validCounter);
		CodeProfiler.getInstance().set("Failures", failuresCounter);
		CodeProfiler.getInstance().printStats("StorageValidation");
		
		
	}

	private static long extractTimelineEndMsec(ShardedStorageReader reader, PageId id) throws BadDateTimeFormat,
			ParseException, IOException {
		PageMetadata meta = reader.getPageMetadata(id);
		IntervalDate firstStartDate = GlobalTime.parseDate(meta.getStartDate(), meta.getStartTime());
		List<PageDisplays> history = reader.read(id);
		long timeEndingMsec = firstStartDate.getTime() + PageDisplaysHistory.unwrappedLength(history) * 3600000L;
		/*
		 * LOGGER.info("page  = " + meta + " start date = " + firstStartDate +
		 * " (" + meta.getStartDate() + " " + meta.getStartTime() + " => " +
		 * firstStartDate.getTime() + ") length = " +
		 * PageDisplaysHistory.unwrappedLength(history));
		 * LOGGER.info("timeEndingMsec = " + timeEndingMsec + " timeEnding = " +
		 * (new IntervalDate(timeEndingMsec)));
		 */
		LOGGER.info("Timeline end = "+new IntervalDate(timeEndingMsec));
		return timeEndingMsec;
	}
}
