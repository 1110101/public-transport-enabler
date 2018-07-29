/*
 * Copyright the original author or authors.
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

import java.io.Serializable;
import java.util.Date;
import java.util.List;

import javax.annotation.Nullable;

import static com.google.common.base.Preconditions.checkNotNull;

public abstract class Leg implements Serializable {
    private static final long serialVersionUID = 8498461220084523265L;

    public final Location departure;
    public final Location arrival;
    public List<Point> path;

    public Leg(final Location departure, final Location arrival, final List<Point> path) {
        this.departure = checkNotNull(departure);
        this.arrival = checkNotNull(arrival);
        this.path = path;
    }

    /** Coarse departure time. */
    public abstract Date getDepartureTime();

    /** Coarse arrival time. */
    public abstract Date getArrivalTime();

    /** Minimum time occurring in this leg. */
    public abstract Date getMinTime();

    /** Maximum time occurring in this leg. */
    public abstract Date getMaxTime();

    public final static class Public extends Leg {
        private static final long serialVersionUID = 1312066446239817422L;

        public final Line line;
        public final @Nullable
        Location destination;
        public final Stop departureStop;
        public final Stop arrivalStop;
        public final @Nullable List<Stop> intermediateStops;
        public final @Nullable String message;

        public Public(final Line line, final Location destination, final Stop departureStop, final Stop arrivalStop,
                final List<Stop> intermediateStops, final List<Point> path, final String message) {
            super(departureStop.location, arrivalStop.location, path);

            this.line = checkNotNull(line);
            this.destination = destination;
            this.departureStop = checkNotNull(departureStop);
            this.arrivalStop = checkNotNull(arrivalStop);
            this.intermediateStops = intermediateStops;
            this.message = message;

            checkNotNull(departureStop.getDepartureTime());
            checkNotNull(arrivalStop.getArrivalTime());
        }

        @Override
        public Date getDepartureTime() {
            return departureStop.getDepartureTime(false);
        }

        public Date getDepartureTime(final boolean preferPlanTime) {
            return departureStop.getDepartureTime(preferPlanTime);
        }

        public boolean isDepartureTimePredicted() {
            return departureStop.isDepartureTimePredicted(false);
        }

        public Long getDepartureDelay() {
            return departureStop.getDepartureDelay();
        }

        public Position getDeparturePosition() {
            return departureStop.getDeparturePosition();
        }

        public boolean isDeparturePositionPredicted() {
            return departureStop.isDeparturePositionPredicted();
        }

        @Override
        public Date getArrivalTime() {
            return arrivalStop.getArrivalTime(false);
        }

        public Date getArrivalTime(final boolean preferPlanTime) {
            return arrivalStop.getArrivalTime(preferPlanTime);
        }

        public boolean isArrivalTimePredicted() {
            return arrivalStop.isArrivalTimePredicted(false);
        }

        public Long getArrivalDelay() {
            return arrivalStop.getArrivalDelay();
        }

        public Position getArrivalPosition() {
            return arrivalStop.getArrivalPosition();
        }

        public boolean isArrivalPositionPredicted() {
            return arrivalStop.isArrivalPositionPredicted();
        }

        @Override
        public Date getMinTime() {
            return departureStop.getMinTime();
        }

        @Override
        public Date getMaxTime() {
            return arrivalStop.getMaxTime();
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).add("line", line).add("destination", destination)
                    .add("departureStop", departureStop).add("arrivalStop", arrivalStop).omitNullValues().toString();
        }
    }

    public final static class Individual extends Leg {
        public enum Type {
            WALK, BIKE, CAR, TRANSFER
        }

        private static final long serialVersionUID = -6651381862837233925L;

        public final Type type;
        public final Date departureTime;
        public final Date arrivalTime;
        public final int min;
        public final int distance;

        public Individual(final Type type, final Location departure, final Date departureTime, final Location arrival,
                final Date arrivalTime, final List<Point> path, final int distance) {
            super(departure, arrival, path);

            this.type = checkNotNull(type);
            this.departureTime = checkNotNull(departureTime);
            this.arrivalTime = checkNotNull(arrivalTime);
            this.min = (int) ((arrivalTime.getTime() - departureTime.getTime()) / 1000 / 60);
            this.distance = distance;
        }

        public Individual movedClone(final Date departureTime) {
            final Date arrivalTime = new Date(
                    departureTime.getTime() + this.arrivalTime.getTime() - this.departureTime.getTime());
            return new Individual(this.type, this.departure, departureTime, this.arrival, arrivalTime, this.path,
                    this.distance);
        }

        @Override
        public Date getDepartureTime() {
            return departureTime;
        }

        @Override
        public Date getArrivalTime() {
            return arrivalTime;
        }

        @Override
        public Date getMinTime() {
            return departureTime;
        }

        @Override
        public Date getMaxTime() {
            return arrivalTime;
        }

        @Override
        public String toString() {
            return MoreObjects.toStringHelper(this).addValue(type).add("departure", departure).add("arrival", arrival)
                    .add("min", min).add("distance", distance).omitNullValues().toString();
        }
    }
}
