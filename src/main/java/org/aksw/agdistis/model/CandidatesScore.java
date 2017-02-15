/*
 * Copyright (C) 2016 diegomoussallem
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
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.aksw.agdistis.model;

/**
 *
 * @author diegomoussallem
 */
public class CandidatesScore implements Comparable<CandidatesScore> {

	private int startPos;
	private String uri;
	private double score; // Don't use double type for financial information.

	public int getStart() {
		return startPos;
	}

	public void setStart(int start) {
		this.startPos = start;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	public double getScore() {
		return score;
	}

	public void setScore(double score) {
		this.score = score;
	}

	@Override
	public int compareTo(CandidatesScore o) {
		return new Double(o.getScore()).compareTo(score);
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("Candidates [StartPosition=");
		builder.append(startPos);
		builder.append(", Uri=");
		builder.append(uri);
		builder.append(", Score=");
		builder.append(score);
		builder.append("]");
		return builder.toString();
	}

}
