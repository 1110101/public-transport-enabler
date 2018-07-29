package de.schildbach.pte.dto;

import java.io.Serializable;

public class JourneyDetails implements Serializable {

	public String id;
	public Leg.Public leg;


	public JourneyDetails(Leg.Public leg, String id) {
		this.id = id;
		this.leg = leg;
	}
}
