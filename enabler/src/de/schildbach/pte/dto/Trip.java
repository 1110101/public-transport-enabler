/*
 * Copyright 2010-2015 the original author or authors.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package de.schildbach.pte.dto;

import com.google.common.base.MoreObjects;
import com.google.common.base.MoreObjects.ToStringHelper;
import com.google.common.base.Objects;

import java.io.Serializable;
import java.util.Date;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;

/**
 * @author Andreas Schildbach
 */
public final class Trip implements Serializable {

    private String id;
    public final Location from;
    public final Location to;
    public final List<Leg> legs;
    public final List<Fare> fares;
    public final int[] capacity;
    public final Integer numChanges;

    public Trip(final String id, final Location from, final Location to, final List<Leg> legs, final List<Fare> fares,
            final int[] capacity, final Integer numChanges) {
        this.id = id;
        this.from = checkNotNull(from);
        this.to = checkNotNull(to);
        this.legs = checkNotNull(legs);
        this.fares = fares;
        this.capacity = capacity;
        this.numChanges = numChanges;

        checkArgument(!legs.isEmpty());
    }

    public Date getFirstDepartureTime() {
        return legs.get(0).getDepartureTime();
    }

    public @Nullable
    Leg.Public getFirstPublicLeg() {
        for (final Leg leg : legs)
            if (leg instanceof Leg.Public)
                return (Leg.Public) leg;

        return null;
    }

    public @Nullable Date getFirstPublicLegDepartureTime() {
        final Leg.Public firstPublicLeg = getFirstPublicLeg();
        if (firstPublicLeg != null)
            return firstPublicLeg.getDepartureTime();
        else
            return null;
    }

    public Date getLastArrivalTime() {
        return legs.get(legs.size() - 1).getArrivalTime();
    }

    public @Nullable
    Leg.Public getLastPublicLeg() {
        for (int i = legs.size() - 1; i >= 0; i--) {
            final Leg leg = legs.get(i);
            if (leg instanceof Leg.Public)
                return (Leg.Public) leg;
        }

        return null;
    }

    public @Nullable Date getLastPublicLegArrivalTime() {
        final Leg.Public lastPublicLeg = getLastPublicLeg();
        if (lastPublicLeg != null)
            return lastPublicLeg.getArrivalTime();
        else
            return null;
    }

    /**
     * Duration of whole trip in milliseconds, including leading and trailing individual legs.
     *
     * @return duration in ms
     */
    public long getDuration() {
        final Date first = getFirstDepartureTime();
        final Date last = getLastArrivalTime();
        return last.getTime() - first.getTime();
    }

    /**
     * Duration of the public leg part in milliseconds. This includes individual legs between public legs, but
     * excludes individual legs that lead or trail the trip.
     *
     * @return duration in ms, or null if there are no public legs
     */
    public @Nullable Long getPublicDuration() {
        final Date first = getFirstPublicLegDepartureTime();
        final Date last = getLastPublicLegArrivalTime();
        if (first != null && last != null)
            return last.getTime() - first.getTime();
        else
            return null;
    }

    /** Minimum time occurring in this trip. */
    public Date getMinTime() {
        Date minTime = null;

        for (final Leg leg : legs)
            if (minTime == null || leg.getMinTime().before(minTime))
                minTime = leg.getMinTime();

        return minTime;
    }

    /** Maximum time occurring in this trip. */
    public Date getMaxTime() {
        Date maxTime = null;

        for (final Leg leg : legs)
            if (maxTime == null || leg.getMaxTime().after(maxTime))
                maxTime = leg.getMaxTime();

        return maxTime;
    }

    /**
     * <p>
     * Number of changes on the trip.
     * </p>
     *
     * <p>
     * Returns {@link #numChanges} if it isn't null. Otherwise, it tries to compute the number by counting
     * public legs of the trip. The number of changes for a Trip consisting of only individual Legs is null.
     * </p>
     *
     * @return number of changes on the trip, or null if no public legs are involved
     */
    @Nullable
    public Integer getNumChanges() {
        if (numChanges == null) {
            Integer numCount = null;

            for (final Leg leg : legs) {
                if (leg instanceof Leg.Public) {
                    if (numCount == null) {
                        numCount = 0;
                    } else {
                        numCount++;
                    }
                }
            }
            return numCount;
        } else {
            return numChanges;
        }
    }

    /** Returns true if no legs overlap, false otherwise. */
    public boolean isTravelable() {
        Date time = null;

        for (final Leg leg : legs) {
            final Date departureTime = leg.getDepartureTime();
            if (time != null && departureTime.before(time))
                return false;
            time = departureTime;

            final Date arrivalTime = leg.getArrivalTime();
            if (time != null && arrivalTime.before(time))
                return false;
            time = arrivalTime;
        }

        return true;
    }

    /** If an individual leg overlaps, try to adjust so that it doesn't. */
    public void adjustUntravelableIndividualLegs() {
        final int numLegs = legs.size();
        if (numLegs < 1)
            return;

        for (int i = 1; i < numLegs; i++) {
            final Leg leg = legs.get(i);

            if (leg instanceof Leg.Individual) {
                final Leg previous = legs.get(i - 1);

                if (leg.getDepartureTime().before(previous.getArrivalTime()))
                    legs.set(i, ((Leg.Individual) leg).movedClone(previous.getArrivalTime()));
            }
        }
    }

    public Set<Product> products() {
        final Set<Product> products = EnumSet.noneOf(Product.class);

        for (final Leg leg : legs)
            if (leg instanceof Leg.Public)
                products.add(((Leg.Public) leg).line.product);

        return products;
    }

    public String getId() {
        if (id == null)
            id = buildSubstituteId();

        return id;
    }

    private String buildSubstituteId() {
        final StringBuilder builder = new StringBuilder();

        for (final Leg leg : legs) {
            builder.append(leg.departure.hasId() ? leg.departure.id : leg.departure.lat + '/' + leg.departure.lon)
                    .append('-');
            builder.append(leg.arrival.hasId() ? leg.arrival.id : leg.arrival.lat + '/' + leg.arrival.lon).append('-');

            if (leg instanceof Leg.Individual) {
                builder.append("individual");
            } else if (leg instanceof Leg.Public) {
                final Leg.Public publicLeg = (Leg.Public) leg;
                final Date plannedDepartureTime = publicLeg.departureStop.plannedDepartureTime;
                if (plannedDepartureTime != null)
                    builder.append(plannedDepartureTime.getTime()).append('-');
                final Date plannedArrivalTime = publicLeg.arrivalStop.plannedArrivalTime;
                if (plannedArrivalTime != null)
                    builder.append(plannedArrivalTime.getTime()).append('-');
                final Line line = publicLeg.line;
                builder.append(line.productCode());
                builder.append(line.label);
            }

            builder.append('|');
        }

        builder.setLength(builder.length() - 1);

        return builder.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Trip))
            return false;
        final Trip other = (Trip) o;
        return Objects.equal(this.getId(), other.getId());
    }

    @Override
    public int hashCode() {
        return Objects.hashCode(getId());
    }

    @Override
    public String toString() {
        final ToStringHelper helper = MoreObjects.toStringHelper(this).addValue(getId());
        final Date firstPublicLegDepartureTime = getFirstPublicLegDepartureTime();
        final Date lastPublicLegArrivalTime = getLastPublicLegArrivalTime();
        helper.addValue(
                firstPublicLegDepartureTime != null ? String.format(Locale.US, "%ta %<tR", firstPublicLegDepartureTime)
                        : "null" + '-' + lastPublicLegArrivalTime != null
                                ? String.format(Locale.US, "%ta %<tR", lastPublicLegArrivalTime) : "null");
        helper.add("numChanges", numChanges);
        return helper.toString();
    }

}
