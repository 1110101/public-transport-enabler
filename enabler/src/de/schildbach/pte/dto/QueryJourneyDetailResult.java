package de.schildbach.pte.dto;

import java.io.Serializable;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public class QueryJourneyDetailResult implements Serializable {

	public enum Status {
		OK, INVALID_ID, SERVICE_DOWN
	}

	public final @Nullable
	ResultHeader header;
	public final QueryJourneyDetailResult.Status status;

	public JourneyDetails journeyDetails;


	public QueryJourneyDetailResult(final ResultHeader header, final JourneyDetails journeyDetails) {
		this.header = header;
		this.status = QueryJourneyDetailResult.Status.OK;
		this.journeyDetails = checkNotNull(journeyDetails);
	}

	public QueryJourneyDetailResult(final ResultHeader header, final QueryJourneyDetailResult.Status status) {
		this.header = header;
		this.status = checkNotNull(status);
		this.journeyDetails = null;
	}

}
